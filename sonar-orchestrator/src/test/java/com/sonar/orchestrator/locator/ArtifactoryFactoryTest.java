/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package com.sonar.orchestrator.locator;

import com.sonar.orchestrator.config.Configuration;
import java.util.HashMap;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ArtifactoryFactoryTest {

  private static final String DEFAULT_REPOSITORY = "https://repox.jfrog.io/repox";
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

  private static Configuration getConfiguration(String url, String apiKey, String accessToken) {
    HashMap<String, String> properties = new HashMap<>();
    properties.put("orchestrator.artifactory.url", url);
    properties.put("orchestrator.artifactory.apiKey", apiKey);
    properties.put("orchestrator.artifactory.accessToken", accessToken);
    return Configuration.builder()
      .addProperties(properties)
      .addEnvVariables()
      .build();
  }

  private static Stream<Arguments> params() {
    return Stream.of(
      Arguments.of(new Parameters(null, DEFAULT_REPOSITORY, API_KEY, API_KEY, ACCESS_TOKEN, ACCESS_TOKEN, DefaultArtifactory.class)),
      Arguments.of(new Parameters("", DEFAULT_REPOSITORY, API_KEY, API_KEY, ACCESS_TOKEN, ACCESS_TOKEN, DefaultArtifactory.class)),
      Arguments.of(new Parameters(DEFAULT_REPOSITORY, DEFAULT_REPOSITORY, API_KEY, API_KEY, ACCESS_TOKEN, ACCESS_TOKEN, DefaultArtifactory.class)),
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