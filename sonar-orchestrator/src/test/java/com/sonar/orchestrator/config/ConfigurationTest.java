/*
 * Orchestrator
 * Copyright (C) 2011-2020 SonarSource SA
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

import com.google.common.collect.ImmutableMap;
import com.sonar.orchestrator.PropertyAndEnvTest;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.test.MockHttpServer;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static com.sonar.orchestrator.TestModules.setEnv;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class ConfigurationTest extends PropertyAndEnvTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void getString() {
    Properties props = new Properties();
    props.setProperty("foo", "bar");
    Configuration config = Configuration.create(props);

    assertThat(config.getString("foo")).isEqualTo("bar");
    assertThat(config.getString("xxx")).isNull();
    assertThat(config.getString("xxx", "default")).isEqualTo("default");
  }

  @Test
  public void getInt() {
    Properties props = new Properties();
    props.setProperty("one", "1");
    Configuration config = Configuration.create(props);

    assertThat(config.getInt("one", -1)).isEqualTo(1);
    assertThat(config.getInt("two", -1)).isEqualTo(-1);
  }

  @Test
  public void asMap() {
    Properties props = new Properties();
    props.setProperty("foo", "1");
    props.setProperty("bar", "2");

    Configuration config = Configuration.create(props);

    assertThat(config.asMap()).containsOnly(entry("foo", "1"), entry("bar", "2"));
  }

  @Test
  public void asMap_immutable() {
    thrown.expect(UnsupportedOperationException.class);

    Properties props = new Properties();
    props.setProperty("foo", "bar");
    Configuration config = Configuration.create(props);

    config.asMap().put("new", "property");
  }

  @Test
  public void createEnv() {
    Configuration config = Configuration.createEnv();
    assertThat(config.getString("java.io.tmpdir")).isNotNull();
  }

  @Test
  public void interpolateProperties() {
    Properties props = new Properties();
    props.setProperty("where", "FR");
    props.setProperty("codeStory", "devoxx${where}");

    Configuration config = Configuration.create(props);
    assertThat(config.getString("codeStory")).isEqualTo("devoxxFR");
  }

  @Test
  public void loadPropertiesFile() {
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
  public void loadPropertiesFileFromHomeByDefault() throws IOException {
    URL url = getClass().getResource("/com/sonar/orchestrator/config/ConfigurationTest/sample.properties");
    File homeDir = temp.newFolder();
    FileUtils.copyURLToFile(url, new File(homeDir, "orchestrator.properties"));
    Properties props = new Properties();
    props.setProperty("foo", "bar");
    props.setProperty("orchestrator.home", homeDir.toString());

    Configuration config = Configuration.create(props);
    assertThat(config.getString("foo")).isEqualTo("bar");
    assertThat(config.getString("yes")).isEqualTo("true");
    assertThat(config.getString("loadedFromFile")).isEqualTo("true");
  }

  @Test
  public void shouldOverridePropertiesFile() {
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
  public void getFileLocationOfShared() throws URISyntaxException {
    URL url = getClass().getResource("/com/sonar/orchestrator/config/ConfigurationTest/sample.properties");
    Properties props = new Properties();
    props.setProperty("orchestrator.configUrl", url.toString());
    props.setProperty("orchestrator.it_sources", FilenameUtils.getFullPath(url.toURI().getPath()));
    Configuration config = Configuration.create(props);

    FileLocation location = config.getFileLocationOfShared("sample.properties");
    assertThat(location.getFile()).isFile();
    assertThat(location.getFile()).exists();
    assertThat(location.getFile().getName()).isEqualTo("sample.properties");
  }

  @Test
  public void getFileLocationOfSharedApplyPriority() throws URISyntaxException {
    URL url = getClass().getResource("/com/sonar/orchestrator/config/ConfigurationTest/sample.properties");
    Properties props = new Properties();
    props.setProperty("orchestrator.configUrl", url.toString());
    Map<String, String> envVariables = new HashMap<>(System.getenv());
    envVariables.put("SONAR_IT_SOURCES", FilenameUtils.getFullPath(url.toURI().getPath()));
    setEnv(ImmutableMap.copyOf(envVariables));
    props.setProperty("orchestrator.it_sources", FilenameUtils.getFullPath(url.toURI().getPath()));
    Configuration config = Configuration.create(props);

    FileLocation location = config.getFileLocationOfShared("sample.properties");
    assertThat(location.getFile().exists()).isTrue();
  }

  @Test
  public void getFileLocationOfShared_bad_place() {
    thrown.expect(IllegalStateException.class);

    URL url = getClass().getResource("/com/sonar/orchestrator/config/ConfigurationTest/sample.properties");
    Properties props = new Properties();
    props.setProperty("orchestrator.configUrl", url.toString());
    props.setProperty("orchestrator.it_sources", "/com/sonar/orchestrator/config/ConfigurationTest");
    Configuration config = Configuration.create(props);

    config.getFileLocationOfShared("/bad/path");
  }

  @Test
  public void getFileLocationOfShared_not_a_directory() {
    thrown.expect(IllegalStateException.class);

    URL url = getClass().getResource("/com/sonar/orchestrator/config/ConfigurationTest/sample.properties");
    Properties props = new Properties();
    props.setProperty("orchestrator.configUrl", url.toString());
    props.setProperty("orchestrator.it_sources", "/com/sonar/orchestrator/config/ConfigurationTest/sample.properties");
    Configuration config = Configuration.create(props);

    config.getFileLocationOfShared(".");
  }

  @Test
  public void loadRemotePropertiesFile() throws Exception {
    MockHttpServer server = new MockHttpServer();
    server.start();
    server.setMockResponseData("foo=bar");
    try {
      Properties props = new Properties();
      props.setProperty("orchestrator.configUrl", "http://localhost:" + server.getPort() + "/jenkins/orchestrator.properties");

      Configuration config = Configuration.create(props);
      assertThat(config.getString("foo")).isEqualTo("bar");
    } finally {
      server.stop();
    }
  }

  @Test(expected = IllegalStateException.class)
  public void loadRemotePropertiesFile_fail_if_not_found() throws Exception {
    MockHttpServer server = new MockHttpServer();
    server.start();
    server.setMockResponseStatus(404);
    try {
      Properties props = new Properties();
      props.setProperty("orchestrator.configUrl", "http://localhost:" + server.getPort());

      Configuration.create(props);
    } finally {
      server.stop();
    }
  }

  /**
   * ORCH-88
   */
  @Test
  public void interpolate_config_url() {
    URL url = getClass().getResource("/com/sonar/orchestrator/config/ConfigurationTest/sample.properties");
    Properties props = new Properties();
    String value = StringUtils.replace(url.toString(), "sample.properties", "${filename}");
    props.setProperty("orchestrator.configUrl", value);
    props.setProperty("filename", "sample.properties");

    Configuration config = Configuration.create(props);
    assertThat(config.getString("yes")).isEqualTo("true");
    assertThat(config.getString("loadedFromFile")).isEqualTo("true");
  }

  @Test
  public void should_support_h2_as_embedded_database() {
    Properties props = new Properties();
    props.setProperty("sonar.jdbc.dialect", "embedded");

    Configuration config = Configuration.create(props);
    assertThat(config.getString("sonar.jdbc.dialect")).isEqualTo("h2");
  }

  @Test
  public void configure_orchestrator_home_with_deprecated_property() throws Exception {
    String property = "SONAR_USER_HOME";
    testOrchestratorHome(property);
  }

  @Test
  public void configure_orchestrator_home() throws Exception {
    testOrchestratorHome("orchestrator.home");
  }

  @Test
  public void configure_orchestrator_home_with_env_variable() throws Exception {
    testOrchestratorHome("ORCHESTRATOR_HOME");
  }

  private void testOrchestratorHome(String property) throws IOException {
    File dir = temp.newFolder();

    Configuration underTest = Configuration.builder().setProperty(property, dir.getCanonicalPath()).build();

    assertThat(underTest.fileSystem().getOrchestratorHome().getCanonicalPath()).isEqualTo(dir.getCanonicalPath());
  }

}
