/*
 * Orchestrator
 * Copyright (C) 2011-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.sonar.orchestrator.http;

import java.io.File;
import java.io.IOException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import static com.sonar.orchestrator.container.Server.ADMIN_LOGIN;
import static com.sonar.orchestrator.container.Server.ADMIN_PASSWORD;
import static com.sonar.orchestrator.util.OrchestratorUtils.checkArgument;
import static com.sonar.orchestrator.util.OrchestratorUtils.isEmpty;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class HttpCall {

  private static final String DEFAULT_USER_AGENT = "Orchestrator";

  private final OkHttpClient okClient;
  private final HttpUrl baseUrl;
  private HttpMethod method = HttpMethod.GET;
  private final Map<String, String> parameters = new LinkedHashMap<>();
  private final Map<String, String> headers = new LinkedHashMap<>();
  private Long timeoutMs = null;

  HttpCall(OkHttpClient okClient, HttpUrl baseUrl) {
    this.okClient = okClient;
    this.baseUrl = baseUrl;
    this.headers.put("User-Agent", DEFAULT_USER_AGENT);
  }

  public HttpUrl getBaseUrl() {
    return baseUrl;
  }

  public HttpCall setMethod(HttpMethod m) {
    this.method = m;
    return this;
  }

  public HttpCall setCredentials(String login, @Nullable String password) {
    headers.put("Authorization", Credentials.basic(login, password == null ? "" : password));
    return this;
  }

  public HttpCall setAdminCredentials() {
    return setCredentials(ADMIN_LOGIN, ADMIN_PASSWORD);
  }

  public HttpCall setAuthenticationToken(String token) {
    return setCredentials(token, null);
  }

  /**
   * Adds parameter to URL query.
   *
   * If value is {@code null}, then only the key is sent. For example
   * {@code setParam("foo", null)} sends "?foo".
   */
  public HttpCall setParam(String key, @Nullable String value) {
    parameters.put(key, value);
    return this;
  }

  /**
   * Adds one or more parameters to URL query
   *
   * Example: {@code setParams("foo", "value of foo", "bar", "value of bar")}
   *
   * @throws IllegalArgumentException if argument {@code otherKeysAndNames} has odd size
   */
  public HttpCall setParams(String key1, @Nullable String value1, String... otherKeysAndNames) {
    checkArgument(otherKeysAndNames.length % 2 == 0, "Expecting even number of arguments: %s", Arrays.toString(otherKeysAndNames));
    parameters.put(key1, value1);
    for (int i = 0; i < otherKeysAndNames.length; i++) {
      parameters.put(otherKeysAndNames[i], otherKeysAndNames[i + 1]);
      i++;
    }
    return this;
  }

  /**
   * Adds a header to HTTP request.
   */
  public HttpCall setHeader(String key, String value) {
    requireNonNull(key, "Header key cannot be null");
    requireNonNull(value, "Header [" + key + "] cannot have null value");
    headers.put(key, value);
    return this;
  }

  /**
   * Override the default read/write timeouts.
   */
  public HttpCall setTimeoutMs(long timeoutMs) {
    this.timeoutMs = timeoutMs;
    return this;
  }

  public HttpResponse execute() {
    Request okRequest = buildOkHttpRequest();
    try (Response okResponse = doExecute(okRequest)) {
      if (!okResponse.isSuccessful()) {
        throw new HttpException(okRequest.url(), okResponse.code(), okResponse.body().string());
      }
      return HttpResponse.from(okResponse);
    } catch (IOException e) {
      throw new IllegalStateException(format("Can not call %s due to network failure", okRequest.url()), e);
    }
  }

  public HttpResponse executeUnsafely() {
    Request okRequest = buildOkHttpRequest();
    try (Response okResponse = doExecute(okRequest)) {
      return HttpResponse.from(okResponse);
    } catch (IOException e) {
      throw new IllegalStateException(format("Can not call %s due to network failure", okRequest.url()), e);
    }
  }

  public void downloadToFile(File file) {
    Request okRequest = buildOkHttpRequest();
    try {
      doDownloadToFile(okRequest, file);
    } catch (ProtocolException|SocketException|SocketTimeoutException se) {
      // retry, because of some false-positives when downloading files from GitHub
      try {
        doDownloadToFile(okRequest, file);
      } catch (IOException e2) {
        throw new IllegalStateException(format("Can not call %s", okRequest.url()), e2);
      }
    } catch (IOException e) {
      throw new IllegalStateException(format("Can not call %s", okRequest.url()), e);
    }
  }

  private void doDownloadToFile(Request okRequest, File file) throws IOException {
    try (Response okResponse = doExecute(okRequest)) {
      if (!okResponse.isSuccessful()) {
        throw new HttpException(okRequest.url(), okResponse.code(), okResponse.body().string());
      }
      FileUtils.copyInputStreamToFile(okResponse.body().byteStream(), file);
    }
  }

  public File downloadToDirectory(File dir) {
    Request okRequest = buildOkHttpRequest();
    try {
      return doDownloadToDirectory(dir, okRequest);
    } catch (ProtocolException|SocketException|SocketTimeoutException se) {
      // retry, because of some false-positives when downloading files from GitHub
      try {
        return doDownloadToDirectory(dir, okRequest);
      } catch (IOException e2) {
        throw new IllegalStateException(format("Can not call %s", okRequest.url()), e2);
      }
    } catch (IOException e) {
      throw new IllegalStateException(format("Can not call %s", okRequest.url()), e);
    }
  }

  private File doDownloadToDirectory(File dir, Request okRequest) throws IOException {
    try (Response okResponse = doExecute(okRequest)) {
      if (!okResponse.isSuccessful()) {
        throw new HttpException(okRequest.url(), okResponse.code(), okResponse.body().string());
      }
      String filename = extractFilename(okResponse);
      File toFile = new File(dir, filename);
      FileUtils.copyInputStreamToFile(okResponse.body().byteStream(), toFile);
      return toFile;
    }
  }

  private Request buildOkHttpRequest() {
    Request.Builder okRequest;
    switch (method) {
      case GET:
        // parameters of GET request are sent in the URL
        HttpUrl.Builder okUrl = baseUrl.newBuilder();
        parameters.forEach(okUrl::setQueryParameter);
        okRequest = new Request.Builder().url(okUrl.build());
        break;
      case POST:
        // parameters of POST request are sent in the body
        okRequest = new Request.Builder().url(baseUrl);
        FormBody.Builder schwarzy = new FormBody.Builder();
        parameters.entrySet().stream()
          .filter(e -> e.getValue() != null)
          .forEach(e -> schwarzy.add(e.getKey(), e.getValue()));
        okRequest.post(schwarzy.build());
        break;
      case MULTIPART_POST:
        okRequest = new Request.Builder().url(baseUrl);
        MultipartBody.Builder bodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        parameters.forEach(bodyBuilder::addFormDataPart);
        okRequest.post(bodyBuilder.build());
        break;
      default:
        throw new UnsupportedOperationException("Unsupported HTTP method: " + method);
    }
    headers.forEach(okRequest::header);
    return okRequest.build();
  }

  private Response doExecute(Request okRequest) throws IOException {
    OkHttpClient copy = okClient;
    if (timeoutMs != null) {
      // see https://github.com/square/okhttp/wiki/Recipes#per-call-configuration
      copy = okClient.newBuilder()
        .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .build();
    }
    return copy.newCall(okRequest).execute();
  }

  private static String extractFilename(Response response) {
    String disposition = response.header("Content-Disposition");
    String filename;
    if (disposition == null) {
      // extract filename from URL
      filename = StringUtils.substringAfterLast(response.request().url().encodedPath(), "/");
    } else {
      filename = disposition.replaceFirst("(?i)^.*filename=\"([^\"]+)\".*$", "$1");
      if (disposition.equals(filename)) {
        // strange case on bintray: "attachment; filename = sonar-lits-plugin-0.5.jar"
        filename = StringUtils.substringAfterLast(disposition, "=");
      }
      if (filename != null) {
        filename = StringUtils.remove(filename, "\"");
        filename = StringUtils.remove(filename, "'");
        filename = StringUtils.remove(filename, ";");
        filename = StringUtils.remove(filename, " ");
      }
    }

    if (isEmpty(filename)) {
      throw new IllegalStateException(
        format("Can not guess the target filename for download of %s. Header Content-Disposition is missing or empty.", response.request().url()));
    }

    if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
      throw new IllegalStateException("Header Content-Disposition has invalid value: " + filename);
    }

    return filename;
  }
}
