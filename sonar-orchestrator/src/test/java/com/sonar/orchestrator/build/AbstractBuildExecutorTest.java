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
package com.sonar.orchestrator.build;

import com.google.common.collect.ImmutableMap;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.test.MockHttpServerInterceptor;
import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

public class AbstractBuildExecutorTest {

  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  @Test
  public void shouldNotAppendCoverageArgumentToOptsByDefault() {
    Map<String, String> env = ImmutableMap.of();

    Configuration config = Configuration.create(new HashMap<String, String>());

    AbstractBuildExecutor.appendCoverageArgumentToOpts(env, config, "SONAR_OPTS");
  }

  @Test
  public void shouldAppendCoverageArgumentToOpts() {
    Map<String, String> env = new HashMap<>();
    env.put("SONAR_OPTS", "foo");

    Configuration config = Configuration.builder()
      .addProperties(singletonMap("orchestrator.computeCoverage", "true"))
      .addEnvVariables()
      .build();

    AbstractBuildExecutor.appendCoverageArgumentToOpts(env, config, "SONAR_OPTS");

    assertThat(env.get("SONAR_OPTS")).startsWith("foo -javaagent:");
  }

  @Test
  public void shouldCreateEnvironmentVariableIfNeeded() {
    Map<String, String> env = new HashMap<>();

    Configuration config = Configuration.builder()
      .addProperties(singletonMap("orchestrator.computeCoverage", "true"))
      .addEnvVariables()
      .build();

    AbstractBuildExecutor.appendCoverageArgumentToOpts(env, config, "SONAR_OPTS");

    assertThat(env.get("SONAR_OPTS")).startsWith("-javaagent:");
  }
}
