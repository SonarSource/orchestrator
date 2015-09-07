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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public final class NetworkUtils {
  private NetworkUtils() {
  }

  public static int getNextAvailablePort() {
    for (int index = 0; index < 5; index++) {
      ServerSocket socket = null;
      try {
        socket = new ServerSocket();
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress("localhost", 0));
        int unusedPort = socket.getLocalPort();
        if (isValidPort(unusedPort)) {
          return unusedPort;
        }

      } catch (IOException e) {
        throw new IllegalStateException("Can not find a free network port", e);
      } finally {
        IOUtils.closeQuietly(socket);
      }
    }

    throw new IllegalStateException("Can not find an open network port");
  }

  // Firefox blocks some reserved ports : http://www-archive.mozilla.org/projects/netlib/PortBanning.html
  private static final int[] BLOCKED_PORTS = {2049, 4045, 6000};

  static boolean isValidPort(int port) {
    return port > 1023 && !ArrayUtils.contains(BLOCKED_PORTS, port);
  }
}
