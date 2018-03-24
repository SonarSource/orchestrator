/*
 * Orchestrator
 * Copyright (C) 2011-2018 SonarSource SA
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
import com.sonar.orchestrator.container.SonarDistribution;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.locator.PluginLocation;
import com.sonar.orchestrator.locator.ResourceLocation;
import com.sonar.orchestrator.locator.URLLocation;
import com.sonar.orchestrator.server.StartupLogWatcher;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.UpdateCenterDeserializer;
import org.sonar.updatecenter.common.UpdateCenterDeserializer.Mode;

import static com.sonar.orchestrator.util.OrchestratorUtils.checkArgument;
import static com.sonar.orchestrator.util.OrchestratorUtils.checkState;
import static com.sonar.orchestrator.util.OrchestratorUtils.defaultIfEmpty;
import static com.sonar.orchestrator.util.OrchestratorUtils.defaultIfNull;
import static com.sonar.orchestrator.util.OrchestratorUtils.isEmpty;
import static java.util.Objects.requireNonNull;

public class OrchestratorBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(OrchestratorBuilder.class);

  private static final String ALIAS_LTS_OR_OLDEST_COMPATIBLE = "LTS_OR_OLDEST_COMPATIBLE";
  private static final String ALIAS_OLDEST_COMPATIBLE = "OLDEST_COMPATIBLE";

  private final Configuration config;
  private final SonarDistribution distribution;
  private final Map<String, String> overriddenProperties;
  private UpdateCenter updateCenter;
  private StartupLogWatcher startupLogWatcher;

  OrchestratorBuilder(Configuration initialConfig) {
    this.config = initialConfig;
    this.distribution = new SonarDistribution();
    this.overriddenProperties = new HashMap<>();
  }

  public OrchestratorBuilder setZipFile(File zip) {
    checkArgument(zip.exists(), "SonarQube ZIP file does not exist: %s", zip.getAbsolutePath());
    checkArgument(zip.isFile(), "SonarQube ZIP is not a file: %s", zip.getAbsolutePath());
    this.distribution.setZipFile(zip);
    return this;
  }

  public OrchestratorBuilder setSonarVersion(String s) {
    checkArgument(!isEmpty(s), "Empty SonarQube version");
    this.overriddenProperties.put(Configuration.SONAR_VERSION_PROPERTY, s);
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
   * Resolve SonarQube version base on value of property sonar.runtimeVersion. Some aliases can be used:
   * <ul>
   *   <li>DEV: Return dev version of SonarQube as defined in update center</li>
   *   <li>LTS: Return LTS version of SonarQube as defined in update center</li>
   * </ul>
   */
  public Optional<String> getSonarVersion() {
    String requestedVersion = getOrchestratorProperty(Configuration.SONAR_VERSION_PROPERTY);
    if (isEmpty(requestedVersion)) {
      return Optional.empty();
    }
    if (ALIAS_LTS_OR_OLDEST_COMPATIBLE.equals(requestedVersion)) {
      throw new IllegalArgumentException("Alias '" + ALIAS_LTS_OR_OLDEST_COMPATIBLE + "' is not supported anymore for SonarQube versions");
    }
    String version;
    try {
      Release sonarRelease = getUpdateCenter().getSonar().getRelease(requestedVersion);
      version = sonarRelease.getVersion().toString();
    } catch (NoSuchElementException e) {
      LOG.warn("Version " + requestedVersion + " of SonarQube does not exist in update center. Fallback to blindly use " + requestedVersion);
      version = requestedVersion;
    }
    setOrchestratorProperty(Configuration.SONAR_VERSION_PROPERTY, version);
    return Optional.of(version);
  }

  public String getPluginVersion(String pluginKey) {
    Release pluginRelease = getPluginRelease(pluginKey);
    return pluginRelease.getVersion().toString();
  }

  private String getRequestedPluginVersion(String pluginKey) {
    String pluginVersionPropertyKey = getPluginVersionPropertyKey(pluginKey);
    String pluginVersion = getOrchestratorProperty(pluginVersionPropertyKey);
    checkState(!isEmpty(pluginVersion), "Missing %s plugin version. Please define property %s", pluginKey, pluginVersionPropertyKey);
    return pluginVersion;
  }

  private static String getPluginVersionPropertyKey(String pluginKey) {
    return pluginKey + "Version";
  }

  private Release getPluginRelease(String pluginKey) {
    Release resolvedRelease = resolvePluginVersion(pluginKey, getRequestedPluginVersion(pluginKey));
    // Override plugin version with actual value to allow later check (assumeThat)
    setOrchestratorProperty(getPluginVersionPropertyKey(pluginKey), resolvedRelease.getVersion().toString());
    return resolvedRelease;
  }

  private Release resolvePluginVersion(String pluginKey, String version) {
    if (ALIAS_OLDEST_COMPATIBLE.equals(version)) {
      throw new IllegalArgumentException("Alias " + ALIAS_OLDEST_COMPATIBLE + " is not supported anymore (plugin " + pluginKey + ")");
    }
    Release release = getUpdateCenter().getUpdateCenterPluginReferential().findPlugin(pluginKey).getRelease(version);
    if (release == null) {
      throw new IllegalStateException("Unable to resolve " + pluginKey + " plugin version " + version);
    }
    return release;
  }

  public UpdateCenter getUpdateCenter() {
    if (updateCenter != null) {
      return updateCenter;
    }
    File updateCenterFile = null;
    try {
      updateCenterFile = File.createTempFile("update-center", ".properties");
      new Locators(config).copyToFile(URLLocation.create(getUpdateCenterUrl()), updateCenterFile);
      Properties props = new Properties();
      try (Reader propsReader = new FileReader(updateCenterFile)) {
        props.load(propsReader);
      }
      updateCenter = new UpdateCenterDeserializer(Mode.DEV, false).fromProperties(props);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read update center properties file", e);
    } finally {
      FileUtils.deleteQuietly(updateCenterFile);
    }
    return updateCenter;
  }

  private URL getUpdateCenterUrl() {
    String url = defaultIfEmpty(getOrchestratorProperty("orchestrator.updateCenterUrl"), "http://update.sonarsource.org/update-center-dev.properties");
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      throw new IllegalStateException(url + " is not a valid URL for update center", e);
    }
  }

  /**
   * Install a plugin. Examples :
   * <ul>
   *   <li>{@code addPlugin(MavenLocation.create("org.codehaus.sonar-plugins", "sonar-foo-plugin", "1.0")} seeks in local then remote repository</li>
   *   <li>{@code addPlugin(FileLocation.of("/path/to/jar")} installs the given JAR file</li>
   *   <li>{@code addPlugin(PluginLocation.of("foo")} seeks in local repo, remote repo then update center. The property "fooVersion" must be set in configuration.</li>
   * </ul>
   */
  public OrchestratorBuilder addPlugin(Location location) {
    requireNonNull(location);
    distribution.addPluginLocation(location);
    return this;
  }

  /**
   * Install a plugin that is available in Maven repositories. The version of the plugin is passed using an orchestrator property.
   * This method doesn't use update center and as a result do not support version aliases like "RELEASE" and "SNAPSHOT"
   * nor plugin groups.
   * <p/>
   * Use {@link #addPlugin(String)} if you want to use advanced features provided by update center.
   * <p/>
   * Example: {@code addMavenPlugin("org.sonarsource.ldap", "sonar-ldap-plugin", "ldapVersion")}
   */
  public OrchestratorBuilder addMavenPlugin(String groupId, String artifactId, String versionPropertyKey) {
    String version = getOrchestratorProperty(versionPropertyKey);
    requireNonNull(version, "Property " + versionPropertyKey + " is not defined");
    distribution.addPluginLocation(MavenLocation.create(groupId, artifactId, version));
    return this;
  }

  /**
   * Install a plugin by its key. First groupId/artifactId is determined using update center.
   * Once Maven coordinates are known the plugin is located in local Maven repository.<br/>
   * If not found there will be an attempt to download it.<br/>
   * If still not found URL defined in update center will be used.<br/>
   * <p/>
   * This method requires the property &lt;pluginKey&gt;Version to be set, for example "javaVersion". Aliases like DEV are supported.
   * <p/>
   * Example: {@code addPlugin("java")}
   *
   * @since 2.10
   */
  public OrchestratorBuilder addPlugin(String pluginKey) {
    String version = getPluginVersion(pluginKey);

    addPluginLocation(pluginKey, version);

    return this;
  }

  private void addPluginLocation(String pluginKey, String version) {
    try {
      Release r = updateCenter.getUpdateCenterPluginReferential().findPlugin(pluginKey).getRelease(version);
      String groupId = r.groupId();
      String artifactId = r.artifactId();
      distribution.addPluginLocation(PluginLocation.create(pluginKey, version, groupId, artifactId));
    } catch (NoSuchElementException e) {
      throw new IllegalStateException("Unable to find plugin " + pluginKey + " version " + version + " in update center", e);
    }
  }

  public OrchestratorBuilder setOrchestratorProperty(String key, @Nullable String value) {
    checkNotEmpty(key);
    overriddenProperties.put(key, value);
    return this;
  }

  public String getOrchestratorProperty(String key) {
    return defaultIfNull(overriddenProperties.get(key), config.getString(key));
  }

  public Configuration getOrchestratorConfiguration() {
    return this.config;
  }

  public OrchestratorBuilder setServerProperty(String key, @Nullable String value) {
    checkNotEmpty(key);
    distribution.setServerProperty(key, value);
    return this;
  }

  private static void checkNotEmpty(String key) {
    checkArgument(!isEmpty(key), "Empty property key");
  }

  public String getServerProperty(String key) {
    return distribution.getServerProperty(key);
  }

  public OrchestratorBuilder restoreProfileAtStartup(Location profileBackup) {
    distribution.restoreProfileAtStartup(profileBackup);
    return this;
  }

  /**
   * Installs a development license that unlocks the SonarSource commercial plugins
   * built for SonarQube 6.7 LTS.
   *
   * @since 3.15
   */
  public OrchestratorBuilder activateLicense() {
    distribution.activateLicense();
    return this;
  }

  /**
   * Set the root context for the webapp. Default value is {@code ""}. Value cannot be
   * overridden on version 5.4 of SonarQube (feature was disabled then re-introduced
   * in 5.5, see https://jira.sonarsource.com/browse/SONAR-7122 and
   * https://jira.sonarsource.com/browse/SONAR-7494)
   * @since 2.8
   * @deprecated in 3.15. Use the property "sonar.web.context" via {@link #setServerProperty(String, String)}.
   */
  @Deprecated
  public OrchestratorBuilder setContext(String context) {
    setServerProperty("sonar.web.context", context);
    return this;
  }

  /**
   * Remove all plugins distributed with SonarQube (ie extensions/plugins/*)
   * @since 2.9
   * @deprecated removal becomes the default since 3.0. Indeed now any plugin must be load explicitly
   */
  @Deprecated
  public OrchestratorBuilder removeDistributedPlugins() {
    distribution.setRemoveDistributedPlugins(true);
    return this;
  }

  public Orchestrator build() {
    getSonarVersion().ifPresent(s -> this.distribution.setVersion(Version.create(s)));
    checkState(distribution.getZipFile().isPresent() || distribution.version().isPresent(), "Version or path to ZIP of SonarQube is missing");
    Configuration.Builder configBuilder = Configuration.builder();
    Configuration finalConfig = configBuilder
      .addConfiguration(config)
      .addMap(overriddenProperties)
      .setUpdateCenter(getUpdateCenter())
      .build();

    this.distribution.addPluginLocation(ResourceLocation.create("/com/sonar/orchestrator/sonar-reset-data-plugin-1.0-SNAPSHOT.jar"));
    return new Orchestrator(finalConfig, distribution, startupLogWatcher);
  }
}
