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
import java.util.Map;
import okhttp3.HttpUrl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.connectors.HttpClient4Connector;

import static com.google.common.base.Preconditions.checkState;
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
      wsClient = new Sonar(new HttpClient4Connector(new Host(url.toString())));
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
      sonarClient = SonarClient.create(url.toString());
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
    return SonarClient.builder().url(url.toString()).login(login).password(password).build();
  }

  /**
   * @deprecated in 3.15. Instantiate your own client configured with {@link #getUrl()}
   * or use {@link #newHttpCall(String)}
   */
  @Deprecated
  public Sonar getAdminWsClient() {
    if (adminWsClient == null && url != null) {
      adminWsClient = new Sonar(new HttpClient4Connector(new Host(url.toString(), ADMIN_LOGIN, ADMIN_PASSWORD)));
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
    try (InputStream input = fileSystem.openInputStream(backup)) {
      newHttpCall("/api/qualityprofiles/restore")
        .setMethod(HttpMethod.POST)
        .setAdminCredentials()
        .setParam("backup", IOUtils.toString(input, UTF_8))
        .execute();
      return this;
    } catch (IOException e) {
      throw new IllegalStateException("Cannot restore Quality profile", e);
    }
  }

  /**
   * Provision a new project.
   * @since 2.17
   */
  public void provisionProject(String projectKey, String projectName) {
    newHttpCall("/api/projects/create")
      .setParam("key", projectKey)
      .setParam("name", projectName)
      .setAdminCredentials()
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
