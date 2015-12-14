/*
 * Orchestrator
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package com.sonar.orchestrator.util;

import com.sonar.orchestrator.util.NetworkUtils.RandomPortFinder;
import com.sonar.orchestrator.util.NetworkUtils.TravisIncrementalPortFinder;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class NetworkUtilsTest {

  @Test
  public void shouldGetAvailablePortWithoutLockingHost() {
    for (int i = 0; i < 1000; i++) {
      /*
       * The Well Known Ports are those from 0 through 1023.
       * DCCP Well Known ports SHOULD NOT be used without IANA registration.
       */
      assertThat(NetworkUtils.getNextAvailablePort()).isGreaterThan(1023);
    }
  }

  @Test
  public void shouldGetRandomPort() {
    assertThat(NetworkUtils.getNextAvailablePort()).isNotEqualTo(NetworkUtils.getNextAvailablePort());
  }

  @Test
  public void shouldNotBeValidPorts() {
    assertThat(RandomPortFinder.isValidPort(0)).isFalse();// <=1023
    assertThat(RandomPortFinder.isValidPort(50)).isFalse();// <=1023
    assertThat(RandomPortFinder.isValidPort(1023)).isFalse();// <=1023
    assertThat(RandomPortFinder.isValidPort(2049)).isFalse();// NFS
    assertThat(RandomPortFinder.isValidPort(4045)).isFalse();// lockd
  }

  @Test
  public void shouldBeValidPorts() {
    assertThat(RandomPortFinder.isValidPort(1059)).isTrue();
  }

  @Test(expected = IllegalStateException.class)
  public void shouldFailWhenNoValidPortIsAvailable() throws IOException {
    RandomPortFinder randomPortFinder = spy(new RandomPortFinder());
    doReturn(0).when(randomPortFinder).getRandomUnusedPort();

    randomPortFinder.getNextAvailablePort();
  }

  @Test(expected = IllegalStateException.class)
  public void shouldFailWhenItsNotPossibleToOpenASocket() throws IOException {
    RandomPortFinder randomPortFinder = spy(new RandomPortFinder());
    doThrow(new IOException("Not possible")).when(randomPortFinder).getRandomUnusedPort();

    randomPortFinder.getNextAvailablePort();
  }

  @Test
  public void shouldIncrementPortOnTravis() {
    TravisIncrementalPortFinder travisIncrementalPortFinder = new TravisIncrementalPortFinder();

    assertThat(travisIncrementalPortFinder.getNextAvailablePort()).isEqualTo(20000);
    assertThat(travisIncrementalPortFinder.getNextAvailablePort()).isEqualTo(20001);
    assertThat(travisIncrementalPortFinder.getNextAvailablePort()).isEqualTo(20002);
    assertThat(travisIncrementalPortFinder.getNextAvailablePort()).isEqualTo(20003);
  }
}
