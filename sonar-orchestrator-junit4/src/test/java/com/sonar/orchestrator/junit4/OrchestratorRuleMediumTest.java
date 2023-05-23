/*
 * Orchestrator - JUnit 4
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
package com.sonar.orchestrator.junit4;

import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class OrchestratorRuleMediumTest {

  @ClassRule
  public static OrchestratorRule ORCHESTRATOR = OrchestratorRule.builderEnv()
    .setSonarVersion("LATEST_RELEASE")
    .build();

  @Test
  public void serverShouldBeStarted() {
    assertTrue(ORCHESTRATOR.getServer().newHttpCall("/api/server/version").execute().isSuccessful());
  }

}