/*
 * Orchestrator Locators
 * Copyright (C) SonarSource Sàrl
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
package com.sonar.orchestrator.locator;

import com.sonar.orchestrator.config.Configuration;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ArtifactoryFactoryTest {

  @TempDir
  Path homeDir;

  private static final String DEFAULT_REPOSITORY = "https://repox.jfrog.io/artifactory";
  private static final String INTERNAL_REPOSITORY = "https://repox-internal.dev.sonar.build/artifactory";
  private static final String SOME_MAVEN_REPOSITORY = "https://localhost:9000/maven-repo";
  private static final String ACCESS_TOKEN = "access_token";
  private static final String API_KEY = "api_key";

  @ParameterizedTest
  @MethodSource("params")
  void createArtifactory_shouldInstantiateCorrectArtifactory(Parameters parameters) {
    Artifactory result = ArtifactoryFactory.createArtifactory(getConfiguration(parameters.getInputUrl(), parameters.getInputApiKey(), parameters.getInputAccessToken()));

    Assertions.assertEquals(parameters.getExpectedUrl(), result.getBaseUrl());
    Assertions.assertEquals(parameters.getExpectedApiKey(), result.getApiKey());
    Assertions.assertEquals(parameters.getExpectedAccessToken(), result.getAccessToken());
    assertThat(result).isInstanceOf(parameters.getExpectedClass());
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "https://repox.jfrog.io",
    "https://repox-internal.dev.sonar.build",
    "https://repox-internal.sonar.build",
    "https://repox-internal.sonar.build/artifactory",
    "https://repox.sonar.build",
    "https://repox.sonar.build/artifactory",
    "https://REPOX-INTERNAL.SONAR.BUILD:443/artifactory"
  })
  void authenticated_hosts_preserve_credentials(String url) {
    Artifactory result = ArtifactoryFactory.createArtifactory(getConfiguration(url, API_KEY, ACCESS_TOKEN));

    assertThat(result).isInstanceOf(DefaultArtifactory.class);
    assertThat(result.getBaseUrl()).isEqualTo(url);
    assertThat(result.getAccessToken()).isEqualTo(ACCESS_TOKEN);
    assertThat(result.getApiKey()).isEqualTo(API_KEY);
  }

  @ParameterizedTest
  @ValueSource(strings = {"orchestrator.artifactory.accessToken", "ARTIFACTORY_ACCESS_TOKEN"})
  void production_edge_uses_configured_access_token(String tokenKey) {
    Configuration configuration = Configuration.builder()
      .setProperty("orchestrator.home", homeDir)
      .setProperty("orchestrator.artifactory.url", "https://repox-internal.sonar.build")
      .setProperty(tokenKey, ACCESS_TOKEN)
      .build();

    Artifactory result = ArtifactoryFactory.createArtifactory(configuration);

    assertThat(result).isInstanceOf(DefaultArtifactory.class);
    assertThat(result.getAccessToken()).isEqualTo(ACCESS_TOKEN);
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "https://repox.jfrog.io.example.com/artifactory",
    "https://prefix-repox.jfrog.io/artifactory",
    "https://repox-internal.dev.sonar.build.example.com/artifactory",
    "https://prefix-repox-internal.dev.sonar.build/artifactory",
    "https://repox-internal.sonar.build.example.com/artifactory",
    "https://prefix-repox-internal.sonar.build/artifactory",
    "https://repox.sonar.build.example.com/artifactory",
    "https://prefix-repox.sonar.build/artifactory",
    "https://repox-internal.sonar.build@example.com/artifactory",
    "https://example.com/repox-internal.sonar.build"
  })
  void unrelated_hosts_do_not_receive_credentials(String url) {
    Artifactory result = ArtifactoryFactory.createArtifactory(getConfiguration(url, API_KEY, ACCESS_TOKEN));

    assertThat(result).isInstanceOf(MavenArtifactory.class);
    assertThat(result.getAccessToken()).isNull();
    assertThat(result.getApiKey()).isNull();
  }

  private Configuration getConfiguration(String url, String apiKey, String accessToken) {
    HashMap<String, String> properties = new HashMap<>();
    properties.put("orchestrator.artifactory.url", url);
    properties.put("orchestrator.artifactory.apiKey", apiKey);
    properties.put("orchestrator.artifactory.accessToken", accessToken);
    return Configuration.builder()
      .addProperties(properties)
      .setProperty("orchestrator.home", homeDir)
      .build();
  }

  private static Stream<Arguments> params() {
    return Stream.of(
      Arguments.of(new Parameters(null, DEFAULT_REPOSITORY, API_KEY, API_KEY, ACCESS_TOKEN, ACCESS_TOKEN, DefaultArtifactory.class)),
      Arguments.of(new Parameters("", DEFAULT_REPOSITORY, API_KEY, API_KEY, ACCESS_TOKEN, ACCESS_TOKEN, DefaultArtifactory.class)),
      Arguments.of(new Parameters(DEFAULT_REPOSITORY, DEFAULT_REPOSITORY, API_KEY, API_KEY, ACCESS_TOKEN, ACCESS_TOKEN, DefaultArtifactory.class)),
      Arguments.of(new Parameters(INTERNAL_REPOSITORY, INTERNAL_REPOSITORY, API_KEY, API_KEY, ACCESS_TOKEN, ACCESS_TOKEN, DefaultArtifactory.class)),
      Arguments.of(new Parameters(SOME_MAVEN_REPOSITORY, SOME_MAVEN_REPOSITORY, API_KEY, null, ACCESS_TOKEN, null, MavenArtifactory.class))
    );
  }

  static class Parameters {
    private final String inputUrl;
    private final String expectedUrl;
    private final String inputApiKey;
    private final String expectedApiKey;
    private final String inputAccessToken;
    private final String expectedAccessToken;
    private final Class<?> expectedClass;

    public Parameters(@Nullable String inputUrl, String expectedUrl, String inputApiKey, @Nullable String expectedApiKey, String inputAccessToken,
      @Nullable String expectedAccessToken, Class<?> expectedClass) {
      this.inputUrl = inputUrl;
      this.expectedUrl = expectedUrl;
      this.inputApiKey = inputApiKey;
      this.expectedApiKey = expectedApiKey;
      this.inputAccessToken = inputAccessToken;
      this.expectedAccessToken = expectedAccessToken;
      this.expectedClass = expectedClass;
    }

    public String getInputUrl() {
      return inputUrl;
    }

    public String getExpectedUrl() {
      return expectedUrl;
    }

    public String getInputApiKey() {
      return inputApiKey;
    }

    public String getExpectedApiKey() {
      return expectedApiKey;
    }

    public String getInputAccessToken() {
      return inputAccessToken;
    }

    public String getExpectedAccessToken() {
      return expectedAccessToken;
    }

    public Class<?> getExpectedClass() {
      return expectedClass;
    }
  }
}