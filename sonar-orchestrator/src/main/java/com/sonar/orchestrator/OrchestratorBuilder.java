/*
 * Orchestrator
 * Copyright (C) 2011-2020 SonarSource SA
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
package com.sonar.orchestrator;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.container.SonarDistribution;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.server.StartupLogWatcher;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

import static com.sonar.orchestrator.util.OrchestratorUtils.checkArgument;
import static com.sonar.orchestrator.util.OrchestratorUtils.checkState;
import static com.sonar.orchestrator.util.OrchestratorUtils.isEmpty;
import static java.util.Objects.requireNonNull;

public class OrchestratorBuilder {

  private final Configuration config;
  private final SonarDistribution distribution;
  private final Map<String, String> overriddenProperties;
  private StartupLogWatcher startupLogWatcher;

  OrchestratorBuilder(Configuration initialConfig) {
    this.config = initialConfig;
    this.distribution = new SonarDistribution();
    this.overriddenProperties = new HashMap<>();
  }

  /**
   * Set the local distribution of SonarQube to be installed.
   * <p/>
   * Only one of methods {@link #setSonarVersion(String)} and {@link #setZipFile(File)} must be called.
   *
   * @throws IllegalArgumentException if the zip file does not exist
   */
  public OrchestratorBuilder setZipFile(File zip) {
    checkArgument(zip.exists(), "SonarQube ZIP file does not exist: %s", zip.getAbsolutePath());
    checkArgument(zip.isFile(), "SonarQube ZIP is not a file: %s", zip.getAbsolutePath());
    return setZipLocation(FileLocation.of(zip));
  }

  /**
   * Set the path to SonarQube zip by its location, for instance {@link FileLocation} or {@link MavenLocation}
   * @since 3.19
   */
  public OrchestratorBuilder setZipLocation(Location zip) {
    this.distribution.setZipLocation(requireNonNull(zip));
    return this;
  }

  /**
   * Set the version of SonarQube to be installed. Can be:
   * <ul>
   *   <li>a fixed version like {@code "7.1.0.1234"} or {@code "7.1"} (GA release)</li>
   *   <li>the alias {@code "DEV"} for the latest official build that has been promoted (validated by QA).
   *   Build comes from master branch, not from feature branches</li>
   *   <li>the alias {@code "DEV[x.y]"}, same as "DEV" but restricted to series x.y.*. For example {@code "DEV[7.1]"} may
   *   install the version 7.1.0.1234.</li>
   *   <li>the alias {@code "LATEST_RELEASE"} for the latest official release</li>
   *   <li>the alias {@code "LATEST_RELEASE[x.y]"}, same as {@code "LATEST_RELEASE"} but restricted to
   *   series x.y.*. For example {@code "LATEST_RELEASE[7.1]"} may install the version 7.1.1.</li>
   * </ul>
   * The term "latest" refers to the highest version number, not the more recently published version.
   * <p/>
   * The alias {@code "LTS"} is no more supported. It should be replaced by {@code "LATEST_RELEASE[6.7]"} if
   * the LTS series is 6.7.x.
   * <p/>
   * Only one of methods {@link #setSonarVersion(String)} and {@link #setZipFile(File)} must be called.
   * <p/>Since version 3.17, the property "sonar.runtimeVersion" is no longer automatically
   * supported. The caller is responsible for loading the version of SonarQube from wherever it
   * needs.
   */
  public OrchestratorBuilder setSonarVersion(String s) {
    checkArgument(!isEmpty(s), "Empty SonarQube version");
    this.distribution.setVersion(s);
    return this;
  }

  /**
   * Ability to watch server logs at startup. The watcher is responsible for
   * detecting when server is operational. It allows for example:
   * <ul>
   *   <li>to test clustering, for example by starting a single Elasticsearch
   *   node. Server is considered as started when the log "Process[es] is up" is displayed.</li>
   *   <li>to test upgrade, as the server is stuck as long as database model
   *   is not upgraded</li>
   *   <li>to parse the listening port of Elasticsearch as a random port
   *   is used by Orchestrator</li>
   * </ul>
   *
   * @since 3.13
   */
  public OrchestratorBuilder setStartupLogWatcher(@Nullable StartupLogWatcher w) {
    this.startupLogWatcher = w;
    return this;
  }

  /**
   * Install a plugin before starting SonarQube server. Location of plugin may be:
   * <ul>
   *   <li>a local JAR file: {@code addPlugin(FileLocation.of("/path/to/jar")}</li>
   *   <li>a Maven ID: {@code addPlugin(MavenLocation.of("org.sonarsource.java", "sonar-java-plugin", "5.2.0.13398")}.
   *   Artifact is searched from local cache (~/.sonar/orchestrator/cache), from local Maven repository
   *   (if exists) then from Artifactory.</li>
   * </ul>
   * <p/>
   * Using the Maven ID allows to reference version by an alias:
   * <ul>
   *   <li>the alias {@code "DEV"} for the latest official build that has been promoted (validated by QA). Build comes from master branch, not from feature branches</li>
   *   <li>the alias {@code "DEV[x.y]"}, same as "DEV" but restricted to series x.y.*. For example {@code "DEV[5.2]"} may install the
   *   version 5.2.0.13398 which is latest promoted build of 5.2 series.</li>
   *   <li>the alias {@code "LATEST_RELEASE"} for the latest official release</li>
   *   <li>the alias {@code "LATEST_RELEASE[x.y]"}, same as {@code "LATEST_RELEASE"} but restricted to series x.y.*, for example {@code "LATEST_RELEASE[5.2]"}.</li>
   * </ul>
   * <p/>
   * Downloading and resolving aliases of commercial plugins requires the Artifactory credentials to be set
   * (see parameter "orchestrator.artifactory.apiKey").
   */
  public OrchestratorBuilder addPlugin(Location location) {
    distribution.addPluginLocation(requireNonNull(location));
    return this;
  }

  /**
   * Similar to {@link #addPlugin} but installs the plugin in the directory for bundled plugins.
   * Only supported by SQ 8.5+
   */
  public OrchestratorBuilder addBundledPlugin(Location location) {
    distribution.addBundledPluginLocation(requireNonNull(location));
    return this;
  }

  public OrchestratorBuilder setOrchestratorProperty(String key, @Nullable String value) {
    checkNotEmpty(key);
    overriddenProperties.put(key, value);
    return this;
  }

  public OrchestratorBuilder setServerProperty(String key, @Nullable String value) {
    checkNotEmpty(key);
    distribution.setServerProperty(key, value);
    return this;
  }

  public OrchestratorBuilder disableForceAuthentication() {
    distribution.setServerProperty("sonar.forceAuthentication", "false");
    return this;
  }

  /**
   * SonarSource commercial plugins must be enabled through a non-community edition.
   * By default community edition is installed. Method is ignored on SonarQube
   * versions less than 7.2.
   *
   * @since 3.19
   */
  public OrchestratorBuilder setEdition(Edition edition) {
    distribution.setEdition(edition);
    return this;
  }

  private static void checkNotEmpty(String key) {
    checkArgument(!isEmpty(key), "Empty property key");
  }

  public OrchestratorBuilder restoreProfileAtStartup(Location profileBackup) {
    distribution.restoreProfileAtStartup(profileBackup);
    return this;
  }

  /**
   * Installs a development license that unlocks the SonarSource commercial plugins.
   * Can be called only by SonarSource projects.
   *
   * @since 3.15
   */
  public OrchestratorBuilder activateLicense() {
    distribution.activateLicense();
    return this;
  }

  public OrchestratorBuilder keepBundledPlugins() {
    distribution.setKeepBundledPlugins(true);
    return this;
  }

  public Orchestrator build() {
    checkState(distribution.getZipLocation().isPresent() ^ distribution.getVersion().isPresent(),
      "One, and only one, of methods setSonarVersion(String) or setZipFile(File) must be called");
    Configuration.Builder configBuilder = Configuration.builder();
    Configuration finalConfig = configBuilder
      .addConfiguration(config)
      .addMap(overriddenProperties)
      .build();

    return new Orchestrator(finalConfig, distribution, startupLogWatcher);
  }
}
