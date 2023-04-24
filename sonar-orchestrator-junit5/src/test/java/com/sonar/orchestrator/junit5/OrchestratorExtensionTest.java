/*
 * Orchestrator - JUnit 5
 * Copyright (C) 2011-2023 SonarSource SA
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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.*;

class OrchestratorExtensionTest {

  @RegisterExtension
  static OrchestratorExtension ORCHESTRATOR = OrchestratorExtension.builderEnv()
    .setSonarVersion("LATEST_RELEASE")
    .build();
  private static boolean includedDidRun;
  private static boolean excludedDidRun;

  @AfterAll
  static void checkIncludedTests() {
    assertTrue(includedDidRun);
    assertFalse(excludedDidRun);
  }

  @Test
  void serverShouldBeStarted() {
    assertTrue(ORCHESTRATOR.getServer().newHttpCall("/api/server/version").execute().isSuccessful());
  }

  @OnlyOnSonarQube(from="1.0")
  @Test
  void testShouldBeIncluded() {
    includedDidRun = true;
  }

  @OnlyOnSonarQube(from="999.0")
  @Test
  void testShouldBeSkipped() {
    excludedDidRun = true;
  }

}