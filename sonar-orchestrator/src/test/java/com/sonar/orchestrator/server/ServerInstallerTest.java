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
import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.container.SonarDistribution;
import com.sonar.orchestrator.db.DatabaseClient;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.Collection;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerInstallerTest {

  private static final File SQ_LITE_ZIP = FileUtils.toFile(ServerInstallerTest.class.getResource("ServerInstallerTest/sonarqube-4.5.6-lite.zip"));
  private static final String VERSION_9_9 = "9.9";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private final Locators locators = mock(Locators.class, Mockito.RETURNS_DEEP_STUBS);
  private final PackagingResolver packagingResolver = mock(PackagingResolver.class);
  private final DatabaseClient dbClient = mock(DatabaseClient.class);
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
    prepareResolutionOfPackaging(Edition.COMMUNITY, Version.create(VERSION_9_9), SQ_LITE_ZIP);

    Server server = newInstaller().install(new SonarDistribution().setVersion(VERSION_9_9));
    assertThat(server.version()).isEqualTo(Version.create(VERSION_9_9));
    // installed in a unique location. Home directory is the name defined in zip structure
    assertThat(server.getHome().getParentFile()).hasParent(workspaceDir);
    assertThat(server.getEdition()).isEqualTo(Edition.COMMUNITY);
    Properties props = openPropertiesFile(server);
    assertThat(props.getProperty("sonar.jdbc.url")).isEqualTo("jdbc:h2:mem");
    assertThat(props.getProperty("sonar.forceAuthentication")).isEqualTo("false");
    assertThat(props.getProperty("sonar.forceRedirectOnDefaultAdminCredentials")).isEqualTo("false");
  }

  @Test
  public void force_authentication_fallback_to_false_for_8_6_and_greater() throws Exception {
    prepareResolutionOfPackaging(Edition.DATACENTER, Version.create(VERSION_9_9), SQ_LITE_ZIP);

    Server server = newInstaller().install(new SonarDistribution()
        .setVersion(VERSION_9_9));

    Properties props = openPropertiesFile(server);
    assertThat(props.getProperty("sonar.forceAuthentication")).isEqualTo("false");
  }

  @Test
  public void default_force_authentication_can_be_enabled() throws Exception {
    prepareResolutionOfPackaging(Edition.DATACENTER, Version.create(VERSION_9_9), SQ_LITE_ZIP);

    Server server = newInstaller().install(new SonarDistribution()
        .setDefaultForceAuthentication(true)
        .setVersion(VERSION_9_9));

    Properties props = openPropertiesFile(server);
    assertThat(props.getProperty("sonar.forceAuthentication")).isNull();
  }

  @Test
  public void force_default_admin_creds_redirect_fallback_to_false() throws Exception {
    prepareResolutionOfPackaging(Edition.DATACENTER, Version.create(VERSION_9_9), SQ_LITE_ZIP);

    Server server = newInstaller().install(new SonarDistribution()
      .setVersion(VERSION_9_9));

    Properties props = openPropertiesFile(server);
    assertThat(props.getProperty("sonar.forceRedirectOnDefaultAdminCredentials")).isEqualTo("false");
  }

  @Test
  public void default_admin_creds_redirect_can_be_enabled() throws Exception {
    prepareResolutionOfPackaging(Edition.DATACENTER, Version.create(VERSION_9_9), SQ_LITE_ZIP);

    Server server = newInstaller().install(new SonarDistribution()
      .setForceDefaultAdminCredentialsRedirect(true)
      .setVersion(VERSION_9_9));

    Properties props = openPropertiesFile(server);
    assertThat(props.getProperty("sonar.forceRedirectOnDefaultAdminCredentials")).isNull();
  }

  @Test
  public void use_random_web_port_on_loopback_address_if_not_defined() throws Exception {
    prepareResolutionOfPackaging(Edition.COMMUNITY, Version.create(VERSION_9_9), SQ_LITE_ZIP);

    Server server = newInstaller().install(new SonarDistribution().setVersion(VERSION_9_9));

    URL serverUrl = new URL(server.getUrl());
    assertThat(InetAddress.getByName(serverUrl.getHost()).isLoopbackAddress()).isTrue();
    assertThat(serverUrl.getPort()).isGreaterThan(1023);
    // no web context
    assertThat(serverUrl.getPath()).isEmpty();
  }

  // ORCH-422
  @Test
  public void show_0_0_0_0_as_localhost() throws Exception {
    prepareResolutionOfPackaging(Edition.COMMUNITY, Version.create(VERSION_9_9), SQ_LITE_ZIP);

    SonarDistribution distribution = new SonarDistribution().setVersion(VERSION_9_9);
    distribution
      .setServerProperty("sonar.web.host", "0.0.0.0");
    Server server = newInstaller().install(distribution);

    URL serverUrl = new URL(server.getUrl());
    assertThat(InetAddress.getByName(serverUrl.getHost()).isLoopbackAddress()).isTrue();
  }

  @Test
  public void use_random_search_port_on_loopback_address_if_not_defined() {
    prepareResolutionOfPackaging(Edition.COMMUNITY, Version.create(VERSION_9_9), SQ_LITE_ZIP);

    Server server = newInstaller().install(new SonarDistribution().setVersion(VERSION_9_9));

    assertThat(server.getSearchPort()).isGreaterThan(1023);
  }

  @Test
  public void throw_if_search_port_on_loopback_address_is_not_defined() {
    prepareResolutionOfPackaging(Edition.DATACENTER, Version.create(VERSION_9_9), SQ_LITE_ZIP);

    SonarDistribution distribution = new SonarDistribution()
      .setVersion(VERSION_9_9)
      .setServerProperty("sonar.cluster.enabled", "true")
      .setServerProperty("sonar.cluster.node.type", "search");
    ServerInstaller serverInstaller = newInstaller();

    assertThatThrownBy(() -> serverInstaller.install(distribution))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Cluster configuration must provide the property sonar.cluster.node.es.host upfront");
  }

  @Test
  public void use_random_search_port_on_loopback_address_if_not_defined_in_old_SQ_DCE_application_node() {
    prepareResolutionOfPackaging(Edition.DATACENTER, Version.create(VERSION_9_9), SQ_LITE_ZIP);

    Server server = newInstaller().install(new SonarDistribution()
      .setVersion(VERSION_9_9)
      .setServerProperty("sonar.cluster.enabled", "true")
      .setServerProperty("sonar.cluster.node.type", "application"));

    assertThat(server.getSearchPort()).isGreaterThan(1023);
  }

  @Test
  public void use_random_search_port_if_not_defined_in_new_SQ_DCE_search_node() {
    prepareResolutionOfPackaging(Edition.DATACENTER, Version.create(VERSION_9_9), SQ_LITE_ZIP);

    Server server = newInstaller().install(new SonarDistribution()
      .setVersion(VERSION_9_9)
      .setServerProperty("sonar.cluster.enabled", "true")
      .setServerProperty("sonar.cluster.node.type", "search")
      .setServerProperty("sonar.cluster.node.search.host", "0.0.0.0")
      .setServerProperty("sonar.cluster.node.es.host", "0.0.0.0"));

    assertThat(server.getSearchPort()).isGreaterThan(1023);
  }

  @Test
  public void use_random_port_on_loopback_address_if_not_defined_in_application_node() {
    prepareResolutionOfPackaging(Edition.DATACENTER, Version.create(VERSION_9_9), SQ_LITE_ZIP);

    Server server = newInstaller().install(new SonarDistribution()
      .setVersion(VERSION_9_9)
      .setServerProperty("sonar.cluster.enabled", "true")
      .setServerProperty("sonar.cluster.node.type", "application"));

    assertThat(server.getSearchPort()).isGreaterThan(1023);
  }

  @Test
  public void web_server_is_configured_through_sonar_properties() throws Exception {
    prepareResolutionOfPackaging(Edition.COMMUNITY, Version.create(VERSION_9_9), SQ_LITE_ZIP);
    SonarDistribution distribution = new SonarDistribution().setVersion(VERSION_9_9);
    distribution
      .setServerProperty("sonar.web.port", "9999")
      .setServerProperty("sonar.web.context", "/foo")
      .setServerProperty("sonar.search.port", "6666")
      .setServerProperty("sonar.es.port", "7777")
    ;
    Server server = newInstaller().install(distribution);

    URL serverUrl = new URL(server.getUrl());
    assertThat(serverUrl.getPort()).isEqualTo(9999);
    assertThat(serverUrl.getPath()).isEqualTo("/foo");

    Properties props = openPropertiesFile(server);
    assertThat(props.getProperty("sonar.search.port")).isEqualTo("6666");
    assertThat(props.getProperty("sonar.es.port")).isEqualTo("7777");
  }

  @Test
  public void web_port_can_be_set_through_special_property_in_orchestrator_properties_file() throws Exception {
    prepareResolutionOfPackaging(Edition.COMMUNITY, Version.create(VERSION_9_9), SQ_LITE_ZIP);
    SonarDistribution distribution = new SonarDistribution().setVersion(VERSION_9_9);
    Configuration.Builder configBuilder = Configuration.builder()
      .setProperty("orchestrator.container.port", "9999");
    Server server = newInstaller(configBuilder).install(distribution);

    URL serverUrl = new URL(server.getUrl());
    assertThat(InetAddress.getByName(serverUrl.getHost()).isLoopbackAddress()).isTrue();
    assertThat(serverUrl.getPort()).isEqualTo(9999);
    assertThat(serverUrl.getPath()).isEmpty();
  }

  @Test
  public void special_orchestrator_property_for_web_port_is_not_used_if_port_defined_explicitly() throws Exception {
    prepareResolutionOfPackaging(Edition.COMMUNITY, Version.create(VERSION_9_9), SQ_LITE_ZIP);
    SonarDistribution distribution = new SonarDistribution().setVersion(VERSION_9_9)
      .setServerProperty("sonar.web.port", "10000");

    Configuration.Builder configBuilder = Configuration.builder()
      .setProperty("orchestrator.container.port", "9999");
    Server server = newInstaller(configBuilder).install(distribution);

    URL serverUrl = new URL(server.getUrl());
    assertThat(serverUrl.getPort()).isEqualTo(10000);
  }

  @Test
  public void installation_directories_do_not_overlap() throws Exception {
    prepareResolutionOfPackaging(Edition.COMMUNITY, Version.create(VERSION_9_9), SQ_LITE_ZIP);

    Server server1 = newInstaller().install(new SonarDistribution().setVersion(VERSION_9_9).setServerProperty("test.id", "1"));
    Server server2 = newInstaller().install(new SonarDistribution().setVersion(VERSION_9_9).setServerProperty("test.id", "2"));
    assertThat(server1.getHome()).exists().isDirectory();
    assertThat(server2.getHome()).exists().isDirectory();
    assertThat(server1.getHome()).isNotEqualTo(server2.getHome());
    assertThat(openPropertiesFile(server1).getProperty("test.id")).isEqualTo("1");
    assertThat(openPropertiesFile(server2).getProperty("test.id")).isEqualTo("2");
  }

  @Test
  public void copy_jdbc_driver_if_defined() {
    prepareResolutionOfPackaging(Edition.COMMUNITY, Version.create(VERSION_9_9), SQ_LITE_ZIP);
    when(dbClient.getDriverFile()).thenReturn(FileUtils.toFile(getClass().getResource("ServerInstallerTest/fake-oracle-driver.jar")));
    when(dbClient.getDialect()).thenReturn("oracle");

    Server server = newInstaller().install(new SonarDistribution().setVersion(VERSION_9_9));
    assertThat(new File(server.getHome(), "extensions/jdbc-driver/oracle/fake-oracle-driver.jar")).exists().isFile();
  }

  @Test
  public void throw_ISE_if_fail_to_copy_jdbc_driver() throws Exception {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to copy JDBC driver");

    prepareResolutionOfPackaging(Edition.COMMUNITY, Version.create(VERSION_9_9), SQ_LITE_ZIP);
    File invalidDriver = temp.newFile();
    invalidDriver.delete();
    when(dbClient.getDriverFile()).thenReturn(invalidDriver);
    when(dbClient.getDialect()).thenReturn("oracle");

    newInstaller().install(new SonarDistribution().setVersion(VERSION_9_9));
  }

  @Test
  public void copy_plugins() throws IOException {
    prepareResolutionOfPackaging(Edition.COMMUNITY, Version.create(VERSION_9_9), SQ_LITE_ZIP);
    SonarDistribution distribution = new SonarDistribution().setVersion(VERSION_9_9);
    distribution.addPluginLocation(MavenLocation.of("fake", "sonar-foo-plugin", "1.0"));
    distribution.addPluginLocation(MavenLocation.of("fake", "sonar-bar-plugin", "1.0"));
    File jar1 = temp.newFile();
    File jar2 = temp.newFile();
    prepareCopyOfPlugin("sonar-foo-plugin", "1.0", jar1);
    prepareCopyOfPlugin("sonar-bar-plugin", "1.0", jar2);

    Server server = newInstaller().install(distribution);

    Collection<File> installedFiles = FileUtils.listFiles(new File(server.getHome(), "extensions/downloads"), null, false);
    assertThat(installedFiles).extracting(File::getName).containsExactlyInAnyOrder(jar1.getName(), jar2.getName());
  }

  private void prepareCopyOfPlugin(String artifactId, String version, File pluginJar) {
    doAnswer(invocationOnMock -> {
      File toDir = invocationOnMock.getArgument(1);
      FileUtils.copyFileToDirectory(pluginJar, toDir);
      return new File(toDir, pluginJar.getName());
    }).when(locators).copyToDirectory(argThat(l -> {
      MavenLocation ml = (MavenLocation) l;
      return ml.getArtifactId().equals(artifactId) && ml.getVersion().equalsIgnoreCase(version);
    }), any());
  }

  @Test
  public void throw_ISE_if_fail_to_copy_plugins() throws Exception {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can not find the plugin");

    prepareResolutionOfPackaging(Edition.COMMUNITY, Version.create(VERSION_9_9), SQ_LITE_ZIP);
    SonarDistribution distrib = new SonarDistribution().setVersion(VERSION_9_9);
    File invalidPlugin = temp.newFile("plugin.jar");
    invalidPlugin.delete();
    distrib.addPluginLocation(FileLocation.of(invalidPlugin));

    newInstaller().install(distrib);
  }

  @Test
  public void remove_bundled_plugins_by_default() {
    prepareResolutionOfPackaging(Edition.COMMUNITY, Version.create(VERSION_9_9), SQ_LITE_ZIP);
    SonarDistribution distrib = new SonarDistribution().setVersion(VERSION_9_9);

    Server server = newInstaller().install(distrib);

    assertThat(bundledPlugin(server)).doesNotExist();
  }

  @Test
  public void keep_bundled_plugins() {
    prepareResolutionOfPackaging(Edition.COMMUNITY, Version.create(VERSION_9_9), SQ_LITE_ZIP);
    SonarDistribution distrib = new SonarDistribution().setVersion(VERSION_9_9).setKeepBundledPlugins(true);

    Server server = newInstaller().install(distrib);

    assertThat(bundledPlugin(server)).isFile().exists();
  }

  private void prepareResolutionOfPackaging(Edition edition, Version version, File zip) {
    when(packagingResolver.resolve(any())).thenReturn(new Packaging(edition, version, zip));
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
    return new ServerInstaller(packagingResolver, config, locators, dbClient);
  }
}
