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
package com.sonar.orchestrator.dsl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilderTest;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.MavenLocation;
import java.net.URL;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class StartServerCommandTest {

  private static URL updateCenterUrl;
  private static ImmutableMap<String, String> SETTINGS;

  @BeforeClass
  public static void prepare() {
    updateCenterUrl = OrchestratorBuilderTest.class.getResource("/update-center-test.properties");
    SETTINGS = ImmutableMap.of(
      "sonar.runtimeVersion", "5.6",
      "sonar.jdbc.dialect", "h2",
      "orchestrator.updateCenterUrl", updateCenterUrl.toString()
      );
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void should_init_orchestrator() {
    StartServerCommand command = new StartServerCommand();
    Dsl.Context context = new Dsl.Context().setSettings(SETTINGS);
    Orchestrator orchestrator = command.initOrchestrator(context);

    assertThat(orchestrator).isNotNull();
    assertThat(orchestrator.getConfiguration().getString("sonar.runtimeVersion")).isEqualTo("5.6");
    assertThat(orchestrator.getConfiguration().getString("sonar.jdbc.dialect")).isEqualTo("h2");
  }

  @Test
  public void should_install_plugins() {
    StartServerCommand command = new StartServerCommand();
    command.addPlugin(new StartServerCommand.Plugin("cobol", "1.12"));
    Dsl.Context context = new Dsl.Context().setSettings(SETTINGS);
    Orchestrator orchestrator = command.initOrchestrator(context);

    List<Location> plugins = orchestrator.getDistribution().getPluginLocations();
    // cobol and orchestrator reset-data
    assertThat(plugins).hasSize(2);

    Location plugin = Iterables.find(plugins, location -> location instanceof MavenLocation);
    assertThat(plugin).isNotNull();
    MavenLocation pluginLocation = (MavenLocation) plugin;
    assertThat(pluginLocation.getGroupId()).isEqualTo("com.sonarsource.cobol");
    assertThat(pluginLocation.getArtifactId()).isEqualTo("sonar-cobol-plugin");
    assertThat(pluginLocation.version().toString()).isEqualTo("1.12");
  }

}
