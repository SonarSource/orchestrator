/*
 * Orchestrator
 * Copyright (C) 2011 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.sonar.orchestrator.container;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.config.Licenses;
import com.sonar.orchestrator.db.H2;
import com.sonar.orchestrator.db.MySql;
import com.sonar.orchestrator.db.Oracle;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerInstallerTest {
  @Test
  public void shouldConfigureDatabaseSettings() throws IOException {
    Configuration config = Configuration.create();
    FileSystem fileSystem = new FileSystem(config);
    File propertiesFile = new File("target/tmp-test/ServerInstallerTest/shouldConfigureDatabaseSettings.properties");
    FileUtils.forceMkdir(propertiesFile.getParentFile());

    ServerInstaller installer = new ServerInstaller(fileSystem, MySql.builder()
      .setUrl("jdbc:mysql://localhost:1234/sonar")
      .build(), mock(Licenses.class), config);
    installer.configureProperties(new SonarDistribution(Version.create("3.0")), propertiesFile);

    Properties after = new Properties();
    InputStream fileInput = new FileInputStream(propertiesFile);
    after.load(fileInput);
    fileInput.close();

    assertThat(after.getProperty("sonar.jdbc.url")).isEqualTo("jdbc:mysql://localhost:1234/sonar");// set
    assertThat(after.getProperty("sonar.jdbc.validationQuery")).isNull();// not supported (yet) in Database component.
  }

  @Test
  public void shouldConfigureLicenses() throws IOException {
    Configuration config = Configuration.create();
    FileSystem fileSystem = new FileSystem(config);
    Properties before = new Properties();
    File propertiesFile = new File("target/tmp-test/ServerInstallerTest/shouldConfigureLicenses.properties");
    FileUtils.forceMkdir(propertiesFile.getParentFile());
    OutputStream fileOutput = new FileOutputStream(propertiesFile);
    before.store(fileOutput, "For unit tests");
    fileOutput.close();

    Licenses licenses = mock(Licenses.class);
    when(licenses.get("abap")).thenReturn("ABCD1234");
    when(licenses.licensePropertyKey("abap")).thenReturn("sonar.abap.license.secured");

    ServerInstaller installer = new ServerInstaller(fileSystem, H2.create(), licenses, config);
    SonarDistribution distribution = new SonarDistribution(Version.create("3.0"));
    distribution.activateLicense("abap");
    installer.configureProperties(distribution, propertiesFile);

    // The properties file should be updated with ABAP license

    Properties after = new Properties();
    InputStream fileInput = new FileInputStream(propertiesFile);
    after.load(fileInput);
    fileInput.close();

    assertThat(after.getProperty("sonar.abap.license.secured")).isEqualTo("ABCD1234");
  }

  @Test
  public void shouldDefineServerProperties() throws IOException {
    Configuration config = Configuration.create();
    FileSystem fileSystem = new FileSystem(config);
    ServerInstaller installer = new ServerInstaller(fileSystem, H2.create(), mock(Licenses.class), config);

    File propertiesFile = new File("target/tmp-test/ServerInstallerTest/shouldDefineServerProperties.properties");
    FileUtils.forceMkdir(propertiesFile.getParentFile());

    SonarDistribution distribution = new SonarDistribution(Version.create("3.0"));
    distribution.setServerProperty("foo", "bar");
    installer.configureProperties(distribution, propertiesFile);

    // The properties file must contain server properties.

    Properties after = new Properties();
    InputStream fileInput = new FileInputStream(propertiesFile);
    after.load(fileInput);
    fileInput.close();

    assertThat(after.getProperty("foo")).isEqualTo("bar");// not changed
    assertThat(after.getProperty("sonar.jdbc.url")).startsWith("jdbc:h2:");// set
  }

  @Test
  public void shouldCopyPlugins() throws IOException {
    Configuration config = Configuration.create();
    FileSystem fileSystem = new FileSystem(config);
    ServerInstaller installer = new ServerInstaller(fileSystem, H2.create(), mock(Licenses.class), config);
    SonarDistribution distribution = new SonarDistribution(Version.create("3.0"))
      .addPluginLocation(getFileLocation("/com/sonar/orchestrator/container/ServerInstallerTest/plugin1.jar"))
      .addPluginLocation(getFileLocation("/com/sonar/orchestrator/container/ServerInstallerTest/plugin2.jar"));

    File home = new File("target/tmp-test/ServerInstallerTest/shouldCopyPlugins");
    FileUtils.forceMkdir(home);
    FileUtils.cleanDirectory(home);

    installer.configureHome(distribution, home);

    assertThat(FileUtils.listFiles(new File(home, "extensions/downloads"), new String[] {"jar"}, false).size()).isEqualTo(2);
    assertThat(new File(home, "extensions/downloads/plugin1.jar")).exists();
    assertThat(new File(home, "extensions/downloads/plugin2.jar")).exists();
  }

  @Test
  public void shouldRemoveDistributedPlugins() throws IOException {
    File home = new File("target/tmp-test/ServerInstallerTest/shouldRemoveDistributedPlugins");
    FileUtils.forceMkdir(home);
    FileUtils.cleanDirectory(home);

    File bundledDir = new File(home, "lib/bundled-plugins");
    FileUtils.forceMkdir(bundledDir);

    File distributedPlugin = new File(bundledDir, "plugin.jar");
    distributedPlugin.createNewFile();

    Configuration config = Configuration.create();
    FileSystem fileSystem = new FileSystem(config);
    ServerInstaller installer = new ServerInstaller(fileSystem, H2.create(), mock(Licenses.class), config);
    SonarDistribution distribution = new SonarDistribution(Version.create("5.0"))
      .addPluginLocation(getFileLocation("/com/sonar/orchestrator/container/ServerInstallerTest/plugin1.jar"))
      .addPluginLocation(getFileLocation("/com/sonar/orchestrator/container/ServerInstallerTest/plugin2.jar"));

    installer.configureHome(distribution, home);

    assertThat(FileUtils.listFiles(new File(home, "extensions/downloads"), new String[] {"jar"}, false).size()).isEqualTo(2);
    assertThat(FileUtils.listFiles(new File(home, "lib/bundled-plugins"), new String[] {"jar"}, false).size()).isEqualTo(0);
    assertThat(new File(home, "extensions/downloads/plugin1.jar")).exists();
    assertThat(new File(home, "extensions/downloads/plugin2.jar")).exists();
    assertThat(new File(home, "lib/bundled-plugins/plugin.jar")).doesNotExist();

    distributedPlugin.createNewFile();

    distribution.setRemoveDistributedPlugins(false);
    installer.configureHome(distribution, home);

    assertThat(FileUtils.listFiles(new File(home, "extensions/downloads"), new String[] {"jar"}, false).size()).isEqualTo(2);
    assertThat(FileUtils.listFiles(new File(home, "lib/bundled-plugins"), new String[] {"jar"}, false).size()).isEqualTo(1);
    assertThat(new File(home, "extensions/downloads/plugin1.jar")).exists();
    assertThat(new File(home, "extensions/downloads/plugin2.jar")).exists();
    assertThat(new File(home, "lib/bundled-plugins/plugin.jar")).exists();
  }

  @Test
  public void shouldCopyJdbcDriver() throws IOException {
    Configuration config = Configuration.create();
    FileSystem fileSystem = new FileSystem(config);
    Oracle oracle = Oracle.builder().setDriverFile(getFile("/com/sonar/orchestrator/container/ServerInstallerTest/driver.jar")).build();
    ServerInstaller installer = new ServerInstaller(fileSystem, oracle, mock(Licenses.class), config);
    SonarDistribution distribution = new SonarDistribution(Version.create("2.8"));

    File home = new File("target/tmp-test/ServerInstallerTest/shouldCopyJdbcDriver");
    FileUtils.forceMkdir(home);
    FileUtils.cleanDirectory(home);

    installer.configureHome(distribution, home);

    assertThat(FileUtils.listFiles(new File(home, "extensions/jdbc-driver/oracle"), new String[] {"jar"}, false).size()).isEqualTo(1);
    assertThat(new File(home, "extensions/jdbc-driver/oracle/driver.jar").exists()).isEqualTo(true);
  }

  @Test
  public void shouldConfigureLogging() throws IOException {
    Configuration config = Configuration.create();
    FileSystem fileSystem = new FileSystem(config);
    ServerInstaller installer = new ServerInstaller(fileSystem, H2.create(), mock(Licenses.class), config);
    SonarDistribution distribution = new SonarDistribution(Version.create("4.5"));

    File home = new File("target/tmp-test/ServerInstallerTest/shouldConfigureLogbackAfter41");
    FileUtils.forceMkdir(home);
    FileUtils.cleanDirectory(home);

    installer.configureHome(distribution, home);

    assertThat(new File(home, "conf/logback.xml")).doesNotExist();

    Properties after = new Properties();
    InputStream fileInput = new FileInputStream(new File(home, "conf/sonar.properties"));
    after.load(fileInput);
    fileInput.close();

    assertThat(after.getProperty("sonar.log.console")).isEqualTo("true");
  }

  private File getFile(String filename) {
    return FileUtils.toFile(getClass().getResource(filename));
  }

  private FileLocation getFileLocation(String filename) {
    return FileLocation.of(getFile(filename));
  }
}
