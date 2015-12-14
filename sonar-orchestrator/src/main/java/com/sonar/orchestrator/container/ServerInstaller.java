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
package com.sonar.orchestrator.container;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.config.Licenses;
import com.sonar.orchestrator.db.DatabaseClient;
import com.sonar.orchestrator.locator.Location;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServerInstaller {
  private static final Logger LOG = LoggerFactory.getLogger(ServerInstaller.class);

  private final FileSystem fileSystem;
  private final DatabaseClient databaseClient;
  private final Licenses licenses;
  private final SonarDownloader downloader;

  public ServerInstaller(FileSystem fileSystem, DatabaseClient databaseClient, Licenses licenses, Configuration configuration) {
    this.fileSystem = fileSystem;
    this.databaseClient = databaseClient;
    this.licenses = licenses;
    this.downloader = new SonarDownloader(fileSystem, configuration);
  }

  public Server install(SonarDistribution distribution) {
    File sonarHome = downloader.downloadAndUnzip(distribution);

    configureHome(distribution, sonarHome);

    return new Server(fileSystem, sonarHome, distribution);
  }

  void configureHome(SonarDistribution distribution, File sonarHome) {
    copyPlugins(distribution, sonarHome);
    copyJdbcDriver(sonarHome);
    configure(distribution, sonarHome);
  }

  private void copyJdbcDriver(File sonarHome) {
    if (databaseClient.getDriverFile() != null) {
      try {
        File driverDir = new File(sonarHome, "extensions/jdbc-driver/" + databaseClient.getDialect());
        FileUtils.forceMkdir(driverDir);
        FileUtils.copyFileToDirectory(databaseClient.getDriverFile(), driverDir);
      } catch (IOException e) {
        throw new IllegalStateException("Fail to copy JDBC driver", e);
      }
    }
  }

  private void copyPlugins(SonarDistribution distribution, File sonarHome) {
    File downloadDir = new File(sonarHome, "extensions/downloads");
    try {
      FileUtils.forceMkdir(downloadDir);
      if (distribution.removeDistributedPlugins()) {
        LOG.info("Remove distribution plugins");
        File dirToClean = new File(sonarHome, "lib/bundled-plugins");
        if (dirToClean.exists()) {
          FileUtils.cleanDirectory(dirToClean);
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to clean the download directory: " + downloadDir, e);
    }
    for (Location plugin : distribution.getPluginLocations()) {
      File pluginFile = fileSystem.copyToDirectory(plugin, downloadDir);
      if (pluginFile == null || !pluginFile.exists()) {
        throw new IllegalStateException("Can not find the plugin " + plugin);
      }
      LOG.info("Installed plugin: {}", pluginFile.getName());
    }
  }

  private void configure(SonarDistribution distribution, File sonarHome) {
    File propertiesFile = new File(sonarHome, "conf/sonar.properties");
    LOG.info("Configuring " + propertiesFile);
    configureProperties(distribution, propertiesFile);
  }

  void configureProperties(SonarDistribution distribution, File propertiesFile) {
    OutputStream output = null;
    try {
      Properties properties = new Properties();
      properties.putAll(distribution.getServerProperties());
      properties.setProperty("sonar.web.port", Integer.toString(distribution.getPort()));
      properties.setProperty("sonar.web.context", "" + distribution.getContext());
      properties.setProperty("sonar.jdbc.url", databaseClient.getUrl());
      properties.setProperty("sonar.jdbc.username", databaseClient.getLogin());
      properties.setProperty("sonar.jdbc.password", databaseClient.getPassword());
      // Use same host as NetworkUtils.getNextAvailablePort()
      properties.setProperty("sonar.web.host", "localhost");
      properties.setProperty("sonar.search.host", "localhost");
      properties.setProperty("sonar.log.console", "true");

      properties.putAll(databaseClient.getAdditionalProperties());

      // Temporary hack to not restart server with License 2.0.
      // Will be dropped with License 2.1.
      for (String pluginKey : distribution.getLicensedPluginKeys()) {
        String license = licenses.get(pluginKey);
        if (license != null) {
          properties.setProperty(licenses.licensePropertyKey(pluginKey), license);
        }
      }

      output = FileUtils.openOutputStream(propertiesFile);
      properties.store(output, "Generated by Orchestrator");
    } catch (IOException e) {
      throw new IllegalStateException("Fail to configure conf/sonar.properties", e);
    } finally {
      IOUtils.closeQuietly(output);
    }
  }
}
