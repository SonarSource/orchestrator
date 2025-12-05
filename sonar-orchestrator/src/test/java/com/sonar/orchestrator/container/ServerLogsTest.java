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
package com.sonar.orchestrator.container;

import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.build.version.Version;
import java.io.File;
import okhttp3.HttpUrl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ServerLogsTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private File home;
  private Locators locators = mock(Locators.class);
  private Server server;

  @Before
  public void setUp() throws Exception {
    home = temporaryFolder.newFolder();
    server = new Server(locators, home, Edition.COMMUNITY, Version.create("7.3.0.1000"), HttpUrl.parse("http://localhost"), 9001, null);
  }

  @Test
  public void getLogs_returns_sonar_log_file_in_logs_dir_of_home_directory() {
    assertThat(server.getLogs()).isEqualTo(new File(new File(home, "logs"), "sonar.log"));
  }

  @Test
  public void getAppLogs_returns_sonar_log_file_in_logs_dir_of_home_directory() {
    assertThat(server.getAppLogs()).isEqualTo(new File(new File(home, "logs"), "sonar.log"));
  }

  @Test
  public void getWebLogs_returns_web_log_file_in_logs_dir_of_home_directory() {
    assertThat(server.getWebLogs()).isEqualTo(new File(new File(home, "logs"), "web.log"));
  }

  @Test
  public void getCeLogs_returns_web_log_file_in_logs_dir_of_home_directory() {
    assertThat(server.getCeLogs()).isEqualTo(new File(new File(home, "logs"), "ce.log"));
  }

  @Test
  public void getEsLogs_returns_web_log_file_in_logs_dir_of_home_directory() {
    assertThat(server.getEsLogs()).isEqualTo(new File(new File(home, "logs"), "es.log"));
  }
}
