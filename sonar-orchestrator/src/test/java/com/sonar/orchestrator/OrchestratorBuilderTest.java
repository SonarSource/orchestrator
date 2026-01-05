/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource Sàrl
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
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.container.SonarDistribution;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.server.StartupLogWatcher;
import com.sonar.orchestrator.build.FakeBuild;
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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OrchestratorBuilderTest {

  private static final String LTS_ALIAS = "LATEST_RELEASE[2025.1]";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Mock
  private BuildResult successResult;

  private static class VanillaOrchestratorBuilder extends OrchestratorBuilder<VanillaOrchestratorBuilder, Orchestrator> {

    VanillaOrchestratorBuilder(Configuration initialConfig) {
      super(initialConfig);
    }

    VanillaOrchestratorBuilder(Configuration initialConfig, System2 system2) {
      super(initialConfig, system2);
    }

    @Override
    protected Orchestrator build(Configuration finalConfig, SonarDistribution distribution, StartupLogWatcher startupLogWatcher) {
      return new Orchestrator(finalConfig, distribution, startupLogWatcher);
    }
  }

  @Test
  public void executeBuild_whenPluginsAdded_shouldBePresentOnInstallationThroughApi() throws Exception {
    Orchestrator orchestrator = new VanillaOrchestratorBuilder(Configuration.createEnv())
      .setSonarVersion(LTS_ALIAS)
      .setEdition(Edition.DEVELOPER)
      // fixed version
      .addPlugin(MavenLocation.of("org.sonarsource.xml", "sonar-xml-plugin", "2.0.1.2020"))
      // alias DEV
      .addPlugin(MavenLocation.of("org.sonarsource.python", "sonar-python-plugin", "DEV"))
      // alias LATEST_RELEASE
      .addPlugin(MavenLocation.of("org.sonarsource.java", "sonar-java-plugin", "LATEST_RELEASE"))
      // alias LATEST_RELEASE[x.y]
      .addPlugin(MavenLocation.of("org.sonarsource.php", "sonar-php-plugin", "LATEST_RELEASE"))
      .build();
    try {
      orchestrator.start();

      verifyWebContext(orchestrator, "");
      assertThat(orchestrator.getServer().version().toString()).startsWith("2025.1.");
      Map<String, String> pluginVersions = loadInstalledPluginVersions(orchestrator);
      assertThat(pluginVersions).containsEntry("xml", "2.0.1 (build 2020)");
      assertThat(pluginVersions.get("python")).isNotEmpty();
      assertThat(pluginVersions.get("java")).isNotEmpty();
      assertThat(pluginVersions.get("php")).isNotEmpty();
    } finally {
      orchestrator.stop();
    }
  }

  @Test
  public void executeBuild_whenPluginAdded_shouldBePhysicallyPresentInDirectory() {
    Orchestrator orchestrator = new VanillaOrchestratorBuilder(Configuration.createEnv())
      .setSonarVersion("DEV")
      .addBundledPlugin(MavenLocation.of("org.sonarsource.xml", "sonar-xml-plugin", "1.5.0.1373"))
      .build();

    orchestrator.install();
    File dir = new File(orchestrator.getServer().getHome(), "lib/extensions");
    assertThat(dir).exists();
    assertThat(dir.listFiles()).hasSize(1);
  }

  @Test
  public void executeBuild_whenPluginAdded_shouldBeInDownloadsDirectory() {
    Orchestrator orchestrator = new VanillaOrchestratorBuilder(Configuration.createEnv())
      .setSonarVersion(LTS_ALIAS)
      .setEdition(Edition.DEVELOPER)
      .addPlugin(MavenLocation.of("org.sonarsource.xml", "sonar-xml-plugin", "1.5.0.1373"))
      .build();

    orchestrator.install();
    File dir = new File(orchestrator.getServer().getHome(), "extensions/downloads");
    assertThat(dir).exists();
    assertThat(dir.listFiles()).hasSize(1);
  }

  @Test
  public void executeBuild_shouldContainBundledPlugins() {
    Orchestrator orchestrator = new VanillaOrchestratorBuilder(Configuration.createEnv())
      .setSonarVersion(LTS_ALIAS)
      .setEdition(Edition.DEVELOPER)
      .keepBundledPlugins()
      .build();
    orchestrator.install();

    assertThat(orchestrator.getServer().version().toString()).startsWith("2025.1.");
    assertThat(orchestrator.getServer().getEdition()).isEqualTo(Edition.DEVELOPER);
    File pluginsDir = new File(orchestrator.getServer().getHome(), "lib/extensions");
    assertThat(FileUtils.listFiles(pluginsDir, new String[]{"jar"}, false)).isNotEmpty();
  }

  @Test
  public void executeBuild_whenAddedBundledPluginsToKeep_shouldBePresentInDirectory() {
    Orchestrator orchestrator = new VanillaOrchestratorBuilder(Configuration.createEnv())
      .setSonarVersion("DEV")
      .addBundledPluginToKeep("sonar-java-")
      .build();

    orchestrator.install();
    File dir = new File(orchestrator.getServer().getHome(), "lib/extensions");
    assertThat(dir).exists();

    // SonarJava consists of two plug-ins since SQ 10.6!
    // - sonar-java-plugin-<version>.jar
    // - sonar-java-symbolic-execution-plugin-<version>.jar
    assertThat(dir.listFiles()).hasSize(2);
  }

  @Test
  public void executeBuild_whenDefaultForceAuthentication() throws Exception {
    Orchestrator orchestrator = new VanillaOrchestratorBuilder(Configuration.createEnv())
      .setSonarVersion("DEV")
      .defaultForceAuthentication()
      .build();
    Server server = orchestrator.install();

    Properties properties = openPropertiesFile(server);
    assertThat(properties.getProperty("sonar.forceAuthentication")).isNull();
  }

  @Test
  public void executeBuild_whenDebugCeEnabled() throws Exception {
    Orchestrator orchestrator = new VanillaOrchestratorBuilder(Configuration.createEnv(), mock(System2.class))
      .setSonarVersion("DEV")
      .enableCeDebug()
      .build();
    Server server = orchestrator.install();

    Properties properties = openPropertiesFile(server);
    assertThat(properties).containsKey("sonar.ce.javaAdditionalOpts");
    assertThat(properties.getProperty("sonar.ce.javaAdditionalOpts")).contains("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5006");
  }

  @Test
  public void executeBuild_whenDebugWebEnabled() throws Exception {
    Orchestrator orchestrator = new VanillaOrchestratorBuilder(Configuration.createEnv(), mock(System2.class))
      .setSonarVersion("DEV")
      .enableWebDebug()
      .build();
    Server server = orchestrator.install();

    Properties properties = openPropertiesFile(server);
    assertThat(properties).containsKey("sonar.web.javaAdditionalOpts");
    assertThat(properties.getProperty("sonar.web.javaAdditionalOpts")).contains("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005");
  }

  @Test
  public void executeBuild_whenDebugEnabledOnCi_shouldFail() {
    System2 system2 = mock(System2.class);
    when(system2.getenv("CIRRUS_CI")).thenReturn("true");
    OrchestratorBuilder<?, ?> orchestratorBuilder = new VanillaOrchestratorBuilder(Configuration.create(), system2);
    assertThatThrownBy(orchestratorBuilder::enableWebDebug).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(orchestratorBuilder::enableCeDebug).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void executeBuild_whenEmptyProperties() throws Exception {
    Orchestrator orchestrator = new VanillaOrchestratorBuilder(Configuration.createEnv())
      .setSonarVersion("DEV")
      .emptySonarProperties()
      .build();
    Server server = orchestrator.install();

    Properties properties = openPropertiesFile(server);
    assertThat(properties).isEmpty();
    assertThat(server.getSearchPort()).isEqualTo(9001);
    assertThat(server.getUrl()).isEqualTo("http://localhost:9000");
  }

  @Test
  public void executeBuild_whenForceAuthenticationNotSet_shouldDefaultToFalse() throws Exception {
    Orchestrator orchestrator = new VanillaOrchestratorBuilder(Configuration.createEnv())
      .setSonarVersion("DEV")
      .build();
    Server server = orchestrator.install();

    Properties properties = openPropertiesFile(server);
    assertThat(properties.getProperty("sonar.forceAuthentication")).isEqualTo("false");
  }

  @Test
  public void executeBuild_whenDefaultForceDefaultAdminCredentialsRedirect_shouldBeNull() throws Exception {
    Orchestrator orchestrator = new VanillaOrchestratorBuilder(Configuration.createEnv())
      .setSonarVersion("DEV")
      .defaultForceDefaultAdminCredentialsRedirect()
      .build();
    Server server = orchestrator.install();

    Properties properties = openPropertiesFile(server);
    assertThat(properties.getProperty("sonar.forceRedirectOnDefaultAdminCredentials")).isNull();
  }

  @Test
  public void executeBuild_whenDefaultForceDefaultAdminCredentialsRedirectNotSet_shouldBeFalse() throws Exception {
    Orchestrator orchestrator = new VanillaOrchestratorBuilder(Configuration.createEnv())
      .setSonarVersion("DEV")
      .build();
    Server server = orchestrator.install();

    Properties properties = openPropertiesFile(server);
    assertThat(properties.getProperty("sonar.forceRedirectOnDefaultAdminCredentials")).isEqualTo("false");
  }

  @Test
  public void executeBuild_whenUseDefaultAdminCredentialsForBuildsIsEnabled() {
    Orchestrator orchestrator = new VanillaOrchestratorBuilder(Configuration.createEnv())
      .setSonarVersion("DEV")
      .useDefaultAdminCredentialsForBuilds(true)
      .build();

    try {
      orchestrator.start();
      when(successResult.isSuccess()).thenReturn(true);

      String adminToken = verifyTokenCreated(orchestrator, "sonar.token");
      verifyTokenReused(orchestrator, "sonar.token", adminToken);
      verifyTokenFromProperty(orchestrator, "sonar.token");
      verifyTokenFromProperty(orchestrator, "sonar.login");
      verifyTokenFromArgument(orchestrator, "sonar.token");
      verifyTokenFromArgument(orchestrator, "sonar.login");
      verifyTokenCreatedForMultipleBuilds(orchestrator);
    } finally {
      orchestrator.stop();
    }
  }

  @Test
  public void executeBuild_whenDefaultAdminCredentialsEnabledOnOldVersion_shouldUseLoginProperty() {
    Orchestrator orchestrator = new VanillaOrchestratorBuilder(Configuration.createEnv())
      .setSonarVersion(LTS_ALIAS)
      .setEdition(Edition.DEVELOPER)
      .useDefaultAdminCredentialsForBuilds(true)
      .build();

    try {
      orchestrator.start();
      when(successResult.isSuccess()).thenReturn(true);

      String adminToken = verifyTokenCreated(orchestrator, "sonar.token");
      verifyTokenReused(orchestrator, "sonar.token", adminToken);
    } finally {
      orchestrator.stop();
    }
  }

  private String verifyTokenCreated(Orchestrator orchestrator, String property) {
    Build<FakeBuild> fakeBuild = FakeBuild.create(successResult);
    orchestrator.executeBuild(fakeBuild);
    String token = fakeBuild.getProperties().get(property);
    assertThat(token).isNotBlank();
    return token;
  }

  private void verifyTokenReused(Orchestrator orchestrator, String property, String tokenToReuse) {
    Build<FakeBuild> fakeBuild = FakeBuild.create(successResult);
    orchestrator.executeBuild(fakeBuild);
    String token = fakeBuild.getProperties().get(property);
    assertThat(token).isNotBlank().isEqualTo(tokenToReuse);
  }

  private void verifyTokenFromProperty(Orchestrator orchestrator, String property) {
    Build<FakeBuild> fakeBuild = FakeBuild.create(successResult);
    fakeBuild.setProperty(property, "custom-property-token");
    orchestrator.executeBuildQuietly(fakeBuild);
    assertThat(fakeBuild.getProperties()).containsEntry(property, "custom-property-token");
  }

  private void verifyTokenFromArgument(Orchestrator orchestrator, String property) {
    Build<FakeBuild> fakeBuild = FakeBuild.create(successResult);
    fakeBuild.addArgument("-D" + property + "=custom-argument-token");
    orchestrator.executeBuildQuietly(fakeBuild);
    assertThat(fakeBuild.arguments()).contains("-D" + property + "=custom-argument-token");
  }

  private void verifyTokenCreatedForMultipleBuilds(Orchestrator orchestrator) {
    Build<FakeBuild> fakeBuild1 = FakeBuild.create(successResult);
    Build<FakeBuild> fakeBuild2 = FakeBuild.create(successResult);
    Build<FakeBuild> fakeBuild3 = FakeBuild.create(successResult);
    orchestrator.executeBuilds(fakeBuild1, fakeBuild2, fakeBuild3);

    assertThat(fakeBuild1.getProperties()).hasEntrySatisfying("sonar.token", s -> assertThat(s).isNotBlank());
    assertThat(fakeBuild2.getProperties()).hasEntrySatisfying("sonar.token", s -> assertThat(s).isNotBlank());
    assertThat(fakeBuild3.getProperties()).hasEntrySatisfying("sonar.token", s -> assertThat(s).isNotBlank());
  }

  @Test
  public void executeBuild_whenZipFileDoesNotExist_shouldFail() throws IOException {
    File zip = temp.newFile();
    assertThat(zip.delete()).isTrue();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("SonarQube ZIP file does not exist: " + zip.getAbsolutePath());

    new VanillaOrchestratorBuilder(Configuration.create())
      .setZipFile(zip)
      .build();
  }

  @Test
  public void executeBuild_whenZipFileIsDirectory_shouldFail() throws IOException {
    File dir = temp.newFolder();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("SonarQube ZIP is not a file: " + dir.getAbsolutePath());

    new VanillaOrchestratorBuilder(Configuration.create())
      .setZipFile(dir)
      .build();
  }

  @Test
  public void executeBuild_whenWebContextOverridden() throws Exception {
    Orchestrator orchestrator = new VanillaOrchestratorBuilder(Configuration.createEnv())
      .setSonarVersion(LTS_ALIAS)
      .setEdition(Edition.DEVELOPER)
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
    String json = orchestrator.getServer().newHttpCall("api/plugins/installed").setAdminCredentials().execute().getBodyAsString();
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
