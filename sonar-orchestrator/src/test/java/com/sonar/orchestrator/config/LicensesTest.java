/*
 * Orchestrator
 * Copyright (C) 2011-2018 SonarSource SA
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
package com.sonar.orchestrator.config;

import com.sonar.orchestrator.test.MockHttpServerInterceptor;
import com.sonar.orchestrator.util.NetworkUtils;
import java.net.InetAddress;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class LicensesTest {

  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private Licenses underTest;

  @Before
  public void setUp() {
    underTest = new Licenses("http://localhost:" + httpServer.getPort() + "/");
  }

  @Test
  public void download_dev_license() {
    httpServer.setMockResponseData("abcd1234");

    Licenses licenses = new Licenses("http://localhost:" + httpServer.getPort() + "/");
    assertThat(licenses.getLicense()).isEqualTo("abcd1234");

    // use cache
    assertThat(licenses.getLicense()).isEqualTo("abcd1234");
  }

  @Test
  public void failIfUnknownPluginOrInvalidLicense() {
    httpServer.setMockResponseStatus(404);

    thrown.expect(IllegalStateException.class);
    underTest.getLicense();
  }

  @Test
  public void failIfConnectionFailure() {
    int freePort = NetworkUtils.getNextAvailablePort(InetAddress.getLoopbackAddress());

    thrown.expect(IllegalStateException.class);

    Licenses licenses = new Licenses("http://localhost:" + freePort + "/");
    licenses.getLicense();
  }

}
