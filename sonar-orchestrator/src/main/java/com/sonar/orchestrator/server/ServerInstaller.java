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
package com.sonar.orchestrator.server;

import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.container.SonarDistribution;
import com.sonar.orchestrator.db.DatabaseClient;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.util.ZipUtils;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class ServerInstaller {

  private static final Logger LOG = LoggerFactory.getLogger(ServerInstaller.class);

  private static final AtomicInteger sharedDirId = new AtomicInteger(0);
  private final ServerZipFinder zipFinder;
  private final FileSystem fs;
  private final DatabaseClient databaseClient;

  public ServerInstaller(ServerZipFinder zipFinder, FileSystem fs, DatabaseClient databaseClient) {
    this.zipFinder = zipFinder;
    this.fs = fs;
    this.databaseClient = databaseClient;
  }

  public Server install(SonarDistribution distrib) {
    File home = locateAndUnzip(distrib);
    configureHome(distrib, home);
    return new Server(fs, home, distrib);
  }

  private File locateAndUnzip(SonarDistribution distrib) {
    File zip = zipFinder.find(distrib);
    File toDir = new File(fs.workspace(), String.valueOf(sharedDirId.addAndGet(1)));
    try {
      FileUtils.deleteDirectory(toDir);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to delete directory " + toDir, e);
    }
    ZipUtils.unzip(zip, toDir);
    File[] roots = toDir.listFiles((FileFilter) FileFilterUtils.directoryFileFilter());
    if (roots.length != 1) {
      throw new IllegalStateException("ZIP is badly structured. Missing root directory in " + toDir);
    }
    return roots[0];
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
        throw new IllegalStateException(format("Fail to copy JDBC driver [%s]", databaseClient.getDriverFile()), e);
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
      File pluginFile = fs.copyToDirectory(plugin, downloadDir);
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
    try (OutputStream output = FileUtils.openOutputStream(propertiesFile)) {
      Properties properties = new Properties();
      properties.putAll(distribution.getServerProperties());
      properties.setProperty("sonar.web.port", Integer.toString(distribution.getPort()));
      properties.setProperty("sonar.web.context", "" + distribution.getContext());
      properties.setProperty("sonar.jdbc.url", databaseClient.getUrl());
      properties.setProperty("sonar.jdbc.username", databaseClient.getLogin());
      properties.setProperty("sonar.jdbc.password", databaseClient.getPassword());
      // Use same host as NetworkUtils.getNextAvailablePort()
      properties.setProperty("sonar.web.host", "localhost");
      properties.setProperty("sonar.search.port", "0");
      properties.setProperty("sonar.search.host", "localhost");
      properties.setProperty("sonar.log.console", "true");

      properties.putAll(databaseClient.getAdditionalProperties());

      properties.store(output, "Generated by Orchestrator");
    } catch (IOException e) {
      throw new IllegalStateException(format("Fail to configure [%s]", propertiesFile), e);
    }
  }
}
