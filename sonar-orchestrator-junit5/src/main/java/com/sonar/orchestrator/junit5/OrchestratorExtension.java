/*
 * Orchestrator - JUnit 5
 * Copyright (C) 2011-2025 SonarSource SA
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
package com.sonar.orchestrator.junit5;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.container.SonarDistribution;
import com.sonar.orchestrator.server.StartupLogWatcher;
import com.sonar.orchestrator.version.Version;
import java.lang.reflect.AnnotatedElement;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

/**
 * To be used as a JUnit 5 {@link org.junit.jupiter.api.extension.Extension}. For example:
 * <pre>{@code
 * public class MyTest {
 *
 *   @RegisterExtension
 *   static OrchestratorExtension ORCHESTRATOR = OrchestratorExtension.builderEnv()
 *     .setSonarVersion("LATEST_RELEASE")
 *     .addPlugin(MavenLocation.of("org.sonarsource.html", "sonar-html-plugin", "DEV"))
 *     .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-jsp.xml"))
 *     .build();
 * }
 * }</pre>
 */
public class OrchestratorExtension extends Orchestrator implements BeforeAllCallback, AfterAllCallback, ExecutionCondition {

  private static final ConditionEvaluationResult ENABLED = ConditionEvaluationResult.enabled("@OnlyOnSonarQube is not present");
  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(Orchestrator.class);

  OrchestratorExtension(Configuration config, SonarDistribution distribution, @Nullable StartupLogWatcher startupLogWatcher) {
    super(config, distribution, startupLogWatcher);
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    // This is to avoid multiple starts when using nested tests
    // See https://github.com/junit-team/junit5/issues/2421
    if (context.getStore(NAMESPACE).getOrComputeIfAbsent(AtomicInteger.class).getAndIncrement() == 0) {
      start();
    }
  }

  @Override
  public void afterAll(ExtensionContext context) {
    if (context.getStore(NAMESPACE).getOrComputeIfAbsent(AtomicInteger.class).decrementAndGet() == 0) {
      stop();
    }
  }

  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    AnnotatedElement element = context.getElement().orElse(null);
    return AnnotationUtils.findAnnotation(element, OnlyOnSonarQube.class).map(this::toResult).orElse(ENABLED);
  }

  private ConditionEvaluationResult toResult(OnlyOnSonarQube annotation) {
    String min = annotation.from();
    if (this.getServer().version().compareTo(Version.create(min)) < 0) {
      String reason = "SonarQube version (" + this.getServer().version() + ") is lower than minimum requested (" + min + ")";
      return ConditionEvaluationResult.disabled(reason);
    }
    String reason = "SonarQube version (" + this.getServer().version() + ") meets requirements";
    return ConditionEvaluationResult.enabled(reason);
  }

  public static OrchestratorExtensionBuilder builderEnv() {
    return new OrchestratorExtensionBuilder(Configuration.createEnv());
  }

  public static OrchestratorExtensionBuilder builder(Configuration config) {
    return new OrchestratorExtensionBuilder(config);
  }

}
