/*
 * Orchestrator
 * Copyright (C) 2011-2022 SonarSource SA
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
package com.sonar.orchestrator;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import com.sonar.orchestrator.build.Build;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.FakeBuild;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.util.System2;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OrchestratorBuilderTest {

  private static final String LTS_ALIAS = "LATEST_RELEASE[8.9]";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void install_plugins_on_sonarqube_lts() throws Exception {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion(LTS_ALIAS)
      // fixed version
      .addPlugin(MavenLocation.of("org.sonarsource.xml", "sonar-xml-plugin", "1.5.0.1373"))
      // alias DEV
      .addPlugin(MavenLocation.of("org.sonarsource.python", "sonar-python-plugin", "DEV"))
      // alias LATEST_RELEASE
      .addPlugin(MavenLocation.of("org.sonarsource.java", "sonar-java-plugin", "LATEST_RELEASE"))
      // alias LATEST_RELEASE[x.y]
      .addPlugin(MavenLocation.of("org.sonarsource.php", "sonar-php-plugin", "LATEST_RELEASE[2.13]"))
      .build();
    try {
      orchestrator.start();

      verifyWebContext(orchestrator, "");
      assertThat(orchestrator.getServer().version().toString()).startsWith("8.9.");
      Map<String, String> pluginVersions = loadInstalledPluginVersions(orchestrator);
      System.out.println(pluginVersions);
      assertThat(pluginVersions.get("xml")).isEqualTo("1.5 (build 1373)");
      assertThat(pluginVersions.get("python")).isNotEmpty();
      assertThat(pluginVersions.get("java")).isNotEmpty();
      assertThat(pluginVersions.get("php")).startsWith("2.13");
    } finally {
      orchestrator.stop();
    }
  }

  @Test
  public void add_bundled_plugins() {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion("DEV")
      .addBundledPlugin(MavenLocation.of("org.sonarsource.xml", "sonar-xml-plugin", "1.5.0.1373"))
      .build();

    orchestrator.install();
    File dir = new File(orchestrator.getServer().getHome(), "lib/extensions");
    assertThat(dir).exists();
    assertThat(dir.listFiles()).hasSize(1);
  }

  @Test
  public void add_bundled_plugins_as_normal_plugin() {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion(LTS_ALIAS)
      .addPlugin(MavenLocation.of("org.sonarsource.xml", "sonar-xml-plugin", "1.5.0.1373"))
      .build();

    orchestrator.install();
    File dir = new File(orchestrator.getServer().getHome(), "extensions/downloads");
    assertThat(dir).exists();
    assertThat(dir.listFiles()).hasSize(1);
  }

  @Test
  public void install_with_bundled_plugins() {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion(LTS_ALIAS)
      .keepBundledPlugins()
      .build();
    orchestrator.install();

    assertThat(orchestrator.getServer().version().toString()).startsWith("8.9.");
    assertThat(orchestrator.getServer().getEdition()).isEqualTo(Edition.COMMUNITY);
    File pluginsDir = new File(orchestrator.getServer().getHome(), "lib/extensions");
    assertThat(FileUtils.listFiles(pluginsDir, new String[]{"jar"}, false)).isNotEmpty();
  }

  @Test
  public void enable_default_force_authentication() throws Exception {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion("DEV")
      .defaultForceAuthentication()
      .build();
    Server server = orchestrator.install();

    Properties properties = openPropertiesFile(server);
    assertThat(properties.getProperty("sonar.forceAuthentication")).isNull();
  }

  @Test
  public void enable_debug_ce() throws Exception {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create(), mock(System2.class))
      .setSonarVersion("DEV")
      .enableCeDebug()
      .build();
    Server server = orchestrator.install();

    Properties properties = openPropertiesFile(server);
    assertThat(properties).containsKey("sonar.ce.javaAdditionalOpts");
    assertThat(properties.getProperty("sonar.ce.javaAdditionalOpts")).contains("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5006");
  }

  @Test
  public void enable_debug_web() throws Exception {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create(), mock(System2.class))
      .setSonarVersion("DEV")
      .enableWebDebug()
      .build();
    Server server = orchestrator.install();

    Properties properties = openPropertiesFile(server);
    assertThat(properties).containsKey("sonar.web.javaAdditionalOpts");
    assertThat(properties.getProperty("sonar.web.javaAdditionalOpts")).contains("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005");
  }

  @Test
  public void fail_enable_debug_if_on_CI() {
    System2 system2 = mock(System2.class);
    when(system2.getenv("CIRRUS_CI")).thenReturn("true");
    OrchestratorBuilder orchestratorBuilder = new OrchestratorBuilder(Configuration.create(), system2);
    assertThatThrownBy(orchestratorBuilder::enableWebDebug).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(orchestratorBuilder::enableCeDebug).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void empty_sonar_properties() throws Exception {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion("DEV")
      .emptySonarProperties()
      .build();
    Server server = orchestrator.install();

    Properties properties = openPropertiesFile(server);
    assertThat(properties.isEmpty()).isTrue();
    assertThat(server.getSearchPort()).isEqualTo(9001);
    assertThat(server.getUrl()).isEqualTo("http://localhost:9000");
  }

  @Test
  public void disable_force_authentication_by_default() throws Exception {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion("DEV")
      .build();
    Server server = orchestrator.install();

    Properties properties = openPropertiesFile(server);
    assertThat(properties.getProperty("sonar.forceAuthentication")).isEqualTo("false");
  }

  @Test
  public void enable_default_force_redirect_on_default_admin_creds() throws Exception {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion("DEV")
      .defaultForceDefaultAdminCredentialsRedirect()
      .build();
    Server server = orchestrator.install();

    Properties properties = openPropertiesFile(server);
    assertThat(properties.getProperty("sonar.forceRedirectOnDefaultAdminCredentials")).isNull();
  }

  @Test
  public void disable_force_redirect_on_default_admin_creds_by_default() throws Exception {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion("DEV")
      .build();
    Server server = orchestrator.install();

    Properties properties = openPropertiesFile(server);
    assertThat(properties.getProperty("sonar.forceRedirectOnDefaultAdminCredentials")).isEqualTo("false");
  }

  @Test
  public void use_default_admin_credentials_for_builds_if_enabled() {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion("DEV")
      .useDefaultAdminCredentialsForBuilds(true)
      .build();

    try {
      orchestrator.start();

      BuildResult successResult = mock(BuildResult.class);
      when(successResult.isSuccess()).thenReturn(true);

      Build<FakeBuild> fakeBuild = FakeBuild.create(successResult);
      orchestrator.executeBuild(fakeBuild);
      String adminToken = fakeBuild.getProperties().get("sonar.login");
      assertThat(adminToken).isNotBlank();

      //second execution should reuse admin token
      fakeBuild = FakeBuild.create(successResult);
      orchestrator.executeBuild(fakeBuild);
      String secondAdminToken = fakeBuild.getProperties().get("sonar.login");
      assertThat(secondAdminToken).isNotBlank()
        .isEqualTo(adminToken);

      fakeBuild = FakeBuild.create(successResult);
      fakeBuild.setProperty("sonar.login", "custom-property-token");
      orchestrator.executeBuildQuietly(fakeBuild);

      assertThat(fakeBuild.getProperties()).containsEntry("sonar.login", "custom-property-token");

      fakeBuild = FakeBuild.create(successResult);
      fakeBuild.addArgument("-Dsonar.login=custom-argument-token");
      orchestrator.executeBuildQuietly(fakeBuild);

      assertThat(fakeBuild.arguments()).contains("-Dsonar.login=custom-argument-token");

      Build<FakeBuild> fakeBuild1 = FakeBuild.create(successResult);
      Build<FakeBuild> fakeBuild2 = FakeBuild.create(successResult);
      Build<FakeBuild> fakeBuild3 = FakeBuild.create(successResult);
      orchestrator.executeBuilds(fakeBuild1, fakeBuild2, fakeBuild3);

      assertThat(fakeBuild1.getProperties()).hasEntrySatisfying("sonar.login", s -> assertThat(s).isNotBlank());
      assertThat(fakeBuild2.getProperties()).hasEntrySatisfying("sonar.login", s -> assertThat(s).isNotBlank());
      assertThat(fakeBuild3.getProperties()).hasEntrySatisfying("sonar.login", s -> assertThat(s).isNotBlank());

    } finally {
      orchestrator.stop();
    }
  }

  @Test
  public void fail_if_zip_file_does_not_exist() throws IOException {
    File zip = temp.newFile();
    zip.delete();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("SonarQube ZIP file does not exist: " + zip.getAbsolutePath());

    new OrchestratorBuilder(Configuration.create())
      .setZipFile(zip)
      .build();
  }

  @Test
  public void fail_if_zip_file_is_a_directory() throws IOException {
    File dir = temp.newFolder();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("SonarQube ZIP is not a file: " + dir.getAbsolutePath());

    new OrchestratorBuilder(Configuration.create())
      .setZipFile(dir)
      .build();
  }

  @Test
  public void override_web_context() throws Exception {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion(LTS_ALIAS)
      .setServerProperty("sonar.web.context", "/sonarqube")
      .build();

    try {
      orchestrator.start();
      verifyWebContext(orchestrator, "/sonarqube");
    } finally {
      orchestrator.stop();
    }
  }

  private Map<String, String> loadInstalledPluginVersions(Orchestrator orchestrator) {
    String json = orchestrator.getServer().newHttpCall("api/plugins/installed").execute().getBodyAsString();
    JsonArray plugins = Json.parse(json)
      .asObject().get("plugins")
      .asArray();
    Map<String, String> versionByPluginKeys = new HashMap<>();
    for (JsonValue plugin : plugins) {
      versionByPluginKeys.put(plugin.asObject().get("key").asString(), plugin.asObject().get("version").asString());
    }
    return versionByPluginKeys;
  }

  private static void verifyWebContext(Orchestrator orchestrator, String expectedWebContext) throws MalformedURLException {
    URL baseUrl = new URL(orchestrator.getServer().getUrl());
    assertThat(baseUrl.getPath()).isEqualTo(expectedWebContext);
  }

  private static Properties openPropertiesFile(Server server) throws IOException {
    try (InputStream input = FileUtils.openInputStream(new File(server.getHome(), "conf/sonar.properties"))) {
      Properties conf = new Properties();
      conf.load(input);
      return conf;
    }
  }

}
