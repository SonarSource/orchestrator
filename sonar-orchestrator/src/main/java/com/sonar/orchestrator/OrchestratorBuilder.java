/*
 * Orchestrator
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.container.SonarDistribution;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.locator.PluginLocation;
import com.sonar.orchestrator.locator.ResourceLocation;
import com.sonar.orchestrator.locator.URLLocation;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.UpdateCenterDeserializer;
import org.sonar.updatecenter.common.UpdateCenterDeserializer.Mode;

public class OrchestratorBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(OrchestratorBuilder.class);

  private static final String LTS_OR_OLDEST_COMPATIBLE = "LTS_OR_OLDEST_COMPATIBLE";

  private final Configuration config;
  private final SonarDistribution distribution;
  private final Map<String, String> overriddenProperties;
  private UpdateCenter updateCenter;
  private String mainPluginKey;

  OrchestratorBuilder(Configuration initialConfig) {
    this.config = initialConfig;
    this.distribution = new SonarDistribution();
    this.overriddenProperties = Maps.newHashMap();
  }

  public OrchestratorBuilder setSonarVersion(String s) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(s), "Empty SonarQube version");
    this.overriddenProperties.put(Configuration.SONAR_VERSION_PROPERTY, s);
    return this;
  }

  /**
   * Resolve SonarQube version base on value of property sonar.runtimeVersion. Some aliases can be used:
   * <ul>
   *   <li>DEV: Return dev version of SonarQube as defined in update center</li>
   *   <li>LTS: Return LTS version of SonarQube as defined in update center</li>
   *   <li>LTS_OR_OLDEST_COMPATIBLE: Return LTS version of SonarQube if compatible with main plugin. If not
   *   compatible then fallback to use oldest version of SQ compatible with main plugin (including dev version of SQ)</li>
   * </ul>
   * @see #setMainPluginKey(String)
   */
  public String getSonarVersion() {
    String requestedVersion = getOrchestratorProperty(Configuration.SONAR_VERSION_PROPERTY);
    if (StringUtils.isBlank(requestedVersion)) {
      throw new IllegalStateException("Missing SonarQube version. Please define property " + Configuration.SONAR_VERSION_PROPERTY);
    }
    String version;
    if (LTS_OR_OLDEST_COMPATIBLE.equals(requestedVersion)) {
      version = getLtsOrOldestCompatible();
    } else {
      try {
        Release sonarRelease = getUpdateCenter().getSonar().getRelease(requestedVersion);
        version = sonarRelease.getVersion().toString();
      } catch (NoSuchElementException e) {
        LOG.warn("Version " + requestedVersion + " of SonarQube was not found in update center. Fallback to blindly use " + requestedVersion);
        version = requestedVersion;
      }
    }
    setOrchestratorProperty("sonar.runtimeVersion", version);
    return version;
  }

  private String getLtsOrOldestCompatible() {
    if (StringUtils.isBlank(mainPluginKey)) {
      throw new IllegalStateException("You must define the main plugin when using " + LTS_OR_OLDEST_COMPATIBLE + " alias as SQ version");
    }
    org.sonar.updatecenter.common.Version sqLts = getUpdateCenter().getSonar().getLtsRelease().getVersion();
    Plugin mainPlugin = getUpdateCenter().getUpdateCenterPluginReferential().findPlugin(mainPluginKey);
    Release mainPluginRelease = mainPlugin.getRelease(getRequestedPluginVersion(mainPluginKey));
    if (mainPluginRelease.supportSonarVersion(sqLts)) {
      return sqLts.toString();
    } else {
      return mainPluginRelease.getMinimumRequiredSonarVersion().toString();
    }
  }

  public String getPluginVersion(String pluginKey) {
    Release pluginRelease = getPluginRelease(pluginKey);
    return pluginRelease.getVersion().toString();
  }

  private String getRequestedPluginVersion(String pluginKey) {
    String pluginVersionPropertyKey = getPluginVersionPropertyKey(pluginKey);
    String pluginVersion = getOrchestratorProperty(pluginVersionPropertyKey);
    if (StringUtils.isBlank(pluginVersion)) {
      throw new IllegalStateException("Missing " + pluginKey + " plugin version. Please define property " + pluginVersionPropertyKey);
    }
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
    if ("OLDEST_COMPATIBLE".equals(version)) {
      Plugin plugin = getUpdateCenter().getUpdateCenterPluginReferential().findPlugin(pluginKey);
      String sonarVersion = getSonarVersion();
      for (Release r : plugin.getAllReleases()) {
        if (r.supportSonarVersion(org.sonar.updatecenter.common.Version.create(sonarVersion)) && !r.isArchived()) {
          return r;
        }
      }
      throw new IllegalStateException("No version of " + pluginKey + " plugin is compatible with SonarQube " + sonarVersion);
    }
    Release release = getUpdateCenter().getUpdateCenterPluginReferential().findPlugin(pluginKey).getRelease(version);
    if (release == null) {
      throw new IllegalStateException("Unable to resolve " + pluginKey + " plugin version " + version);
    }
    return release;
  }

  public UpdateCenter getUpdateCenter() {
    if (updateCenter == null) {
      File updateCenterFile = null;
      try {
        updateCenterFile = File.createTempFile("update-center", ".properties");
        new Locators(config).copyToFile(URLLocation.create(getUpdateCenterUrl()), updateCenterFile);
        updateCenter = new UpdateCenterDeserializer(Mode.DEV, false).fromSingleFile(updateCenterFile);
      } catch (IOException e) {
        throw new IllegalStateException("Unable to read update center properties file", e);
      } finally {
        FileUtils.deleteQuietly(updateCenterFile);
      }
    }
    return updateCenter;
  }

  private URL getUpdateCenterUrl() {
    String url = StringUtils.defaultIfBlank(getOrchestratorProperty("orchestrator.updateCenterUrl"), "http://update.sonarsource.org/update-center.properties");
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
    Preconditions.checkNotNull(location);
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
    Preconditions.checkNotNull(version, "Property " + versionPropertyKey + " is not defined");
    distribution.addPluginLocation(MavenLocation.create(groupId, artifactId, version));
    return this;
  }

  /**
   * Install a plugin by its key. First groupId/artifactId is determined using update center.
   * Once Maven coordinates are known the plugin is located in local Maven repository.<br/>
   * If not found there will be an attempt to download it from Nexus.<br/>
   * If still not found URL defined in update center will be used.<br/>
   * <p/>
   * If plugin is the parent of an ecosystem (java, dotnet, ...) then all child plugins will also be installed using the same algorithm.
   * <p/>
   * This method requires the property &lt;pluginKey&gt;Version to be set, for example "fortifyVersion". Aliases like DEV and OLDEST_COMPATIBLE are supported.
   * <p/>
   * Example: {@code addPlugin("fortify")}
   *
   * @since 2.10
   */
  public OrchestratorBuilder addPlugin(String pluginKey) {
    String version = getPluginVersion(pluginKey);

    addPluginLocation(pluginKey, version);

    // Now try to install all other plugins if it is an ecosystem
    Release release = getPluginRelease(pluginKey);
    if (release != null) {
      for (Release children : release.getChildren()) {
        addPluginLocation(children.getKey(), version);
        // Set plugin version with actual value to allow later check (assumeThat)
        setOrchestratorProperty(getPluginVersionPropertyKey(children.getKey()), version);
      }
    }
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
    return StringUtils.defaultString(overriddenProperties.get(key), config.getString(key));
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
    Preconditions.checkArgument(!Strings.isNullOrEmpty(key), "Empty property key");
  }

  public String getServerProperty(String key) {
    return distribution.getServerProperty(key);
  }

  public OrchestratorBuilder restoreProfileAtStartup(Location profileBackup) {
    distribution.restoreProfileAtStartup(profileBackup);
    return this;
  }

  public OrchestratorBuilder activateLicense(String pluginKey) {
    distribution.activateLicense(pluginKey);
    return this;
  }

  /**
   * Set the root context for the webapp (default to /sonar)
   * @since 2.8
   */
  public OrchestratorBuilder setContext(String context) {
    distribution.setContext(context);
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

  /**
   * Define main plugin key.
   * @see OrchestratorBuilder#getSonarVersion()
   * @since 2.13
   */
  public OrchestratorBuilder setMainPluginKey(String pluginKey) {
    this.mainPluginKey = pluginKey;
    return this;
  }

  public Orchestrator build() {
    String version = getSonarVersion();

    Configuration.Builder configBuilder = Configuration.builder();
    Configuration finalConfig = configBuilder
      .addConfiguration(config)
      .addMap(overriddenProperties)
      .setUpdateCenter(getUpdateCenter())
      .build();

    Preconditions.checkState(!Strings.isNullOrEmpty(version), "Missing Sonar version");

    this.distribution.setVersion(Version.create(version));

    if (this.distribution.version().isGreaterThanOrEquals("5.0")) {
      this.distribution.addPluginLocation(ResourceLocation.create("/com/sonar/orchestrator/sonar-reset-data-plugin-1.0-SNAPSHOT.jar"));
    }
    return new Orchestrator(finalConfig, distribution);
  }
}
