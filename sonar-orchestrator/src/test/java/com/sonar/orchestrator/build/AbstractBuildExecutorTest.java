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
package com.sonar.orchestrator.build;

import com.google.common.collect.ImmutableMap;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.junit.PropertyFilterRunner;
import com.sonar.orchestrator.test.MockHttpServerInterceptor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(PropertyFilterRunner.class)
public class AbstractBuildExecutorTest {

  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  @Test
  public void shouldNotAppendCoverageArgumentToOptsByDefault() {
    Map<String, String> env = ImmutableMap.of();

    AbstractBuildExecutor.appendCoverageArgumentToOpts(env, Configuration.create(), "SONAR_OPTS");
  }

  @Test
  public void shouldAppendCoverageArgumentToOpts() {
    Map<String, String> env = new HashMap<>();
    env.put("SONAR_OPTS", "foo");

    Configuration config = Configuration.builder()
        .setProperty("orchestrator.computeCoverage", "true")
        .setProperty("maven.nexusUrl", "http://localhost:" + httpServer.getPort() + "/")
        .setProperty("maven.nexusRepository", "ss-repo")
        .build();
    AbstractBuildExecutor.appendCoverageArgumentToOpts(env, config, "SONAR_OPTS");

    assertThat(env.get("SONAR_OPTS")).matches("foo -javaagent:.*");
  }

  @Test
  public void shouldCreateEnvironmentVariableIfNeeded() {
    Map<String, String> env = new HashMap<>();

    Configuration config = Configuration.builder()
        .setProperty("orchestrator.computeCoverage", "true")
        .setProperty("maven.nexusUrl", "http://localhost:" + httpServer.getPort() + "/")
        .setProperty("maven.nexusRepository", "ss-repo")
        .build();
    AbstractBuildExecutor.appendCoverageArgumentToOpts(env, config, "SONAR_OPTS");

    assertThat(env.get("SONAR_OPTS")).matches("-javaagent:.*");
  }
}
