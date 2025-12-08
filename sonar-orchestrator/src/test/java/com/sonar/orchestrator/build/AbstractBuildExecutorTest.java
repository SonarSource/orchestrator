/*
 * Orchestrator
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
package com.sonar.orchestrator.build;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.locator.Locators;
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
    Map<String, String> env = Map.of();

    Configuration config = Configuration.create(new HashMap<String, String>());

    AbstractBuildExecutor.appendCoverageArgumentToOpts(env, config, null, "SONAR_OPTS");

    assertThat(env).isEmpty();
  }

  @Test
  public void shouldAppendCoverageArgumentToOpts() {
    Map<String, String> env = new HashMap<>();
    env.put("SONAR_OPTS", "foo");

    Configuration config = Configuration.builder()
      .addProperties(singletonMap("orchestrator.computeCoverage", "true"))
      .addEnvVariables()
      .build();
    Locators locators = new Locators(config);

    AbstractBuildExecutor.appendCoverageArgumentToOpts(env, config, locators, "SONAR_OPTS");

    assertThat(env.get("SONAR_OPTS")).startsWith("foo -javaagent:");
  }

  @Test
  public void shouldCreateEnvironmentVariableIfNeeded() {
    Map<String, String> env = new HashMap<>();

    Configuration config = Configuration.builder()
      .addProperties(singletonMap("orchestrator.computeCoverage", "true"))
      .addEnvVariables()
      .build();
    Locators locators = new Locators(config);

    AbstractBuildExecutor.appendCoverageArgumentToOpts(env, config, locators, "SONAR_OPTS");

    assertThat(env.get("SONAR_OPTS")).startsWith("-javaagent:");
  }
}
