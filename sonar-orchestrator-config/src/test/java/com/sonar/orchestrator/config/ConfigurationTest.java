/*
 * Orchestrator Configuration
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
package com.sonar.orchestrator.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Strings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.MapEntry.entry;

class ConfigurationTest {

  @Test
  void getString() {
    Properties props = new Properties();
    props.setProperty("foo", "bar");
    Configuration config = Configuration.create(props);

    assertThat(config.getString("foo")).isEqualTo("bar");
    assertThat(config.getString("xxx")).isNull();
    assertThat(config.getString("xxx", "default")).isEqualTo("default");
  }

  @Test
  void getInt() {
    Properties props = new Properties();
    props.setProperty("one", "1");
    Configuration config = Configuration.create(props);

    assertThat(config.getInt("one", -1)).isEqualTo(1);
    assertThat(config.getInt("two", -1)).isEqualTo(-1);
  }

  @Test
  void asMap() {
    Properties props = new Properties();
    props.setProperty("foo", "1");
    props.setProperty("bar", "2");

    Configuration config = Configuration.create(props);

    assertThat(config.asMap()).contains(entry("foo", "1"), entry("bar", "2"));
  }

  @Test
  void asMap_immutable() {
    Properties props = new Properties();
    props.setProperty("foo", "bar");
    Configuration config = Configuration.create(props);

    Map<String, String> map = config.asMap();
    assertThatThrownBy(() -> map.put("new", "property")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void createEnv() {
    Configuration config = Configuration.createEnv();
    assertThat(config.getString("java.io.tmpdir")).isNotNull();
  }

  @Test
  void interpolateProperties() {
    Properties props = new Properties();
    props.setProperty("where", "FR");
    props.setProperty("codeStory", "devoxx${where}");

    Configuration config = Configuration.create(props);
    assertThat(config.getString("codeStory")).isEqualTo("devoxxFR");
  }

  @Test
  void loadPropertiesFile() {
    URL url = getClass().getResource("/com/sonar/orchestrator/config/ConfigurationTest/sample.properties");
    Properties props = new Properties();
    props.setProperty("foo", "bar");
    props.setProperty("orchestrator.configUrl", url.toString());

    Configuration config = Configuration.create(props);
    assertThat(config.getString("foo")).isEqualTo("bar");
    assertThat(config.getString("yes")).isEqualTo("true");
    assertThat(config.getString("loadedFromFile")).isEqualTo("true");
  }

  @Test
  void loadPropertiesFileFromHomeByDefault(@TempDir Path homeDir) throws IOException {
    URL url = getClass().getResource("/com/sonar/orchestrator/config/ConfigurationTest/sample.properties");
    FileUtils.copyURLToFile(url, new File(homeDir.toFile(), "orchestrator.properties"));
    Properties props = new Properties();
    props.setProperty("foo", "bar");
    props.setProperty("orchestrator.home", homeDir.toString());

    Configuration config = Configuration.create(props);
    assertThat(config.getString("foo")).isEqualTo("bar");
    assertThat(config.getString("yes")).isEqualTo("true");
    assertThat(config.getString("loadedFromFile")).isEqualTo("true");
  }

  @Test
  void shouldOverridePropertiesFile() {
    URL url = getClass().getResource("/com/sonar/orchestrator/config/ConfigurationTest/sample.properties");
    Properties props = new Properties();
    props.setProperty("yes", "vrai");
    props.setProperty("orchestrator.configUrl", url.toString());

    Configuration config = Configuration.create(props);
    // the file contains yes=true, but it should not override the initial yes=vrai
    assertThat(config.getString("yes")).isEqualTo("vrai");
    assertThat(config.getString("loadedFromFile")).isEqualTo("vrai");
  }

  @Test
  void loadRemotePropertiesFile() {
    WireMockServer wireMockServer = new WireMockServer(options().dynamicPort());
    try {
      wireMockServer.start();
      wireMockServer.stubFor(get(urlMatching(".*")).willReturn(aResponse().withBody("foo=bar")));
      Properties props = new Properties();
      props.setProperty("orchestrator.configUrl", wireMockServer.baseUrl() + "/jenkins/orchestrator.properties");

      Configuration config = Configuration.create(props);
      assertThat(config.getString("foo")).isEqualTo("bar");
    } finally {
      wireMockServer.stop();
    }
  }

  @Test
  void loadRemotePropertiesFile_fail_if_not_found() {
    WireMockServer wireMockServer = new WireMockServer(options().dynamicPort());
    try {
      wireMockServer.start();
      wireMockServer.stubFor(get(urlMatching(".*")).willReturn(notFound()));
      Properties props = new Properties();
      props.setProperty("orchestrator.configUrl", wireMockServer.baseUrl());

      assertThatThrownBy(() -> Configuration.create(props)).isInstanceOf(IllegalStateException.class);
    } finally {
      wireMockServer.stop();
    }
  }

  /**
   * ORCH-88
   */
  @Test
  void interpolate_config_url() {
    URL url = getClass().getResource("/com/sonar/orchestrator/config/ConfigurationTest/sample.properties");
    Properties props = new Properties();
    String value = Strings.CS.replace(url.toString(), "sample.properties", "${filename}");
    props.setProperty("orchestrator.configUrl", value);
    props.setProperty("filename", "sample.properties");

    Configuration config = Configuration.create(props);
    assertThat(config.getString("yes")).isEqualTo("true");
    assertThat(config.getString("loadedFromFile")).isEqualTo("true");
  }

  @Test
  void should_support_h2_as_embedded_database() {
    Properties props = new Properties();
    props.setProperty("sonar.jdbc.dialect", "embedded");

    Configuration config = Configuration.create(props);
    assertThat(config.getString("sonar.jdbc.dialect")).isEqualTo("h2");
  }

  @ParameterizedTest
  @ValueSource(strings = {"SONAR_USER_HOME", "ORCHESTRATOR_HOME", "orchestrator.home"})
  void configure_orchestrator_home_with_deprecated_property(String property, @TempDir Path dir) {
    Configuration underTest = Configuration.builder().setProperty(property, dir).build();

    assertThat(underTest.fileSystem().getOrchestratorHome()).isEqualTo(dir);
  }

}
