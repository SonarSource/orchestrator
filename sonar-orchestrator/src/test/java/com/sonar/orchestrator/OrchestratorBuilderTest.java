/*
 * Orchestrator
 * Copyright (C) 2011-2017 SonarSource SA
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

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.locator.PluginLocation;
import java.io.File;
import java.net.URL;
import java.util.NoSuchElementException;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class OrchestratorBuilderTest {

  private static URL updateCenterUrl;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @BeforeClass
  public static void prepare() {
    updateCenterUrl = OrchestratorBuilderTest.class.getResource("/update-center-test.properties");
  }

  @Test
  public void throw_IAE_if_both_zip_path_and_version_of_sonarqube_are_missing() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Version or path to ZIP of SonarQube is missing");

    new OrchestratorBuilder(Configuration.create())
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .build();
  }

  @Test
  public void install_sonarqube_from_local_zip_file() throws Exception {
    File zip = temp.newFile("sonarqube-6.3.1.4567.zip");

    // version is not set
    Orchestrator orch = new OrchestratorBuilder(Configuration.create())
      .setZipFile(zip)
      .build();

    assertThat(orch.getDistribution().getZipFile().get()).isEqualTo(zip);
  }

  @Test
  public void sonarVersionIsSetProgrammatically() {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion("3.0")
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .build();
    assertThat(orchestrator.getConfiguration().getSonarVersion().toString()).isEqualTo("3.0");
  }

  @Test
  public void sonarVersionIsSetByConfig() {
    Configuration config = Configuration.builder().setProperty(Configuration.SONAR_VERSION_PROPERTY, "3.0").build();
    Orchestrator orchestrator = new OrchestratorBuilder(config)
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .build();
    assertThat(orchestrator.getDistribution().version().get().toString()).isEqualTo("3.0");
  }

  @Test
  public void sonarVersionIsSetToRelease() {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion("LATEST_RELEASE")
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .build();
    assertThat(orchestrator.getDistribution().version().get().toString()).isEqualTo("3.6.2");
  }

  @Test
  public void sonarVersionIsSetToLTS() {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion("LTS")
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .build();
    assertThat(orchestrator.getDistribution().version().get().toString()).isEqualTo("3.0");
    assertThat(orchestrator.getConfiguration().getSonarVersion().toString()).isEqualTo("3.0");
  }

  @Test
  public void sonarVersionIsSetToDev() {
    URL updateCenterUrl = this.getClass().getResource("/update-center-test.properties");
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion("DEV")
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .build();
    assertThat(orchestrator.getDistribution().version().get().toString()).startsWith("3.").endsWith("-SNAPSHOT");
    assertThat(orchestrator.getConfiguration().getSonarVersion().toString()).startsWith("3.").endsWith("-SNAPSHOT");
  }

  @Test
  public void throw_IAE_if_alias_LTS_OR_OLDEST_COMPATIBLE_is_used_for_sonarqube() {
    Configuration config = Configuration.builder()
      .setProperty(Configuration.SONAR_VERSION_PROPERTY, "LTS_OR_OLDEST_COMPATIBLE")
      .setProperty("abapVersion", "2.2")
      .build();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Alias 'LTS_OR_OLDEST_COMPATIBLE' is not supported anymore for SonarQube versions");

    new OrchestratorBuilder(config)
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .build();
  }

  @Test
  public void throw_IAE_if_alias_OLDEST_COMPATIBLE_is_used() {
    Configuration config = Configuration.builder()
      .setProperty(Configuration.SONAR_VERSION_PROPERTY, "3.0")
      .setProperty("cobolVersion", "OLDEST_COMPATIBLE")
      .build();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Alias OLDEST_COMPATIBLE is not supported anymore (plugin cobol)");

    new OrchestratorBuilder(config)
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .addPlugin("cobol")
      .build();
  }

  @Test
  public void sonarVersionNotExistingFallbackToLatest() {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion("10000.0.0")
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .build();
    // ask for version 10000, must get at least the version at the time of writing this test, this is 4.5.2
    assertThat(orchestrator.getConfiguration().getSonarVersion().isGreaterThan("4.5.2")).isTrue();
  }

  @Test
  public void serverProperties() {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion("3.0")
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .setServerProperty("sonar.security.realm", "openid")
      .setOrchestratorProperty("orchestrator.container.port", "1234")
      .build();
    assertThat(orchestrator.getDistribution().getServerProperty("sonar.security.realm")).isEqualTo("openid");
    assertThat(orchestrator.getDistribution().getServerProperty("orchestrator.container.port")).isNull();
  }

  @Test
  public void orchestratorProperties() {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion("3.0")
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .setServerProperty("sonar.security.realm", "openid")
      .setOrchestratorProperty("orchestrator.container.port", "1234")
      .build();
    assertThat(orchestrator.getConfiguration().getString("orchestrator.container.port")).isEqualTo("1234");
    assertThat(orchestrator.getConfiguration().getString("sonar.security.realm")).isNull();
  }

  @Test
  public void addPlugin() {
    MavenLocation location = MavenLocation.create("org.codehaus.sonar-plugins", "sonar-foo-plugin", "1.0");
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion("3.0")
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .addPlugin(location)
      .build();
    assertThat(orchestrator.getDistribution().getPluginLocations()).contains(location);
  }

  @Test
  public void addMavenPluginEnv() {
    Configuration config = Configuration.builder().setProperty("abapVersion", "2.2").build();
    OrchestratorBuilder builder = new OrchestratorBuilder(config)
      .setSonarVersion("3.0")
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .addPlugin("abap");
    assertThat(builder.getPluginVersion("abap")).isEqualTo("2.2");
    Orchestrator orchestrator = builder.build();
    assertThat(orchestrator.getDistribution().getPluginLocations()).contains(PluginLocation.create("abap", "2.2", "com.sonarsource.abap", "sonar-abap-plugin"));
    assertThat(orchestrator.getConfiguration().getPluginVersion("abap").toString()).isEqualTo("2.2");
  }

  // ORCH-189
  @Test
  public void addPluginNotFound() {
    Configuration config = Configuration.builder().setProperty("xooVersion", "RELEASE").build();

    expectedException.expect(NoSuchElementException.class);
    expectedException.expectMessage("Unable to find plugin with key xoo");

    new OrchestratorBuilder(config)
      .setSonarVersion("3.0")
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .addPlugin("xoo")
      .build();
  }

  @Test
  public void addPluginWithoutVersion() {
    Configuration config = Configuration.builder().setProperty("ldapVersion", "").build();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Missing ldap plugin version. Please define property ldapVersion");

    new OrchestratorBuilder(config)
      .setSonarVersion("3.0")
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .addPlugin("ldap")
      .build();
  }

  @Test
  public void removeDistributedPlugins() {
    assertThat(Orchestrator.builderEnv()
      .setSonarVersion("3.6")
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .build().getDistribution().removeDistributedPlugins()).isTrue();
    assertThat(Orchestrator.builderEnv()
      .setSonarVersion("3.6")
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .build().getDistribution().removeDistributedPlugins()).isTrue();
  }

  @Test
  public void addMavenPluginEnvRelease() {
    Configuration config = Configuration.builder().setProperty("abapVersion", "LATEST_RELEASE").build();
    Orchestrator orchestrator = new OrchestratorBuilder(config)
      .setSonarVersion("3.0")
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .addPlugin("abap")
      .build();
    assertThat(orchestrator.getDistribution().getPluginLocations()).contains(PluginLocation.create("abap", "2.2", "com.sonarsource.abap", "sonar-abap-plugin"));
    assertThat(orchestrator.getConfiguration().getPluginVersion("abap").toString()).isEqualTo("2.2");
  }

  @Test
  public void addPluginDevNotAvailable() {
    Configuration config = Configuration.builder().setProperty("abapVersion", "DEV").build();
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Unable to resolve abap plugin version DEV");
    new OrchestratorBuilder(config)
      .setSonarVersion("3.0")
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .addPlugin("abap")
      .build();
  }

  @Test
  public void override_web_context() {
    Configuration config = Configuration.builder().setProperty("sonar.runtimeVersion", "5.6").build();
    Orchestrator orch = new OrchestratorBuilder(config)
      .setContext("/foo")
      .build();
    assertThat(orch.getDistribution().getServerProperty("sonar.web.context")).isEqualTo("/foo");
  }

  @Test
  public void web_context_is_not_defined_by_default() {
    Configuration config = Configuration.builder().setProperty("sonar.runtimeVersion", "5.6").build();
    Orchestrator orch = new OrchestratorBuilder(config).build();
    assertThat(orch.getDistribution().getServerProperty("sonar.web.context")).isNull();
  }
}
