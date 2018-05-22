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
package com.sonar.orchestrator;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.locator.MavenLocation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.junit.Ignore;
import org.junit.Test;

import static com.sonar.orchestrator.container.SonarDistribution.EDITION.DATACENTER;
import static org.assertj.core.api.Assertions.assertThat;

public class OrchestratorBuilderTest {
  @Test
  public void install_plugins_on_sonarqube_lts() throws Exception {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion("LATEST_RELEASE[6.7]")
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
      assertThat(orchestrator.getServer().version().toString()).startsWith("6.7.");
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
  @Ignore
  // We need a first deployment to test this feature
  public void should_download_commercial_editions() {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion("DEV")
      .setEdition(DATACENTER)
      .build();
    try {
      orchestrator.start();
    } finally {
      orchestrator.stop();
    }
  }

  @Test
  public void override_web_context() throws Exception {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion("LATEST_RELEASE[6.7]")
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
}
