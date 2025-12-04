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
package com.sonar.orchestrator.server;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.container.SonarDistribution;
import com.sonar.orchestrator.db.DatabaseClient;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.util.ZipUtils;
import com.sonar.orchestrator.util.NetworkUtils;
import com.sonar.orchestrator.util.OrchestratorUtils;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import okhttp3.HttpUrl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonar.orchestrator.util.NetworkUtils.getNextAvailablePort;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static java.lang.String.format;
import static java.lang.String.valueOf;

public class ServerInstaller {

  private static final Logger LOG = LoggerFactory.getLogger(ServerInstaller.class);
  private static final AtomicInteger sharedDirId = new AtomicInteger(0);

  private static final String WEB_HOST_PROPERTY = "sonar.web.host";
  private static final String WEB_PORT_PROPERTY = "sonar.web.port";
  private static final String WEB_CONTEXT_PROPERTY = "sonar.web.context";

  private static final String SEARCH_HOST_PROPERTY = "sonar.search.host";
  private static final String SEARCH_HTTP_PORT_PROPERTY = "sonar.search.port";
  private static final String SEARCH_TCP_PORT_PROPERTY = "sonar.es.port";
  private static final String CLUSTER_ENABLED_PROPERTY = "sonar.cluster.enabled";
  private static final String CLUSTER_NODE_TYPE_PROPERTY = "sonar.cluster.node.type";
  private static final String CLUSTER_NODE_SEARCH_HOST_PROPERTY = "sonar.cluster.node.search.host";
  private static final String CLUSTER_NODE_SEARCH_PORT_PROPERTY = "sonar.cluster.node.search.port";
  private static final String CLUSTER_NODE_ES_HOST_PROPERTY = "sonar.cluster.node.es.host";
  private static final String CLUSTER_NODE_ES_PORT_PROPERTY = "sonar.cluster.node.es.port";

  private static final String SONAR_CLUSTER_NODE_NAME = "sonar.cluster.node.name";
  private static final String ALL_IPS_HOST = "0.0.0.0";

  private final PackagingResolver packagingResolver;
  private final Configuration configuration;
  private final Locators locators;
  private final DatabaseClient databaseClient;

  public ServerInstaller(PackagingResolver packagingResolver, Configuration configuration, Locators locators,
    DatabaseClient databaseClient) {
    this.packagingResolver = packagingResolver;
    this.configuration = configuration;
    this.locators = locators;
    this.databaseClient = databaseClient;
  }

  public Server install(SonarDistribution distrib) {
    Packaging packaging = packagingResolver.resolve(distrib);

    File homeDir = unzip(packaging);
    preparePlugins(distrib, homeDir);
    copyJdbcDriver(homeDir);
    Properties properties = configureProperties(distrib);
    writePropertiesFile(properties, homeDir);
    String host = properties.getProperty(WEB_HOST_PROPERTY, "localhost");
    // ORCH-422 Like SQ, if host is 0.0.0.0, simply return localhost as URL
    String resolvedHost = ALL_IPS_HOST.equals(host) ? "localhost" : host;
    String url = format("http://%s:%s%s", resolvedHost, properties.getProperty(WEB_PORT_PROPERTY, "9000"),
      properties.getProperty(WEB_CONTEXT_PROPERTY, ""));
    return new Server(locators, homeDir, packaging.getEdition(), packaging.getVersion(), HttpUrl.parse(url),
      getSearchPort(properties),
      (String) properties.get(SONAR_CLUSTER_NODE_NAME));
  }

  private void preparePlugins(SonarDistribution distrib, File homeDir) {
    if (!distrib.isKeepBundledPlugins()) {
      removeBundledPlugins(homeDir, distrib.getBundledPluginNamePrefixesToKeep());
    }
    copyBundledPlugins(distrib.getBundledPluginLocations(), homeDir);
    copyExternalPlugins(distrib.getPluginLocations(), homeDir);
  }

  private File unzip(Packaging packaging) {
    File toDir = new File(configuration.fileSystem().workspace().toFile(), valueOf(sharedDirId.addAndGet(1)));
    try {
      FileUtils.deleteDirectory(toDir);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to delete directory " + toDir, e);
    }
    ZipUtils.unzip(packaging.getZip(), toDir);
    File[] roots = toDir.listFiles((FileFilter) FileFilterUtils.directoryFileFilter());
    if (roots == null || roots.length != 1) {
      throw new IllegalStateException("ZIP is badly structured. Missing root directory in " + toDir);
    }
    return roots[0];
  }

  private void copyJdbcDriver(File sonarHome) {
    if (databaseClient.getDriverFile() != null) {
      try {
        File driverDir = new File(sonarHome, "extensions/jdbc-driver/" + databaseClient.getDialect());
        FileUtils.forceMkdir(driverDir);
        FileUtils.copyFileToDirectory(databaseClient.getDriverFile(), driverDir);
      } catch (IOException e) {
        throw new IllegalStateException(format("Fail to copy JDBC driver [%s]", databaseClient.getDriverFile()), e);
      }
    }
  }

  private static void removeBundledPlugins(File homeDir, Collection<String> bundledPluginNamePrefixesToKeep) {
    if (bundledPluginNamePrefixesToKeep.isEmpty()) {
      LOG.info("Remove bundled plugins");
    } else {
      LOG.info("Remove bundled plugins except: " + String.join(", ", bundledPluginNamePrefixesToKeep));

    }
    cleanDirectory(new File(homeDir, "lib/bundled-plugins"), bundledPluginNamePrefixesToKeep);
    // plugins are bundled in extensions/plugins since version 7.2
    cleanDirectory(new File(homeDir, "extensions/plugins"), bundledPluginNamePrefixesToKeep);
    // SonarSource plugins are bundled in lib/extension since version 8.5
    cleanDirectory(new File(homeDir, "lib/extensions"), bundledPluginNamePrefixesToKeep);
  }

  private static void cleanDirectory(File dir, Collection<String> bundledPluginNamePrefixesToKeep) {
    try {
      if (!dir.exists()) {
        return;
      }
      try (Stream<Path> files = Files.list(dir.toPath())) {
        files.forEach(file -> {
          if (shouldDeletePlugin(file, bundledPluginNamePrefixesToKeep)) {
            try {
              Files.delete(file);
            } catch (IOException e) {
              throw new IllegalStateException("Fail to delete file: " + file, e);
            }
          } else {
            LOG.info("  Keeping: " + file.getFileName().toString());
          }
        });
      }
    } catch (IOException e) {
      throw new IllegalStateException("Fail to clean directory: " + dir, e);
    }
  }

  private static boolean shouldDeletePlugin(Path pluginFile, Collection<String> bundledPluginNamePrefixesToKeep) {
    return bundledPluginNamePrefixesToKeep
      .stream()
      .noneMatch(prefix -> pluginFile.getFileName().toString().startsWith(prefix));
  }

  private void copyExternalPlugins(List<Location> plugins, File homeDir) {
    File toDir = new File(homeDir, "extensions/downloads");
    copyPlugins(plugins, toDir);
  }

  private void copyBundledPlugins(List<Location> plugins, File homeDir) {
    File toDir = new File(homeDir, "lib/extensions");
    copyPlugins(plugins, toDir);
  }

  private void copyPlugins(List<Location> plugins, File toDir) {
    try {
      FileUtils.forceMkdir(toDir);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create directory: " + toDir, e);
    }

    for (Location plugin : plugins) {
      installPluginIntoDir(plugin, toDir);
    }
  }

  private void installPluginIntoDir(Location plugin, File downloadDir) {
    File pluginFile = locators.copyToDirectory(plugin, downloadDir);
    if (pluginFile == null || !pluginFile.exists()) {
      throw new IllegalStateException("Can not find the plugin " + plugin);
    }
    LOG.info("Installed plugin: {}", pluginFile.getName());
  }

  private Properties configureProperties(SonarDistribution distribution) {
    Properties properties = new Properties();
    properties.putAll(distribution.getServerProperties());

    if (distribution.isEmptySonarProperties()) {
      return properties;
    }

    InetAddress loopbackHost = InetAddress.getLoopbackAddress();
    setIfNotPresent(properties, "sonar.jdbc.url", databaseClient.getUrl());
    setIfNotPresent(properties, "sonar.jdbc.username", databaseClient.getLogin());
    setIfNotPresent(properties, "sonar.jdbc.password", databaseClient.getPassword());
    properties.putAll(databaseClient.getAdditionalProperties());
    setIfNotPresent(properties, "sonar.log.console", "true");
    setAndFailIfForbiddenValuePresent(properties, "sonar.telemetry.url", "https://telemetry-staging.test-sonarsource.com/sonarqube", "https://telemetry.sonarsource.com/sonarqube");
    setAndFailIfForbiddenValuePresent(properties, "sonar.telemetry.metrics.url", "https://telemetry-staging.test-sonarsource.com/sonarqube/metrics",
      "https://telemetry.sonarsource.com/sonarqube/metrics");
    setAndFailIfForbiddenValuePresent(properties, "sonar.ai.suggestions.url", "", "https://api.sonarqube.io");
    InetAddress webHost = loadWebHost(properties, loopbackHost);
    configureSearchProperties(properties, loopbackHost);
    properties.setProperty(WEB_HOST_PROPERTY, webHost instanceof Inet6Address ? ("[" + webHost.getHostAddress() + "]") : webHost.getHostAddress());
    properties.setProperty(WEB_PORT_PROPERTY, Integer.toString(loadWebPort(properties, webHost)));
    setIfNotPresent(properties, WEB_CONTEXT_PROPERTY, "");
    completeJavaOptions(properties, "sonar.ce.javaAdditionalOpts");
    completeJavaOptions(properties, "sonar.search.javaAdditionalOpts");
    completeJavaOptions(properties, "sonar.web.javaAdditionalOpts");
    if (!distribution.isDefaultForceAuthentication()) {
      setIfNotPresent(properties, "sonar.forceAuthentication", "false");
    }
    if (!distribution.isForceDefaultAdminCredentialsRedirect()) {
      setIfNotPresent(properties, "sonar.forceRedirectOnDefaultAdminCredentials", "false");
    }

    return properties;
  }

  private static void configureSearchProperties(Properties properties, InetAddress loopbackHost) {
    if (isClusterEnabled(properties) && isSearchNode(properties)) {
      throwIfNotPresent(properties, CLUSTER_NODE_ES_HOST_PROPERTY, format("Cluster configuration must provide the property %s upfront", CLUSTER_NODE_ES_HOST_PROPERTY));
      throwIfNotPresent(properties, CLUSTER_NODE_SEARCH_HOST_PROPERTY, format("Cluster configuration must provide the property %s upfront", CLUSTER_NODE_SEARCH_HOST_PROPERTY));
      setPortPropertyIfNotPresent(properties, CLUSTER_NODE_ES_PORT_PROPERTY, loopbackHost);
      setPortPropertyIfNotPresent(properties, CLUSTER_NODE_SEARCH_PORT_PROPERTY, loopbackHost);
    } else {
      setIfNotPresent(properties, SEARCH_HOST_PROPERTY, loopbackHost.getHostAddress());
      setPortPropertyIfNotPresent(properties, SEARCH_HTTP_PORT_PROPERTY, loopbackHost);
      setPortPropertyIfNotPresent(properties, SEARCH_TCP_PORT_PROPERTY, loopbackHost);
    }
  }

  private static void throwIfNotPresent(Properties properties, String key, String errorMessage) {
    if (properties.getProperty(key) == null) {
      throw new IllegalStateException(errorMessage);
    }
  }

  private static void setPortPropertyIfNotPresent(Properties properties, String key, InetAddress loopbackHost) {
    if (properties.getProperty(key) == null) {
      properties.setProperty(key, valueOf(getNextAvailablePort(loopbackHost)));
    }
    LOG.info("Port allocated for {}: {}", key, properties.get(key));
  }

  private static InetAddress loadWebHost(Properties serverProperties, InetAddress loopbackAddress) {
    String value = serverProperties.getProperty(WEB_HOST_PROPERTY);
    if (!isEmpty(value)) {
      return NetworkUtils.getInetAddressByName(value);
    }
    return loopbackAddress;
  }

  private int loadWebPort(Properties properties, InetAddress webHost) {
    int webPort = Integer.parseInt(
      Stream.of(properties.getProperty(WEB_PORT_PROPERTY), configuration.getString("orchestrator.container.port"))
        .filter(s -> !isEmpty(s))
        .findFirst()
        .orElse("0"));
    if (webPort == 0) {
      webPort = getNextAvailablePort(webHost);
    }
    LOG.info("Port allocated for the WebServer: {}", webPort);
    return webPort;
  }

  private static int getSearchPort(Properties properties) {
    if (isClusterEnabled(properties) && isSearchNode(properties)) {
      return Integer.parseInt(properties.getProperty(CLUSTER_NODE_SEARCH_PORT_PROPERTY));
    } else if (properties.getProperty(SEARCH_HTTP_PORT_PROPERTY) != null) {
      return Integer.parseInt(properties.getProperty(SEARCH_HTTP_PORT_PROPERTY));
    } else {
      //SonarQube's default
      return 9001;
    }
  }

  private static boolean isClusterEnabled(Properties properties) {
    return Boolean.parseBoolean(properties.getProperty(CLUSTER_ENABLED_PROPERTY));
  }

  private static boolean isSearchNode(Properties properties) {
    String nodeType = properties.getProperty(CLUSTER_NODE_TYPE_PROPERTY);
    return "search".equals(nodeType);
  }

  private static void completeJavaOptions(Properties properties, String propertyKey) {
    String javaOpts = OrchestratorUtils.defaultIfEmpty(properties.getProperty(propertyKey), "");
    if (!javaOpts.contains("-Djava.security.egd") && SystemUtils.IS_OS_LINUX) {
      javaOpts += " -Djava.security.egd=file:/dev/./urandom";
    }
    properties.setProperty(propertyKey, javaOpts);
  }

  private static void setIfNotPresent(Properties properties, String key, String value) {
    String initialValue = properties.getProperty(key);
    if (initialValue == null) {
      properties.setProperty(key, value);
    }
  }

  private static void setAndFailIfForbiddenValuePresent(Properties properties, String key, String value, String forbiddenValue) {
    String initialValue = properties.getProperty(key);
    if (initialValue != null && initialValue.contains(forbiddenValue)) {
      throw new IllegalStateException(format("Property %s is forbidden to be set to %s", key, forbiddenValue));
    }
    if (initialValue == null) {
      properties.setProperty(key, value);
    }
  }

  private static void writePropertiesFile(Properties properties, File sonarHome) {
    File propertiesFile = new File(sonarHome, "conf/sonar.properties");
    try (OutputStream output = FileUtils.openOutputStream(propertiesFile)) {
      properties.store(output, "Generated by Orchestrator");
    } catch (IOException e) {
      throw new IllegalStateException(format("Fail to write [%s]", propertiesFile), e);
    }
  }
}
