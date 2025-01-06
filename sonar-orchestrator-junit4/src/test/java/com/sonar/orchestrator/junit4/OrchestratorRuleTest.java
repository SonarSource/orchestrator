/*
 * Orchestrator - JUnit 4
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
package com.sonar.orchestrator.junit4;

import com.sonar.orchestrator.Orchestrator;
import org.assertj.core.api.Assertions;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

public class OrchestratorRuleTest {

  @Test
  public void proxyMethodsOnOrchestrator() {
    Orchestrator proxied = mock(Orchestrator.class);
    OrchestratorRule proxy = new OrchestratorRule(proxied);

    proxy.getServer();
    verify(proxied).getServer();

    reset(proxied);

    proxy.install();
    verify(proxied).install();

    reset(proxied);

    proxy.activateLicense();
    verify(proxied).activateLicense();

    reset(proxied);

    proxy.clearLicense();
    verify(proxied).clearLicense();

    reset(proxied);

    proxy.restartServer();
    verify(proxied).restartServer();

    reset(proxied);

    proxy.getDatabase();
    verify(proxied).getDatabase();

    reset(proxied);

    proxy.getConfiguration();
    verify(proxied).getConfiguration();

    reset(proxied);

    proxy.getFileLocationOfShared("foo");
    verify(proxied).getFileLocationOfShared("foo");

    reset(proxied);

    proxy.getDistribution();
    verify(proxied).getDistribution();

    reset(proxied);

    proxy.executeBuild(null);
    verify(proxied).executeBuild(null, true);

    reset(proxied);

    proxy.executeBuildQuietly(null);
    verify(proxied).executeBuildQuietly(null, true);

    reset(proxied);

    proxy.getDefaultAdminToken();
    verify(proxied).getDefaultAdminToken();

    reset(proxied);

    proxy.executeBuilds();
    verify(proxied).executeBuilds();

    reset(proxied);

    assertThat(proxy.getOrchestrator()).isEqualTo(proxied);
  }

}
