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

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.config.Licenses;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.container.SonarDistribution;
import com.sonar.orchestrator.db.DatabaseClient;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerInstallerTest {

  private static final File ZIP_4_5_6 = FileUtils.toFile(ServerInstallerTest.class.getResource("ServerInstallerTest/sonarqube-4.5.6-lite.zip"));
  private static final Version VERSION_4_5_6 = Version.create("4.5.6");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  ServerZipFinder zipFinder = mock(ServerZipFinder.class);
  FileSystem fs = mock(FileSystem.class);
  DatabaseClient dbClient = mock(DatabaseClient.class);
  Licenses licenses = mock(Licenses.class);
  File installsDir;
  File workspaceDir;
  File mavenLocalDir;
  ServerInstaller underTest;

  @Before
  public void setUp() throws IOException {
    installsDir = temp.newFolder();
    workspaceDir = temp.newFolder();
    mavenLocalDir = temp.newFolder();
    Configuration config = Configuration.builder()
      .setProperty("orchestrator.sonarInstallsDir", installsDir.getAbsolutePath())
      .setProperty("orchestrator.workspaceDir", workspaceDir.getAbsolutePath())
      .setProperty("maven.localRepository", mavenLocalDir.getAbsolutePath())
      .build();
    fs = new FileSystem(config);
    when(dbClient.getUrl()).thenReturn("jdbc:h2:mem");
    when(dbClient.getLogin()).thenReturn("sonar");
    when(dbClient.getPassword()).thenReturn("sonar");
    underTest = new ServerInstaller(zipFinder, fs, dbClient, licenses);
  }

  @Test
  public void test_install() throws Exception {
    when(zipFinder.find(VERSION_4_5_6)).thenReturn(ZIP_4_5_6);

    Server server = underTest.install(new SonarDistribution(VERSION_4_5_6));
    assertThat(server.getDistribution().version()).isEqualTo(VERSION_4_5_6);
    // installed in a unique location. Home directory is the name defined in zip structure
    assertThat(server.getHome()).isEqualTo(new File(workspaceDir, "1/sonarqube-4.5.6"));
    Properties props = openPropertiesFile(server);
    assertThat(props.getProperty("sonar.jdbc.url")).isEqualTo("jdbc:h2:mem");
  }

  @Test
  public void installations_do_not_overlap() throws Exception {
    when(zipFinder.find(VERSION_4_5_6)).thenReturn(ZIP_4_5_6);

    Server server1 = underTest.install(new SonarDistribution(VERSION_4_5_6).setServerProperty("test.id", "1"));
    Server server2 = underTest.install(new SonarDistribution(VERSION_4_5_6).setServerProperty("test.id", "2"));
    assertThat(server1.getHome()).exists().isDirectory();
    assertThat(server2.getHome()).exists().isDirectory();
    assertThat(server1.getHome()).isNotEqualTo(server2.getHome());
    assertThat(openPropertiesFile(server1).getProperty("test.id")).isEqualTo("1");
    assertThat(openPropertiesFile(server2).getProperty("test.id")).isEqualTo("2");
  }

  @Test
  public void copy_jdbc_driver_if_defined() throws Exception {
    when(zipFinder.find(VERSION_4_5_6)).thenReturn(ZIP_4_5_6);
    when(dbClient.getDriverFile()).thenReturn(FileUtils.toFile(getClass().getResource("ServerInstallerTest/fake-oracle-driver.jar")));
    when(dbClient.getDialect()).thenReturn("oracle");

    Server server = underTest.install(new SonarDistribution(VERSION_4_5_6));
    assertThat(new File(server.getHome(), "extensions/jdbc-driver/oracle/fake-oracle-driver.jar")).exists().isFile();
  }

  @Test
  public void throw_ISE_if_fail_to_copy_jdbc_driver() throws Exception {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to copy JDBC driver");

    when(zipFinder.find(VERSION_4_5_6)).thenReturn(ZIP_4_5_6);
    File invalidDriver = temp.newFile();
    invalidDriver.delete();
    when(dbClient.getDriverFile()).thenReturn(invalidDriver);
    when(dbClient.getDialect()).thenReturn("oracle");

    underTest.install(new SonarDistribution(VERSION_4_5_6));
  }

  @Test
  public void copy_plugins() throws Exception {
    when(zipFinder.find(VERSION_4_5_6)).thenReturn(ZIP_4_5_6);
    SonarDistribution distrib = new SonarDistribution(VERSION_4_5_6);
    distrib.addPluginLocation(FileLocation.of(FileUtils.toFile(getClass().getResource("ServerInstallerTest/fake-plugin.jar"))));

    Server server = underTest.install(distrib);

    assertThat(new File(server.getHome(), "extensions/downloads/fake-plugin.jar")).exists().isFile();
  }

  @Test
  public void throw_ISE_if_fail_to_copy_plugins() throws Exception {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can not find the plugin");

    when(zipFinder.find(VERSION_4_5_6)).thenReturn(ZIP_4_5_6);
    SonarDistribution distrib = new SonarDistribution(VERSION_4_5_6);
    File invalidPlugin = temp.newFile("plugin.jar");
    invalidPlugin.delete();
    distrib.addPluginLocation(FileLocation.of(invalidPlugin));

    underTest.install(distrib);
  }

  @Test
  public void copy_licenses() throws Exception {
    when(zipFinder.find(VERSION_4_5_6)).thenReturn(ZIP_4_5_6);
    SonarDistribution distrib = new SonarDistribution(VERSION_4_5_6);
    distrib.activateLicense("cobol");
    when(licenses.get("cobol")).thenReturn("<LICENSE_COBOL>");
    when(licenses.licensePropertyKey("cobol")).thenReturn("sonar.cobol.license.secured");

    Server server = underTest.install(distrib);

    assertThat(openPropertiesFile(server).getProperty("sonar.cobol.license.secured")).isEqualTo("<LICENSE_COBOL>");
  }

  @Test
  public void remove_bundled_plugins_by_default() throws Exception {
    when(zipFinder.find(VERSION_4_5_6)).thenReturn(ZIP_4_5_6);
    SonarDistribution distrib = new SonarDistribution(VERSION_4_5_6);

    Server server = underTest.install(distrib);

    assertThat(bundledPlugin(server)).doesNotExist();
  }

  @Test
  public void do_not_remove_bundled_plugins() throws Exception {
    when(zipFinder.find(VERSION_4_5_6)).thenReturn(ZIP_4_5_6);
    SonarDistribution distrib = new SonarDistribution(VERSION_4_5_6).setRemoveDistributedPlugins(false);

    Server server = underTest.install(distrib);

    assertThat(bundledPlugin(server)).isFile().exists();
  }

  private File bundledPlugin(Server server) {
    return new File(server.getHome(), "lib/bundled-plugins/sonar-java-plugin-2.0.jar");
  }

  private static Properties openPropertiesFile(Server server) throws IOException {
    try (InputStream input = FileUtils.openInputStream(new File(server.getHome(), "conf/sonar.properties"))) {
      Properties conf = new Properties();
      conf.load(input);
      return conf;
    }
  }
}
