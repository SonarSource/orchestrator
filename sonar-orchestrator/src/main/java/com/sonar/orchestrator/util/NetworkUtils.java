/*
 * Orchestrator
 * Copyright (C) 2011-2019 SonarSource SA
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
package com.sonar.orchestrator.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

public final class NetworkUtils {

  private static final Set<Integer> ALREADY_ALLOCATED = new HashSet<>();
  private static final int MAX_TRIES = 50;

  // Firefox blocks some reserved ports : https://developer.mozilla.org/en-US/docs/Mozilla/Mozilla_Port_Blocking
  private static final Set<Integer> HTTP_BLOCKED_PORTS = unmodifiableSet(new HashSet<>(asList(2_049, 4_045, 6_000)));

  private NetworkUtils() {
    // prevent instantiation
  }

  public static InetAddress getLocalhost() {
    try {
      return InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      throw new IllegalStateException("Fail to get localhost IP", e);
    }
  }

  /**
   * Same as {@link InetAddress#getByName(String)} but throws {@link IllegalStateException}
   * instead of {@link UnknownHostException}
   */
  public static InetAddress getInetAddressByName(String name) {
    try {
      return InetAddress.getByName(name);
    } catch (UnknownHostException e) {
      throw new IllegalStateException(format("Fail to resolve address named [%s]", name), e);
    }
  }

  public static int getNextAvailablePort(InetAddress address) {
    return getNextAvailablePort(address, PortAllocator.INSTANCE);
  }

  static int getNextAvailablePort(InetAddress address, PortAllocator portAllocator) {
    for (int i = 0; i < MAX_TRIES; i++) {
      int port = portAllocator.getAvailable(address);
      if (isValidPort(port)) {
        ALREADY_ALLOCATED.add(port);
        return port;
      }
    }
    throw new IllegalStateException("Fail to find an available port on " + address);
  }

  private static boolean isValidPort(int port) {
    return port > 1023 && !HTTP_BLOCKED_PORTS.contains(port) && !ALREADY_ALLOCATED.contains(port);
  }

  static class PortAllocator {
    private static final PortAllocator INSTANCE = new PortAllocator();

    int getAvailable(InetAddress address) {
      try (ServerSocket socket = new ServerSocket(0, 50, address)) {
        return socket.getLocalPort();
      } catch (IOException e) {
        throw new IllegalStateException("Fail to find an available port on " + address, e);
      }
    }
  }
}
