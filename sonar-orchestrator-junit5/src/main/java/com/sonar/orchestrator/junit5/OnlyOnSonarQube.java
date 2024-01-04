/*
 * Orchestrator - JUnit 5
 * Copyright (C) 2011-2024 SonarSource SA
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allow to skip execution of tests, based on SonarQube runtime version. Example:
 * <pre>{@code
 * public class MyTest {
 *
 *   @RegisterExtension
 *   static OrchestratorExtension ORCHESTRATOR = OrchestratorExtension.builderEnv()
 *     .setSonarVersion("LATEST_RELEASE")
 *     .build();
 *
 *   @Test
 *   @OnlyOnSonarQube(from = "9.2")
 *   void shouldRaiseIssuesOnACloudFormationProject() {
 *     // ...
 *   }
 * }
 * }</pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OnlyOnSonarQube {
  /**
   * min version of SonarQube to run the test (inclusive)
   */
  String from();

}
