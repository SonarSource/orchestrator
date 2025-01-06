/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package com.sonar.orchestrator.container;

import com.sonar.orchestrator.http.HttpCall;
import com.sonar.orchestrator.http.HttpClientFactory;
import com.sonar.orchestrator.http.HttpMethod;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Server {
  public static final String ADMIN_LOGIN = "admin";
  public static final String ADMIN_PASSWORD = "admin";

  private final Locators locators;
  private final File home;
  private final Edition edition;
  private final Version version;
  private final HttpUrl url;
  private final int searchPort;
  @CheckForNull
  private final String clusterNodeName;

  public Server(Locators locators, File home, Edition edition, Version version, HttpUrl url, int searchPort, @Nullable String clusterNodeName) {
    this.locators = locators;
    this.home = home;
    this.edition = edition;
    this.version = version;
    this.url = url;
    this.searchPort = searchPort;
    this.clusterNodeName = clusterNodeName;
  }

  public File getHome() {
    return home;
  }

  public Edition getEdition() {
    return edition;
  }

  /**
   * Server base URL, without trailing slash, for example {@code "http://localhost:9000"}
   * or {@code "http://localhost:9000/sonarqube"}.
   */
  public String getUrl() {
    return StringUtils.stripEnd(url.toString(), "/");
  }

  /**
   * Effective version of SonarQube, for example "6.3.0.1234" but not alias like "DEV".
   */
  public Version version() {
    return version;
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

  public int getSearchPort() {
    return searchPort;
  }

  public Optional<String> getClusterNodeName() {
    return Optional.ofNullable(clusterNodeName);
  }

  /**
   * Restore backup of Quality profile. The profile is created
   * if it does not exist yet, otherwise it is reset.
   */
  public Server restoreProfile(Location backup) {
    try (InputStream input = locators.openInputStream(backup)) {
      newHttpCall("/api/qualityprofiles/restore")
        .setMethod(HttpMethod.MULTIPART_POST)
        .setAdminCredentials()
        .setParam("backup", IOUtils.toString(input, UTF_8))
        .execute();
      return this;
    } catch (IOException e) {
      throw new IllegalStateException("Cannot restore Quality profile", e);
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
      .setParam("project", projectKey)
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
      .setParam("project", projectKey)
      .setParam("language", languageKey)
      .setParam("qualityProfile", profileName)
      .execute();
  }
}
