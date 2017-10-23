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
package com.sonar.orchestrator.container;

import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.http.HttpCall;
import com.sonar.orchestrator.http.HttpClientFactory;
import com.sonar.orchestrator.http.HttpMethod;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import okhttp3.HttpUrl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.connectors.HttpClient4Connector;

import static com.sonar.orchestrator.util.OrchestratorUtils.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang.StringUtils.substringAfter;

public class Server {
  public static final String ADMIN_LOGIN = "admin";
  public static final String ADMIN_PASSWORD = "admin";

  private final FileSystem fileSystem;
  private final File home;
  private final SonarDistribution distribution;
  private HttpUrl url;
  private Sonar wsClient;
  private Sonar adminWsClient;
  private SonarClient sonarClient;
  private SonarClient adminSonarClient;

  public Server(FileSystem fileSystem, File home, SonarDistribution distribution, HttpUrl url) {
    this.fileSystem = fileSystem;
    this.home = home;
    this.distribution = distribution;
    this.url = url;
  }

  public File getHome() {
    return home;
  }

  /**
   * Server base URL, without trailing slash, for example {@code "http://localhost:9000"}
   * or {@code "http://localhost:9000/sonarqube"}.
   */
  public String getUrl() {
    return StringUtils.stripEnd(url.toString(), "/");
  }

  public SonarDistribution getDistribution() {
    return distribution;
  }

  /**
   * Effective version of SonarQube, for example "6.3.0.1234" but not alias like "DEV".
   * This method does not need the server to be up (Orchestrator to be started). It
   * introspects the installation file system.
   */
  public Version version() {
    File libsDir = new File(home, "lib");
    checkState(libsDir.exists(), "Installation incomplete, missing directory %s", libsDir);
    File appJar = FileLocation.byWildcardFilename(libsDir, "sonar-application-*.jar").getFile();
    return Version.create(substringAfter(FilenameUtils.getBaseName(appJar.getName()), "sonar-application-"));
  }

  /**
   * @deprecated from SQ 6.2 and on, use {@link #getAppLogs()}, {@link #getWebLogs()}, {@link #getCeLogs()} or {@link #getEsLogs()}
   */
  @Deprecated
  public File getLogs() {
    return getLogFile("sonar.log");
  }

  /**
   * Starting from SQ 6.2, only App JVM logs in "sonar.log" file.
   *
   * @since 3.13
   */
  public File getAppLogs() {
    return getLogFile("sonar.log");
  }

  /**
   * Starting from SQ 6.2, Web JVM logs in "web.log" file.
   *
   * @since 3.13
   */
  public File getWebLogs() {
    return getLogFile("web.log");
  }

  /**
   * Starting from SQ 6.2, Compute Engine JVM logs in "ce.log" file.
   *
   * @since 3.13
   */
  public File getCeLogs() {
    return getLogFile("ce.log");
  }

  /**
   * Starting from SQ 6.2, Elastic Search JVM logs in "es.log" file.
   *
   * @since 3.13
   */
  public File getEsLogs() {
    return getLogFile("es.log");
  }

  private File getLogFile(String logFile) {
    return FileUtils.getFile(home, "logs", logFile);
  }

  /**
   * @deprecated in 3.15. Instantiate your own client configured with {@link #getUrl()}
   * or use {@link #newHttpCall(String)}
   */
  @Deprecated
  public Sonar getWsClient() {
    if (wsClient == null && url != null) {
      wsClient = new Sonar(new HttpClient4Connector(new Host(getUrl())));
    }
    return wsClient;
  }

  /**
   * @since 2.10
   * @deprecated in 3.15. Instantiate your own client configured with {@link #getUrl()}
   * or use {@link #newHttpCall(String)}
   */
  @Deprecated
  public SonarClient wsClient() {
    if (sonarClient == null && url != null) {
      sonarClient = SonarClient.create(getUrl());
    }
    return sonarClient;
  }

  /**
   * @since 2.10
   * @deprecated in 3.15. Instantiate your own client configured with {@link #getUrl()}
   * or use {@link #newHttpCall(String)}
   */
  @Deprecated
  public SonarClient wsClient(String login, String password) {
    return SonarClient.builder().url(getUrl()).login(login).password(password).build();
  }

  /**
   * @deprecated in 3.15. Instantiate your own client configured with {@link #getUrl()}
   * or use {@link #newHttpCall(String)}
   */
  @Deprecated
  public Sonar getAdminWsClient() {
    if (adminWsClient == null && url != null) {
      adminWsClient = new Sonar(new HttpClient4Connector(new Host(getUrl(), ADMIN_LOGIN, ADMIN_PASSWORD)));
    }
    return adminWsClient;
  }

  /**
   * @deprecated in 3.15. Instantiate your own client configured with {@link #getUrl()}
   * or use {@link #newHttpCall(String)}
   */
  @Deprecated
  public SonarClient adminWsClient() {
    if (adminSonarClient == null && url != null) {
      adminSonarClient = wsClient(ADMIN_LOGIN, ADMIN_PASSWORD);
    }
    return adminSonarClient;
  }

  /**
   * Restore backup of Quality profile. The profile is created
   * if it does not exist yet, otherwise it is reset.
   */
  public Server restoreProfile(Location backup) {
    Version version = version();
    try (InputStream input = fileSystem.openInputStream(backup)) {
      if (version.isGreaterThanOrEquals("6.0")) {
        newHttpCall("/api/qualityprofiles/restore")
          .setMethod(HttpMethod.MULTIPART_POST)
          .setAdminCredentials()
          .setParam("backup", IOUtils.toString(input, UTF_8))
          .execute();
      } else {
        restoreProfileForOldVersion(new ByteArrayBody(IOUtils.toByteArray(input), ContentType.TEXT_XML, "profile-backup.xml"));
      }
      return this;
    } catch (IOException e) {
      throw new IllegalStateException("Cannot restore Quality profile", e);
    }
  }

  private Server restoreProfileForOldVersion(ContentBody backup) {
    if (url == null) {
      throw new IllegalStateException("Can not restore profiles backup if the server is not started");
    }
    // still no sonar-ws-client for this web service
    DefaultHttpClient client = new DefaultHttpClient();
    try {
      HttpHost targetHost = new HttpHost("localhost", new URL(getUrl()).getPort(), "http");

      HttpParams params = client.getParams();
      HttpConnectionParams.setConnectionTimeout(params, 60_000);
      HttpConnectionParams.setSoTimeout(params, 120_000);

      client.getCredentialsProvider().setCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()),
        new UsernamePasswordCredentials(ADMIN_LOGIN, ADMIN_PASSWORD));

      BasicHttpContext localcontext = new BasicHttpContext();
      BasicScheme basicAuth = new BasicScheme();

      localcontext.setAttribute("preemptive-auth", basicAuth);
      client.addRequestInterceptor(new PreemptiveAuth(), 0);

      String wsUrl = url + "/api/qualityprofiles/restore";
      HttpPost post = new HttpPost(wsUrl);
      MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
      entity.addPart("backup", backup);
      post.setEntity(entity);
      HttpResponse httpResponse = client.execute(post, localcontext);
      int statusCode = httpResponse.getStatusLine().getStatusCode();

      // Versions less than 3.1 request a standard HTTP/HTML service, but not the web service.
      // For this reason we still check the status 302.
      if (statusCode != 200 && statusCode != 302) {
        throw new IllegalStateException("Fail to restore profile backup, status: " + httpResponse.getStatusLine());
      }
      return this;

    } catch (IOException e) {
      throw new IllegalStateException("Fail to restore profile backup", e);

    } finally {
      client.close();
    }
  }

  static final class PreemptiveAuth implements HttpRequestInterceptor {
    @Override
    public void process(
      final HttpRequest request,
      final HttpContext context) throws HttpException {

      AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);

      if (authState.getAuthScheme() == null) {
        AuthScheme authScheme = (AuthScheme) context.getAttribute("preemptive-auth");
        if (authScheme != null) {
          authState.setAuthScheme(authScheme);
          authState.setCredentials(new UsernamePasswordCredentials(ADMIN_LOGIN, ADMIN_PASSWORD));
        }
      }
    }
  }

  /**
   * Provision a new project. The default administrator account is used
   * (login "admin", password "admin")
   * @since 2.17
   */
  public void provisionProject(String projectKey, String projectName) {
    newHttpCall("/api/projects/create")
      .setMethod(HttpMethod.POST)
      .setAdminCredentials()
      .setParam("key", projectKey)
      .setParam("name", projectName)
      .execute();
  }

  /**
   * @deprecated in 3.15. Replaced by {@link #newHttpCall(String)}.
   */
  @Deprecated
  public String post(String relativePath, Map<String, Object> params) {
    HttpCall httpCall = newHttpCall(relativePath);
    params.entrySet().stream()
      .filter(e -> e.getValue() != null)
      .forEach(e -> httpCall.setParam(e.getKey(), e.getValue().toString()));
    com.sonar.orchestrator.http.HttpResponse response = httpCall.setAdminCredentials()
      .setMethod(HttpMethod.POST)
      .execute();
    return response.getBodyAsString();
  }

  /**
   * Create a {@link HttpCall} for the specified path, for instance {@code "/api/system/ping"}.
   * By default HTTP method is {@link HttpMethod#GET} and caller is not authenticated.
   *
   * @param relativePath path for example {@code "api/system/ping"} or {@code "/api/system/ping"}.
   *                     The leading slash is optional. Query parameters must not be defined.
   *                     See {@link HttpCall#setParam(String, String)}.
   * @since 3.15
   */
  public HttpCall newHttpCall(String relativePath) {
    String segments = StringUtils.strip(relativePath, "/");
    return HttpClientFactory.create().newCall(url.newBuilder().addPathSegments(segments).build());
  }

  /**
   * Associate project to a given quality profile for the given language.
   * @since 2.17
   */
  public void associateProjectToQualityProfile(String projectKey, String languageKey, String profileName) {
    newHttpCall("/api/qualityprofiles/add_project")
      .setMethod(HttpMethod.POST)
      .setAdminCredentials()
      .setParam("projectKey", projectKey)
      .setParam("language", languageKey)
      .setParam("profileName", profileName)
      .execute();
  }
}
