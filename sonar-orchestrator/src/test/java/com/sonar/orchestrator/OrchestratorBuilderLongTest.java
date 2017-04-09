/*
 * Orchestrator
 * Copyright (C) 2011-2017 SonarSource SA
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
package com.sonar.orchestrator;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.version.Version;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OrchestratorBuilderLongTest {
  @Test
  public void start_lts_version_with_default_settings() throws Exception {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion("5.6")
      .build();

    try {
      orchestrator.start();
      verifyVersion(orchestrator, "5.6");
      verifyWebContext(orchestrator, "");
    } finally {
      orchestrator.stop();
    }
  }

  @Test
  public void start_6_3_with_web_context() throws Exception {
    Orchestrator orchestrator = new OrchestratorBuilder(Configuration.create())
      .setSonarVersion("6.3")
      .setServerProperty("sonar.web.context", "/sonarqube")
      .build();

    try {
      orchestrator.start();
      verifyVersion(orchestrator, "6.3");
      verifyWebContext(orchestrator, "/sonarqube");
    } finally {
      orchestrator.stop();
    }
  }

  private static void verifyWebContext(Orchestrator orchestrator, String expectedWebContext) throws MalformedURLException {
    URL baseUrl = new URL(orchestrator.getServer().getUrl());
    assertThat(baseUrl.getPath()).isEqualTo(expectedWebContext);
  }

  private static void verifyVersion(Orchestrator orchestrator, String expectedVersion) {
    assertThat(orchestrator.getServer().version()).isEqualTo(Version.create(expectedVersion));
  }
}
