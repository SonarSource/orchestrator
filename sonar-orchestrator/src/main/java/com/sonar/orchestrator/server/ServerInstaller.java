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
package com.sonar.orchestrator.server;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.container.SonarDistribution;
import com.sonar.orchestrator.db.DatabaseClient;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.util.NetworkUtils;
import com.sonar.orchestrator.util.OrchestratorUtils;
import com.sonar.orchestrator.util.ZipUtils;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import okhttp3.HttpUrl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonar.orchestrator.util.NetworkUtils.getNextAvailablePort;
import static com.sonar.orchestrator.util.OrchestratorUtils.isEmpty;
import static java.lang.String.format;

public class ServerInstaller {

  private static final Logger LOG = LoggerFactory.getLogger(ServerInstaller.class);
  private static final AtomicInteger sharedDirId = new AtomicInteger(0);
  private static final String WEB_HOST_PROPERTY = "sonar.web.host";
  private static final String WEB_PORT_PROPERTY = "sonar.web.port";
  private static final String WEB_CONTEXT_PROPERTY = "sonar.web.context";
  private static final String ALL_IPS_HOST = "0.0.0.0";

  private final PackagingResolver packagingResolver;
  private final Configuration configuration;
  private final Locators locators;
  private final DatabaseClient databaseClient;

  public ServerInstaller(PackagingResolver packagingResolver, Configuration configuration, Locators locators, DatabaseClient databaseClient) {
    this.packagingResolver = packagingResolver;
    this.configuration = configuration;
    this.locators = locators;
    this.databaseClient = databaseClient;
  }

  public Server install(SonarDistribution distrib) {
    Packaging packaging = packagingResolver.resolve(distrib);

    File homeDir = unzip(packaging);
    if (!distrib.isKeepBundledPlugins()) {
      removeBundledPlugins(homeDir);
    }
    copyPlugins(packaging, distrib.getPluginLocations(), homeDir);
    copyJdbcDriver(homeDir);
    Properties properties = configureProperties(distrib);
    writePropertiesFile(properties, homeDir);
    String host = properties.getProperty(WEB_HOST_PROPERTY);
    // ORCH-422 Like SQ, if host is 0.0.0.0, simply return localhost as URL
    String url = format("http://%s:%s%s", ALL_IPS_HOST.equals(host) ? "localhost" : host, properties.getProperty(WEB_PORT_PROPERTY), properties.getProperty(WEB_CONTEXT_PROPERTY));
    return new Server(locators, homeDir, packaging.getEdition(), packaging.getVersion(), HttpUrl.parse(url));
  }

  private File unzip(Packaging packaging) {
    File toDir = new File(configuration.fileSystem().workspace(), String.valueOf(sharedDirId.addAndGet(1)));
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

  private static void removeBundledPlugins(File homeDir) {
    LOG.info("Remove bundled plugins");
    cleanDirectory(new File(homeDir, "lib/bundled-plugins"));
    // plugins are bundled in extensions/plugins since version 7.2
    cleanDirectory(new File(homeDir, "extensions/plugins"));
  }

  private static void cleanDirectory(File dir) {
    try {
      if (dir.exists()) {
        FileUtils.cleanDirectory(dir);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Fail to clean directory: " + dir, e);
    }
  }

  private void copyPlugins(Packaging packaging, List<Location> plugins, File homeDir) {
    File downloadDir = new File(homeDir, "extensions/downloads");
    try {
      FileUtils.forceMkdir(downloadDir);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create directory: " + downloadDir, e);
    }

    for (Location plugin : plugins) {
      installPluginIntoDir(plugin, downloadDir);
    }

    Version sqVersion = packaging.getVersion();
    if (packaging.getEdition() != Edition.COMMUNITY && !sqVersion.isGreaterThanOrEquals(7, 2)) {
      boolean hasLicensePlugin = plugins.stream()
        .filter(p -> p instanceof MavenLocation)
        .map(p -> (MavenLocation) p)
        .anyMatch(p -> p.getArtifactId().equals("sonar-license-plugin") || p.getArtifactId().equals("sonar-dev-license-plugin"));
      if (!hasLicensePlugin) {
        String licenseVersion = "LATEST_RELEASE[3.3]";
        if (sqVersion.getMajor() == 6 && sqVersion.getMinor() == 7 && sqVersion.getPatch() >= 5) {
          licenseVersion = "LATEST_RELEASE[3]";
        }
        installPluginIntoDir(MavenLocation.of("com.sonarsource.license", "sonar-dev-license-plugin", licenseVersion), downloadDir);
      }
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

    InetAddress loopbackHost = InetAddress.getLoopbackAddress();
    setIfNotPresent(properties, "sonar.jdbc.url", databaseClient.getUrl());
    setIfNotPresent(properties, "sonar.jdbc.username", databaseClient.getLogin());
    setIfNotPresent(properties, "sonar.jdbc.password", databaseClient.getPassword());
    properties.putAll(databaseClient.getAdditionalProperties());
    setIfNotPresent(properties, "sonar.log.console", "true");
    setIfNotPresent(properties, "sonar.search.host", loopbackHost.getHostAddress());
    setIfNotPresent(properties, "sonar.search.port", "0");
    InetAddress webHost = loadWebHost(properties, loopbackHost);
    properties.setProperty(WEB_HOST_PROPERTY, webHost.getHostAddress());
    properties.setProperty(WEB_PORT_PROPERTY, Integer.toString(loadWebPort(properties, webHost)));
    setIfNotPresent(properties, WEB_CONTEXT_PROPERTY, "");
    completeJavaOptions(properties, "sonar.ce.javaAdditionalOpts");
    completeJavaOptions(properties, "sonar.search.javaAdditionalOpts");
    completeJavaOptions(properties, "sonar.web.javaAdditionalOpts");

    return properties;
  }

  private static InetAddress loadWebHost(Properties serverProperties, InetAddress loopbackAddress) {
    String value = serverProperties.getProperty(WEB_HOST_PROPERTY);
    if (!isEmpty(value)) {
      return NetworkUtils.getInetAddressByName(value);
    }
    return loopbackAddress;
  }

  private int loadWebPort(Properties properties, InetAddress webHost) {
    int webPort = Integer.parseInt(Stream.of(properties.getProperty(WEB_PORT_PROPERTY), configuration.getString("orchestrator.container.port"))
      .filter(s -> !isEmpty(s))
      .findFirst()
      .orElse("0"));
    if (webPort == 0) {
      webPort = getNextAvailablePort(webHost);
    }
    return webPort;
  }

  private static void completeJavaOptions(Properties properties, String propertyKey) {
    String javaOpts = OrchestratorUtils.defaultIfEmpty(properties.getProperty(propertyKey), "");
    if (!javaOpts.contains("-Djava.net.preferIPv4Stack")) {
      javaOpts += " -Djava.net.preferIPv4Stack=true";
    }
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

  private static void writePropertiesFile(Properties properties, File sonarHome) {
    File propertiesFile = new File(sonarHome, "conf/sonar.properties");
    try (OutputStream output = FileUtils.openOutputStream(propertiesFile)) {
      properties.store(output, "Generated by Orchestrator");
    } catch (IOException e) {
      throw new IllegalStateException(format("Fail to write [%s]", propertiesFile), e);
    }
  }
}
