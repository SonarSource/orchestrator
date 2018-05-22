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
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.container.SonarDistribution;
import com.sonar.orchestrator.db.DatabaseClient;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerInstallerTest {

  private static final File ZIP_4_5_6 = FileUtils.toFile(ServerInstallerTest.class.getResource("ServerInstallerTest/sonarqube-4.5.6-lite.zip"));
  private static final String VERSION_4_5_6 = "4.5.6";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private PackagingResolver packagingResolver = mock(PackagingResolver.class);
  private DatabaseClient dbClient = mock(DatabaseClient.class);
  private File installsDir;
  private File workspaceDir;
  private File mavenLocalDir;

  @Before
  public void setUp() throws IOException {
    installsDir = temp.newFolder();
    workspaceDir = temp.newFolder();
    mavenLocalDir = temp.newFolder();
    when(dbClient.getUrl()).thenReturn("jdbc:h2:mem");
    when(dbClient.getLogin()).thenReturn("sonar");
    when(dbClient.getPassword()).thenReturn("sonar");
  }

  @Test
  public void test_install() throws Exception {
    when(packagingResolver.resolve(any())).thenReturn(new Packaging(SonarDistribution.Edition.COMMUNITY, Version.create(VERSION_4_5_6), ZIP_4_5_6));

    Server server = newInstaller().install(new SonarDistribution().setVersion(VERSION_4_5_6));
    assertThat(server.getDistribution().getVersion().get()).isEqualTo(VERSION_4_5_6);
    // installed in a unique location. Home directory is the name defined in zip structure
    assertThat(server.getHome().getParentFile().getParentFile()).isEqualTo(workspaceDir);
    Properties props = openPropertiesFile(server);
    assertThat(props.getProperty("sonar.jdbc.url")).isEqualTo("jdbc:h2:mem");
  }

  @Test
  public void use_random_web_port_on_loopback_address_if_not_defined() throws Exception {
    when(packagingResolver.resolve(any())).thenReturn(new Packaging(SonarDistribution.Edition.COMMUNITY, Version.create(VERSION_4_5_6), ZIP_4_5_6));

    Server server = newInstaller().install(new SonarDistribution().setVersion(VERSION_4_5_6));

    URL serverUrl = new URL(server.getUrl());
    assertThat(InetAddress.getByName(serverUrl.getHost()).isLoopbackAddress()).isTrue();
    assertThat(serverUrl.getPort()).isGreaterThan(1023);
    // no web context
    assertThat(serverUrl.getPath()).isEqualTo("");
  }

  @Test
  public void web_server_is_configured_through_sonar_properties() throws Exception {
    when(packagingResolver.resolve(any())).thenReturn(new Packaging(SonarDistribution.Edition.COMMUNITY, Version.create(VERSION_4_5_6), ZIP_4_5_6));
    SonarDistribution distribution = new SonarDistribution().setVersion(VERSION_4_5_6);
    distribution
      .setServerProperty("sonar.web.port", "9999")
      .setServerProperty("sonar.web.context", "/foo");
    Server server = newInstaller().install(distribution);

    URL serverUrl = new URL(server.getUrl());
    assertThat(serverUrl.getPort()).isEqualTo(9999);
    assertThat(serverUrl.getPath()).isEqualTo("/foo");
  }

  @Test
  public void web_port_can_be_set_through_special_property_in_orchestrator_properties_file() throws Exception {
    when(packagingResolver.resolve(any())).thenReturn(new Packaging(SonarDistribution.Edition.COMMUNITY, Version.create(VERSION_4_5_6), ZIP_4_5_6));
    SonarDistribution distribution = new SonarDistribution().setVersion(VERSION_4_5_6);
    Configuration.Builder configBuilder = Configuration.builder()
      .setProperty("orchestrator.container.port", "9999");
    Server server = newInstaller(configBuilder).install(distribution);

    URL serverUrl = new URL(server.getUrl());
    assertThat(InetAddress.getByName(serverUrl.getHost()).isLoopbackAddress()).isTrue();
    assertThat(serverUrl.getPort()).isEqualTo(9999);
    assertThat(serverUrl.getPath()).isEqualTo("");
  }

  @Test
  public void special_orchestrator_property_for_web_port_is_not_used_if_port_defined_explicitly() throws Exception {
    when(packagingResolver.resolve(any())).thenReturn(new Packaging(SonarDistribution.Edition.COMMUNITY, Version.create(VERSION_4_5_6), ZIP_4_5_6));
    SonarDistribution distribution = new SonarDistribution().setVersion(VERSION_4_5_6)
      .setServerProperty("sonar.web.port", "10000");

    Configuration.Builder configBuilder = Configuration.builder()
      .setProperty("orchestrator.container.port", "9999");
    Server server = newInstaller(configBuilder).install(distribution);

    URL serverUrl = new URL(server.getUrl());
    assertThat(serverUrl.getPort()).isEqualTo(10000);
  }

  @Test
  public void installation_directories_do_not_overlap() throws Exception {
    when(packagingResolver.resolve(any())).thenReturn(new Packaging(SonarDistribution.Edition.COMMUNITY, Version.create(VERSION_4_5_6), ZIP_4_5_6));

    Server server1 = newInstaller().install(new SonarDistribution().setVersion(VERSION_4_5_6).setServerProperty("test.id", "1"));
    Server server2 = newInstaller().install(new SonarDistribution().setVersion(VERSION_4_5_6).setServerProperty("test.id", "2"));
    assertThat(server1.getHome()).exists().isDirectory();
    assertThat(server2.getHome()).exists().isDirectory();
    assertThat(server1.getHome()).isNotEqualTo(server2.getHome());
    assertThat(openPropertiesFile(server1).getProperty("test.id")).isEqualTo("1");
    assertThat(openPropertiesFile(server2).getProperty("test.id")).isEqualTo("2");
  }

  @Test
  public void copy_jdbc_driver_if_defined() throws Exception {
    when(packagingResolver.resolve(any())).thenReturn(new Packaging(SonarDistribution.Edition.COMMUNITY, Version.create(VERSION_4_5_6), ZIP_4_5_6));
    when(dbClient.getDriverFile()).thenReturn(FileUtils.toFile(getClass().getResource("ServerInstallerTest/fake-oracle-driver.jar")));
    when(dbClient.getDialect()).thenReturn("oracle");

    Server server = newInstaller().install(new SonarDistribution().setVersion(VERSION_4_5_6));
    assertThat(new File(server.getHome(), "extensions/jdbc-driver/oracle/fake-oracle-driver.jar")).exists().isFile();
  }

  @Test
  public void throw_ISE_if_fail_to_copy_jdbc_driver() throws Exception {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to copy JDBC driver");

    when(packagingResolver.resolve(any())).thenReturn(new Packaging(SonarDistribution.Edition.COMMUNITY, Version.create(VERSION_4_5_6), ZIP_4_5_6));
    File invalidDriver = temp.newFile();
    invalidDriver.delete();
    when(dbClient.getDriverFile()).thenReturn(invalidDriver);
    when(dbClient.getDialect()).thenReturn("oracle");

    newInstaller().install(new SonarDistribution().setVersion(VERSION_4_5_6));
  }

  @Test
  public void copy_plugins() throws Exception {
    when(packagingResolver.resolve(any())).thenReturn(new Packaging(SonarDistribution.Edition.COMMUNITY, Version.create(VERSION_4_5_6), ZIP_4_5_6));
    SonarDistribution distrib = new SonarDistribution().setVersion(VERSION_4_5_6);
    distrib.addPluginLocation(FileLocation.of(FileUtils.toFile(getClass().getResource("ServerInstallerTest/fake-plugin.jar"))));

    Server server = newInstaller().install(distrib);

    assertThat(new File(server.getHome(), "extensions/downloads/fake-plugin.jar")).exists().isFile();
  }

  @Test
  public void throw_ISE_if_fail_to_copy_plugins() throws Exception {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can not find the plugin");

    when(packagingResolver.resolve(any())).thenReturn(new Packaging(SonarDistribution.Edition.COMMUNITY, Version.create(VERSION_4_5_6), ZIP_4_5_6));
    SonarDistribution distrib = new SonarDistribution().setVersion(VERSION_4_5_6);
    File invalidPlugin = temp.newFile("plugin.jar");
    invalidPlugin.delete();
    distrib.addPluginLocation(FileLocation.of(invalidPlugin));

    newInstaller().install(distrib);
  }

  @Test
  public void remove_bundled_plugins_by_default() throws Exception {
    when(packagingResolver.resolve(any())).thenReturn(new Packaging(SonarDistribution.Edition.COMMUNITY, Version.create(VERSION_4_5_6), ZIP_4_5_6));
    SonarDistribution distrib = new SonarDistribution().setVersion(VERSION_4_5_6);

    Server server = newInstaller().install(distrib);

    assertThat(bundledPlugin(server)).doesNotExist();
  }

  @Test
  public void do_not_remove_bundled_plugins() throws Exception {
    when(packagingResolver.resolve(any())).thenReturn(new Packaging(SonarDistribution.Edition.COMMUNITY, Version.create(VERSION_4_5_6), ZIP_4_5_6));
    SonarDistribution distrib = new SonarDistribution().setVersion(VERSION_4_5_6).setRemoveDistributedPlugins(false);

    Server server = newInstaller().install(distrib);

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

  private ServerInstaller newInstaller() {
    return newInstaller(Configuration.builder());
  }

  private ServerInstaller newInstaller(Configuration.Builder configBuilder) {
    Configuration config = configBuilder
      .setProperty("orchestrator.sonarInstallsDir", installsDir.getAbsolutePath())
      .setProperty("orchestrator.workspaceDir", workspaceDir.getAbsolutePath())
      .setProperty("maven.localRepository", mavenLocalDir.getAbsolutePath())
      .build();
    return new ServerInstaller(packagingResolver, config, dbClient);
  }
}
