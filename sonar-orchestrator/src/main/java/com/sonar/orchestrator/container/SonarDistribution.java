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
package com.sonar.orchestrator.container;

import com.sonar.orchestrator.locator.Location;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public final class SonarDistribution {

  private String version;
  private Edition edition = Edition.COMMUNITY;
  private final List<Location> pluginLocations = new ArrayList<>();
  private final List<Location> bundledPluginLocations = new ArrayList<>();
  private final List<Location> profileBackups = new ArrayList<>();
  private final Properties serverProperties = new Properties();
  private boolean activateLicense;
  private boolean emptySonarProperties = false;
  private boolean keepBundledPlugins = false;

  private Set<String> bundledPluginNamePrefixesToKeep = new HashSet<>();
  private boolean defaultForceAuthentication = false;
  private boolean forceDefaultAdminCredentialsRedirect = false;
  private boolean useDefaultAdminCredentialsForBuilds = false;
  private Location zip;

  public SonarDistribution() {
    // A distribution without a version yet
  }

  public Optional<Location> getZipLocation() {
    return Optional.ofNullable(zip);
  }

  public SonarDistribution setZipLocation(@Nullable Location zip) {
    this.zip = zip;
    return this;
  }

  public SonarDistribution setVersion(@Nullable String s) {
    this.version = s;
    return this;
  }

  public SonarDistribution addPluginLocation(Location plugin) {
    pluginLocations.add(plugin);
    return this;
  }

  public SonarDistribution addBundledPluginLocation(Location plugin) {
    bundledPluginLocations.add(plugin);
    return this;
  }

  public boolean isEmptySonarProperties() {
    return emptySonarProperties;
  }

  public SonarDistribution setEmptySonarProperties(boolean emptySonarProperties) {
    this.emptySonarProperties = emptySonarProperties;
    return this;
  }

  public boolean isDefaultForceAuthentication() {
    return defaultForceAuthentication;
  }

  public SonarDistribution setDefaultForceAuthentication(boolean defaultForceAuthentication) {
    this.defaultForceAuthentication = defaultForceAuthentication;
    return this;
  }

  public boolean isForceDefaultAdminCredentialsRedirect() {
    return forceDefaultAdminCredentialsRedirect;
  }

  public SonarDistribution setForceDefaultAdminCredentialsRedirect(boolean forceDefaultAdminCredentialsRedirect) {
    this.forceDefaultAdminCredentialsRedirect = forceDefaultAdminCredentialsRedirect;
    return this;
  }

  public boolean useDefaultAdminCredentialsForBuilds() {
    return this.useDefaultAdminCredentialsForBuilds;
  }

  public SonarDistribution useDefaultAdminCredentialsForBuilds(boolean useDefaultAdminCredentialsForBuilds) {
    this.useDefaultAdminCredentialsForBuilds = useDefaultAdminCredentialsForBuilds;
    return this;
  }

  /**
   * Version of SonarQube as defined by {@link com.sonar.orchestrator.OrchestratorBuilder}. When using local zip (see
   * {@link com.sonar.orchestrator.OrchestratorBuilder#setZipFile(File)}, then returned version is {@code null}.
   */
  public Optional<String> getVersion() {
    return Optional.ofNullable(version);
  }

  public List<Location> getPluginLocations() {
    return Collections.unmodifiableList(pluginLocations);
  }

  public List<Location> getBundledPluginLocations() {
    return Collections.unmodifiableList(bundledPluginLocations);
  }


  public SonarDistribution restoreProfileAtStartup(Location backup) {
    requireNonNull(backup);
    this.profileBackups.add(backup);
    return this;
  }

  public List<Location> getProfileBackups() {
    return profileBackups;
  }

  public Properties getServerProperties() {
    return serverProperties;
  }

  public String getServerProperty(String key) {
    return serverProperties.getProperty(key);
  }

  public SonarDistribution setServerProperty(String key, @Nullable String value) {
    if (value == null) {
      serverProperties.remove(key);
    } else {
      serverProperties.setProperty(key, value);
    }
    return this;
  }

  public SonarDistribution removeServerProperty(String key) {
    serverProperties.remove(key);
    return this;
  }

  public SonarDistribution addServerProperties(Properties props) {
    serverProperties.putAll(props);
    return this;
  }

  public SonarDistribution activateLicense() {
    activateLicense = true;
    return this;
  }

  public SonarDistribution setEdition(Edition edition) {
    this.edition = edition;
    return this;
  }

  public Edition getEdition() {
    return edition;
  }

  public boolean isActivateLicense() {
    return activateLicense;
  }

  public boolean isKeepBundledPlugins() {
    return keepBundledPlugins;
  }

  public SonarDistribution setKeepBundledPlugins(boolean b) {
    this.keepBundledPlugins = b;
    return this;
  }

  public SonarDistribution addBundledPluginToKeep(String pluginJarNamePrefix) {
    this.bundledPluginNamePrefixesToKeep.add(pluginJarNamePrefix);
    return this;
  }

  public Set<String> getBundledPluginNamePrefixesToKeep() {
    return this.bundledPluginNamePrefixesToKeep;
  }
}
