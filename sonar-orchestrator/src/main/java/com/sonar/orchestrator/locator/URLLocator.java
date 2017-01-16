/*
 * Orchestrator
 * Copyright (C) 2011-2017 SonarSource SA
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
package com.sonar.orchestrator.locator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.defaultString;
import static org.apache.commons.lang.StringUtils.isBlank;

class URLLocator implements Locator<URLLocation> {

  private static final Logger LOG = LoggerFactory.getLogger(URLLocator.class);
  private static final String USER_AGENT = "Orchestrator";

  @Override
  public File locate(URLLocation location) {
    throw new UnsupportedOperationException();
  }

  @Override
  public File copyToDirectory(URLLocation location, File toDir) {
    try {
      File toFile;
      if (isHttpRequest(location)) {
        Response response = sendHttpRequest(location);
        String filename = getFilenameFromContentDispositionHeader(response.header("Content-Disposition"));
        if (filename == null) {
          filename = location.getFileName();
        }
        toFile = new File(toDir, filename);
        FileUtils.copyInputStreamToFile(response.body().byteStream(), toFile);
      } else {
        toFile = new File(toDir, location.getFileName());
        FileUtils.copyURLToFile(location.getURL(), toFile);
      }
      return toFile;
    } catch (IOException e) {
      throw new IllegalStateException(format("Fail to copy %s to directory: %s", location, toDir), e);
    }
  }

  @Override
  public InputStream openInputStream(URLLocation location) {
    try {
      return location.getURL().openStream();
    } catch (IOException e) {
      throw new IllegalStateException("Fail to open " + location, e);
    }
  }

  @Override
  public File copyToFile(URLLocation location, File toFile) {
    try {
      if (isHttpRequest(location)) {
        Response response = sendHttpRequest(location);
        FileUtils.copyInputStreamToFile(response.body().byteStream(), toFile);
      } else {
        FileUtils.copyURLToFile(location.getURL(), toFile);
      }
      return toFile;
    } catch (IOException e) {
      throw new IllegalStateException(format("Fail to copy %s to file: %s", location, toFile), e);
    }
  }

  private static boolean isHttpRequest(URLLocation location) {
    return location.getURL().getProtocol().toLowerCase(Locale.ENGLISH).startsWith("http");
  }

  private static Response sendHttpRequest(URLLocation location) throws IOException {
    LOG.info("Downloading: " + location.getURL());

    OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS);

    // OkHttp detect 'http.proxyHost' java property, but credentials should be filled
    final String proxyUser = System.getProperty("http.proxyUser");
    if (StringUtils.isNotBlank(System.getProperty("http.proxyHost")) && StringUtils.isNotBlank(proxyUser)) {
      httpClientBuilder.proxyAuthenticator((route, response) -> {
        if (HttpURLConnection.HTTP_PROXY_AUTH == response.code()) {
          String credential = Credentials.basic(proxyUser, System.getProperty("http.proxyPassword"));
          return response.request().newBuilder().header("Proxy-Authorization", credential).build();
        }
        return null;
      });
    }

    OkHttpClient httpClient = httpClientBuilder.build();
    Request httpRequest = new Request.Builder()
      .url(location.getURL())
      .header("User-Agent", USER_AGENT)
      .build();
    Response response = httpClient.newCall(httpRequest).execute();
    if (!response.isSuccessful()) {
      throw new IllegalStateException(format("Fail to download %s. Received %d [%s]", location.getURL(), response.code(), response.message()));
    }
    return response;
  }

  @CheckForNull
  static String getFilenameFromContentDispositionHeader(@Nullable String header) {
    if (isBlank(header)) {
      return null;
    }
    String filename = header.replaceFirst("(?i)^.*filename=\"([^\"]+)\".*$", "$1");
    if (header.equals(filename)) {
      // strange case on bintray: "attachment; filename = sonar-lits-plugin-0.5.jar"
      filename = StringUtils.substringAfterLast(header, "=");
    }
    if (filename != null) {
      filename = StringUtils.remove(filename, "\"");
      filename = StringUtils.remove(filename, "'");
      filename = StringUtils.remove(filename, ";");
      filename = StringUtils.remove(filename, " ");
    }
    return defaultString(filename, null);
  }
}
