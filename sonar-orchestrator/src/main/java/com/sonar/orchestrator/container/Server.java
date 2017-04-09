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

import com.google.common.io.Closeables;
import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
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
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.connectors.HttpClient4Connector;

import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang.StringUtils.substringAfter;

public class Server {
  private static final String LOCALHOST = "localhost";
  public static final String ADMIN_LOGIN = "admin";
  public static final String ADMIN_PASSWORD = "admin";
  private static final Logger LOG = LoggerFactory.getLogger(Server.class);

  private final FileSystem fileSystem;
  private final File home;
  private final SonarDistribution distribution;
  private String url;
  private Sonar wsClient;
  private Sonar adminWsClient;
  private SonarClient sonarClient;
  private SonarClient adminSonarClient;

  public Server(FileSystem fileSystem, File home, SonarDistribution distribution) {
    this.fileSystem = fileSystem;
    this.home = home;
    this.distribution = distribution;
  }

  public File getHome() {
    return home;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
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

  public Sonar getWsClient() {
    if (wsClient == null && url != null) {
      wsClient = new Sonar(new HttpClient4Connector(new Host(url)));
    }
    return wsClient;
  }

  /**
   * @since 2.10
   */
  public SonarClient wsClient() {
    if (sonarClient == null && url != null) {
      sonarClient = SonarClient.create(url);
    }
    return sonarClient;
  }

  /**
   * @since 2.10
   */
  public SonarClient wsClient(String login, String password) {
    return SonarClient.builder().url(url).login(login).password(password).build();
  }

  public Sonar getAdminWsClient() {
    if (adminWsClient == null && url != null) {
      adminWsClient = new Sonar(new HttpClient4Connector(new Host(url, ADMIN_LOGIN, ADMIN_PASSWORD)));
    }
    return adminWsClient;
  }

  public SonarClient adminWsClient() {
    if (adminSonarClient == null && url != null) {
      adminSonarClient = wsClient(ADMIN_LOGIN, ADMIN_PASSWORD);
    }
    return adminSonarClient;
  }

  public Server restoreProfile(Location backup) {
    InputStream input = null;
    try {
      input = fileSystem.openInputStream(backup);
      return restoreProfiles(new ByteArrayBody(IOUtils.toByteArray(input), ContentType.TEXT_XML, "profile-backup.xml"));

    } catch (IOException e) {
      throw new IllegalStateException(e);

    } finally {
      Closeables.closeQuietly(input);
    }
  }

  Server restoreProfiles(ContentBody backup) {
    LOG.info("Restoring profiles: {}", backup);
    if (url == null) {
      throw new IllegalStateException("Can not restore profiles backup if the server is not started");
    }
    // still no sonar-ws-client for this web service
    DefaultHttpClient client = new DefaultHttpClient();
    try {
      HttpHost targetHost = new HttpHost(LOCALHOST, new URL(url).getPort(), "http");

      HttpParams params = client.getParams();
      HttpConnectionParams.setConnectionTimeout(params, 60_000);
      HttpConnectionParams.setSoTimeout(params, 120_000);

      client.getCredentialsProvider().setCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()), new UsernamePasswordCredentials(ADMIN_LOGIN, ADMIN_PASSWORD));

      BasicHttpContext localcontext = new BasicHttpContext();
      BasicScheme basicAuth = new BasicScheme();
      localcontext.setAttribute("preemptive-auth", basicAuth);
      client.addRequestInterceptor(new PreemptiveAuth(), 0);

      String wsUrl = url + "/api/qualityprofiles/restore";
      LOG.info("POST {}", wsUrl);
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

  /**
   * Provision a new project.
   * @since 2.17
   */
  public void provisionProject(String projectKey, String projectName) {
    adminWsClient().post("/api/projects/create",
      "key", projectKey,
      "name", projectName);
  }

  public String post(String relativeUrl, Map<String, Object> params) {
    return adminWsClient().post(relativeUrl, params);
  }

  /**
   * Associate project to a given quality profile for the given language.
   * @since 2.17
   */
  public void associateProjectToQualityProfile(String projectKey, String languageKey, String profileName) {
    adminWsClient().post("api/qualityprofiles/add_project",
      "projectKey", projectKey,
      "language", languageKey,
      "profileName", profileName);
  }

  static final class PreemptiveAuth implements HttpRequestInterceptor {
    @Override
    public void process(
      final HttpRequest request,
      final HttpContext context) throws HttpException {

      AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);

      if (authState.getAuthScheme() == null) {
        AuthScheme authScheme = (AuthScheme) context.getAttribute("preemptive-auth");
        CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
        HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
        if (authScheme != null) {
          Credentials creds = credsProvider.getCredentials(
            new AuthScope(targetHost.getHostName(), targetHost.getPort()));
          if (creds == null) {
            throw new HttpException("No credentials for preemptive authentication");
          }
          authState.setAuthScheme(authScheme);
          authState.setCredentials(creds);
        }
      }
    }
  }
}
