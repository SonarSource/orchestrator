/*
 * Orchestrator
 * Copyright (C) 2011-2019 SonarSource SA
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
import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class OrchestratorBuilderTest {

  private static final String LTS_ALIAS = "LATEST_RELEASE[6.7]";

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
  public void install_with_bundled_plugins() {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion(LTS_ALIAS)
      .keepBundledPlugins()
      .build();
    orchestrator.install();

    assertThat(orchestrator.getServer().version().toString()).startsWith("6.7.");
    assertThat(orchestrator.getServer().getEdition()).isEqualTo(Edition.COMMUNITY);
    File pluginsDir = new File(orchestrator.getServer().getHome(), "lib/bundled-plugins");
    assertThat(FileUtils.listFiles(pluginsDir, new String[]{"jar"}, false)).isNotEmpty();
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
}
