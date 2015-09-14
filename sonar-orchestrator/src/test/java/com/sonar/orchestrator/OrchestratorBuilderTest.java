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
package com.sonar.orchestrator;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.locator.PluginLocation;
import java.net.URL;
import java.util.NoSuchElementException;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class OrchestratorBuilderTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private static URL updateCenterUrl;

  @BeforeClass
  public static void prepare() {
    updateCenterUrl = OrchestratorBuilderTest.class.getResource("/update-center-test.properties");
  }

  @Test
  public void sonarVersionIsMandatory() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Missing SonarQube version");

    new OrchestratorBuilder(Configuration.create())
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .build();
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
    assertThat(orchestrator.getDistribution().version().toString()).isEqualTo("3.0");
  }

  @Test
  public void sonarVersionIsSetToRelease() {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion("LATEST_RELEASE")
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .build();
    assertThat(orchestrator.getDistribution().version().toString()).isEqualTo("3.6.2");
  }

  @Test
  public void sonarVersionIsSetToLTS() {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion("LTS")
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .build();
    assertThat(orchestrator.getDistribution().version().toString()).isEqualTo("3.0");
    assertThat(orchestrator.getConfiguration().getSonarVersion().toString()).isEqualTo("3.0");
  }

  @Test
  public void sonarVersionIsSetToDev() {
    URL updateCenterUrl = this.getClass().getResource("/update-center-test.properties");
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion("DEV")
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .build();
    assertThat(orchestrator.getDistribution().version().toString()).startsWith("3.").endsWith("-SNAPSHOT");
    assertThat(orchestrator.getConfiguration().getSonarVersion().toString()).startsWith("3.").endsWith("-SNAPSHOT");
  }

  @Test
  public void sonarVersionIsSetToLtsOrOldestCompatible() {
    Configuration config = Configuration.builder()
      .setProperty(Configuration.SONAR_VERSION_PROPERTY, "LTS_OR_OLDEST_COMPATIBLE")
      .setProperty("abapVersion", "2.2")
      .build();
    Orchestrator orchestrator = new OrchestratorBuilder(config)
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .setMainPluginKey("abap")
      .build();
    assertThat(orchestrator.getDistribution().version().toString()).isEqualTo("3.0");
  }

  @Test
  public void sonarVersionIsSetToLtsOrOldestCompatible_Cobol_Dont_Support_LTS() {
    Configuration config = Configuration.builder()
      .setProperty(Configuration.SONAR_VERSION_PROPERTY, "LTS_OR_OLDEST_COMPATIBLE")
      .setProperty("cobolVersion", "1.14")
      .build();
    Orchestrator orchestrator = new OrchestratorBuilder(config)
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .setMainPluginKey("cobol")
      .build();
    assertThat(orchestrator.getDistribution().version().toString()).isEqualTo("3.2");
  }

  @Test
  public void pluginVersionIsSetToOldestCompatible() {
    Configuration config = Configuration.builder()
      .setProperty(Configuration.SONAR_VERSION_PROPERTY, "3.0")
      .setProperty("cobolVersion", "OLDEST_COMPATIBLE")
      .build();
    Orchestrator orchestrator = new OrchestratorBuilder(config)
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .addPlugin("cobol")
      .build();
    assertThat(orchestrator.getConfiguration().getPluginVersion("cobol").toString()).isEqualTo("1.12");
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
  public void pluginVersionIsSetToOldestCompatibleButNoVersionCompatible() {
    Configuration config = Configuration.builder()
      .setProperty(Configuration.SONAR_VERSION_PROPERTY, "3.6.2")
      .setProperty("cobolVersion", "OLDEST_COMPATIBLE")
      .build();
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("No version of cobol plugin is compatible with SonarQube 3.6.2");
    new OrchestratorBuilder(config)
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .addPlugin("cobol")
      .build();
  }

  @Test
  public void sonarVersionIsSetToLtsOrOldestCompatibleButNoMainPlugin() {
    Configuration config = Configuration.builder()
      .setProperty(Configuration.SONAR_VERSION_PROPERTY, "LTS_OR_OLDEST_COMPATIBLE")
      .setProperty("abapVersion", "2.2")
      .build();
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("You must define the main plugin when using LTS_OR_OLDEST_COMPATIBLE alias as SQ version");
    new OrchestratorBuilder(config)
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .build();
  }

  @Test
  public void sonarVersionIsSetToLtsOrOldestCompatibleButUnableToResolveMainPlugin() {
    Configuration config = Configuration.builder()
      .setProperty(Configuration.SONAR_VERSION_PROPERTY, "LTS_OR_OLDEST_COMPATIBLE")
      .setProperty("abapVersion", "OLDEST_COMPATIBLE")
      .build();
    thrown.expect(NoSuchElementException.class);
    thrown.expectMessage("Unable to find a release of plugin abap with version OLDEST_COMPATIBLE");
    new OrchestratorBuilder(config)
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .setMainPluginKey("abap")
      .build();
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

    thrown.expect(NoSuchElementException.class);
    thrown.expectMessage("Unable to find plugin with key xoo");

    new OrchestratorBuilder(config)
      .setSonarVersion("3.0")
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .addPlugin("xoo")
      .build();
  }

  @Test
  public void addPluginWithoutVersion() {
    Configuration config = Configuration.builder().setProperty("ldapVersion", "").build();

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Missing ldap plugin version. Please define property ldapVersion");

    new OrchestratorBuilder(config)
      .setSonarVersion("3.0")
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .addPlugin("ldap")
      .build();
  }

  @Test
  public void addMavenPluginEcosystem() {
    Configuration config = Configuration.builder().setProperty("javaVersion", "1.3").build();
    Orchestrator orchestrator = new OrchestratorBuilder(config)
      .setSonarVersion("3.6")
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .addPlugin("java")
      .build();
    assertThat(orchestrator.getDistribution().getPluginLocations()).contains(PluginLocation.create("java", "1.3", "org.codehaus.sonar-plugins.java", "sonar-java-plugin"));
    assertThat(orchestrator.getDistribution().getPluginLocations()).contains(PluginLocation.create("pmd", "1.3", "org.codehaus.sonar-plugins.java", "sonar-pmd-plugin"));
    assertThat(orchestrator.getDistribution().getPluginLocations()).contains(
      PluginLocation.create("checkstyle", "1.3", "org.codehaus.sonar-plugins.java", "sonar-checkstyle-plugin"));
    // ORCH-204
    assertThat(orchestrator.getConfiguration().getPluginVersion("java").toString()).isEqualTo("1.3");
    assertThat(orchestrator.getConfiguration().getPluginVersion("pmd").toString()).isEqualTo("1.3");
    assertThat(orchestrator.getConfiguration().getPluginVersion("checkstyle").toString()).isEqualTo("1.3");
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
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Unable to resolve abap plugin version DEV");
    new OrchestratorBuilder(config)
      .setSonarVersion("3.0")
      .setOrchestratorProperty("orchestrator.updateCenterUrl", updateCenterUrl.toString())
      .addPlugin("abap")
      .build();
  }
}
