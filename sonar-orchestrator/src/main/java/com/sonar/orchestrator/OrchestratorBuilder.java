/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
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
import com.sonar.orchestrator.util.System2;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

import static com.sonar.orchestrator.util.OrchestratorUtils.checkArgument;
import static com.sonar.orchestrator.util.OrchestratorUtils.checkState;
import static com.sonar.orchestrator.util.OrchestratorUtils.isEmpty;
import static java.util.Objects.requireNonNull;

public abstract class OrchestratorBuilder<BUILDER extends OrchestratorBuilder<BUILDER, ORCH>, ORCH> {

  private final Configuration config;
  private final System2 system2;
  private final SonarDistribution distribution;
  private final Map<String, String> overriddenProperties;
  private StartupLogWatcher startupLogWatcher;

  OrchestratorBuilder(Configuration initialConfig) {
    this(initialConfig, System2.INSTANCE);
  }

  protected OrchestratorBuilder(Configuration initialConfig, System2 system2) {
    this.config = initialConfig;
    this.system2 = system2;
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
  public BUILDER setZipFile(File zip) {
    checkArgument(zip.exists(), "SonarQube ZIP file does not exist: %s", zip.getAbsolutePath());
    checkArgument(zip.isFile(), "SonarQube ZIP is not a file: %s", zip.getAbsolutePath());
    return setZipLocation(FileLocation.of(zip));
  }

  /**
   * Set the path to SonarQube zip by its location, for instance {@link FileLocation} or {@link MavenLocation}
   * @since 3.19
   */
  public BUILDER setZipLocation(Location zip) {
    this.distribution.setZipLocation(requireNonNull(zip));
    return (BUILDER) this;
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
  public BUILDER setSonarVersion(String s) {
    checkArgument(!isEmpty(s), "Empty SonarQube version");
    this.distribution.setVersion(s);
    return (BUILDER) this;
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
  public BUILDER setStartupLogWatcher(@Nullable StartupLogWatcher w) {
    this.startupLogWatcher = w;
    return (BUILDER) this;
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
   * (see parameter "orchestrator.artifactory.accessToken").
   */
  public BUILDER addPlugin(Location location) {
    distribution.addPluginLocation(requireNonNull(location));
    return (BUILDER) this;
  }

  /**
   * Similar to {@link #addPlugin} but installs the plugin in the directory for bundled plugins.
   * Only supported by SQ 8.5+
   */
  public BUILDER addBundledPlugin(Location location) {
    distribution.addBundledPluginLocation(requireNonNull(location));
    return (BUILDER) this;
  }

  public BUILDER setOrchestratorProperty(String key, @Nullable String value) {
    checkNotEmpty(key);
    overriddenProperties.put(key, value);
    return (BUILDER) this;
  }

  public BUILDER setServerProperty(String key, @Nullable String value) {
    checkNotEmpty(key);
    distribution.setServerProperty(key, value);
    return (BUILDER) this;
  }

  /**
   * Enable JDWP agent in the CE process for remote debugging on port 5006
   */
  public BUILDER enableCeDebug() {
    failIfRunningOnCI();
    this.setServerProperty("sonar.ce.javaAdditionalOpts", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5006");
    return (BUILDER) this;

  }

  /**
   * Enable JDWP agent in the web process for remote debugging on port 5005
   */
  public BUILDER enableWebDebug() {
    failIfRunningOnCI();
    this.setServerProperty("sonar.web.javaAdditionalOpts", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005");
    return (BUILDER) this;
  }

  private void failIfRunningOnCI() {
    if ("true".equals(system2.getenv("CIRRUS_CI"))) {
      throw new IllegalStateException("Method shouldn't be called on CI");
    }
  }

  /**
   * Orchestrator will start with clean sonar.properties file
   */
  public BUILDER emptySonarProperties() {
    distribution.setEmptySonarProperties(true);
    return (BUILDER) this;
  }

  /**
   * Fallback to default behaviour of force authentication
   *
   * Starting from 8.6 it has been enforced, but due impact on others it will be disabled by default
   */
  public BUILDER defaultForceAuthentication() {
    distribution.setDefaultForceAuthentication(true);
    return (BUILDER) this;
  }

  /**
   * Fallback to default behaviour of force redirect to change admin password
   *
   * Starting from 8.8 it has been enforced, but due impact on ITs, it will be disabled by default.
   */
  public BUILDER defaultForceDefaultAdminCredentialsRedirect() {
    distribution.setForceDefaultAdminCredentialsRedirect(true);
    return (BUILDER) this;
  }

  /**
   * Sets default admin token for all build executions if 'sonar.login' property not provided.
   *
   * Starting from 9.8, permissions for 'Anyone' group has been limited for new instances.
   */
  public BUILDER useDefaultAdminCredentialsForBuilds(boolean defaultAdminCredentialsForBuilds) {
    distribution.useDefaultAdminCredentialsForBuilds(defaultAdminCredentialsForBuilds);
    return (BUILDER) this;
  }

  /**
   * SonarSource commercial plugins must be enabled through a non-community edition.
   * By default community edition is installed. Method is ignored on SonarQube
   * versions less than 7.2.
   *
   * @since 3.19
   */
  public BUILDER setEdition(Edition edition) {
    distribution.setEdition(edition);
    return (BUILDER) this;
  }

  private static void checkNotEmpty(String key) {
    checkArgument(!isEmpty(key), "Empty property key");
  }

  public BUILDER restoreProfileAtStartup(Location profileBackup) {
    distribution.restoreProfileAtStartup(profileBackup);
    return (BUILDER) this;
  }

  /**
   * Installs a development license that unlocks the SonarSource commercial plugins.
   * Can be called only by SonarSource projects.
   *
   * @since 3.15
   */
  public BUILDER activateLicense() {
    distribution.activateLicense();
    return (BUILDER) this;
  }

  /**
   * Keeps all bundled plugins
   */
  public BUILDER keepBundledPlugins() {
    distribution.setKeepBundledPlugins(true);
    return (BUILDER) this;
  }

  /**
   * Add a bundled plugin to be kept.
   * By default, all bundled plugins are removed, unless {@link #keepBundledPlugins()} is called,
   * in which case all plugins are kept.
   * @param pluginJarNamePrefix File name prefix of the plugin jar file to be kept. For example, 'sonar-java'.
   */
  public BUILDER addBundledPluginToKeep(String pluginJarNamePrefix) {
    distribution.addBundledPluginToKeep(pluginJarNamePrefix);
    return (BUILDER) this;
  }

  public ORCH build() {
    checkState(distribution.getZipLocation().isPresent() ^ distribution.getVersion().isPresent(),
      "One, and only one, of methods setSonarVersion(String) or setZipFile(File) must be called");
    Configuration.Builder configBuilder = Configuration.builder();
    Configuration finalConfig = configBuilder
      .addConfiguration(config)
      .addMap(overriddenProperties)
      .build();

    return build(finalConfig, distribution, startupLogWatcher);
  }

  protected abstract ORCH build(Configuration finalConfig, SonarDistribution distribution, StartupLogWatcher startupLogWatcher);
}
