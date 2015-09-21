/*
 * Orchestrator
 * Copyright (C) 2011 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.sonar.orchestrator.util;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;

public final class NetworkUtils {
  private static final RandomPortFinder RANDOM_PORT_FINDER = new RandomPortFinder();

  private NetworkUtils() {
  }

  public static int getNextAvailablePort() {
    return RANDOM_PORT_FINDER.getNextAvailablePort();
  }

  @VisibleForTesting
  static class RandomPortFinder {
    private static final int MAX_TRY = 10;
    // Firefox blocks some reserved ports : http://www-archive.mozilla.org/projects/netlib/PortBanning.html
    private static final int[] BLOCKED_PORTS = {2049, 4045, 6000};
    private final Set<Integer> alreadyDistributedPorts = new HashSet<>();

    public int getNextAvailablePort() {
      synchronized (alreadyDistributedPorts) {
        for (int i = 0; i < MAX_TRY; i++) {
          try {
            int port = getRandomUnusedPort();
            if (isValidPort(port) && alreadyDistributedPorts.add(port)) {
              return port;
            }
          } catch (IOException e) {
            throw new IllegalStateException("Can't find an open network port", e);
          }
        }
      }

      throw new IllegalStateException("Can't find an open network port");
    }

    public int getRandomUnusedPort() throws IOException {
      try (ServerSocket socket = new ServerSocket()) {
        socket.bind(new InetSocketAddress("localhost", 0));
        return socket.getLocalPort();
      }
    }

    static boolean isValidPort(int port) {
      return port > 1023 && !ArrayUtils.contains(BLOCKED_PORTS, port);
    }
  }
}
