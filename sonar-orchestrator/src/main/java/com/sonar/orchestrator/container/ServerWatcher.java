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
package com.sonar.orchestrator.container;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.connectors.ConnectionException;
import org.sonar.wsclient.connectors.HttpClient4Connector;
import org.sonar.wsclient.services.ServerQuery;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkState;

class ServerWatcher {
  private static final Logger LOG = LoggerFactory.getLogger(ServerWatcher.class);
  static final int RETRY_TIMEOUT_MS = 200;
  static final int MAX_TIMEOUT_MS = 5 * 60 * 1000;

  private final Sonar wsClient;
  private final int retries;
  private final CommonsExec commonsExec;

  static class CommonsExec {
    DefaultExecuteResultHandler newResultHandler() {
      return new DefaultExecuteResultHandler();
    }
  }

  private DefaultExecuteResultHandler resultHandler;

  ServerWatcher(Server server) {
    this.wsClient = new Sonar(new HttpClient4Connector(new Host(server.getUrl())));
    this.retries = MAX_TIMEOUT_MS / RETRY_TIMEOUT_MS;
    this.commonsExec = new CommonsExec();
  }

  @VisibleForTesting
  ServerWatcher(Sonar wsClient, int retries, CommonsExec ce) {
    this.wsClient = wsClient;
    this.retries = retries;
    this.commonsExec = ce;
  }

  void execute(DefaultExecutor executor, CommandLine command) throws IOException {
    executor.execute(command, newResultHandler());
    waitForStartup();
  }

  private ExecuteResultHandler newResultHandler() {
    checkState(resultHandler == null, "An existing server watcher is already running");
    resultHandler = commonsExec.newResultHandler();
    return resultHandler;
  }

  private void waitForStartup() {
    LOG.info("Wait for server to start");

    for (int i = 0; i < retries; i++) {
      try {
        if (isServerUp()) {
          LOG.info("Sonar is started");
          return;
        }
        Thread.sleep(RETRY_TIMEOUT_MS);

      } catch (InterruptedException ignored) {
        // try again immediately
      }
    }
    throw new IllegalStateException("Can't start sonar in timely fashion");
  }

  boolean isServerUp() {
    try {
      org.sonar.wsclient.services.Server server = wsClient.find(new ServerQuery());
      if (server != null) {
        org.sonar.wsclient.services.Server.Status status = server.getStatus();
        if (status == org.sonar.wsclient.services.Server.Status.UP || status == org.sonar.wsclient.services.Server.Status.SETUP) {
          return true;
        }
      }
    } catch (ConnectionException e) {
      if (resultHandler.hasResult()) {
        // process is down
        throw new IllegalStateException("Server startup failure", resultHandler.getException());
      }

      // Check if internal error (5xx HTTP code)
      // To be improved - it's quite dangerous to depend on message.
      if (StringUtils.contains(e.getMessage(), "HTTP error: 5")) {
        throw new IllegalStateException("Server internal error", e);
      }
    }
    return false;
  }

  void waitForExit(long timeoutMs) throws InterruptedException {
    if (resultHandler != null) {
      while (!resultHandler.hasResult()) {
        waitFor(timeoutMs);
      }
      resultHandler = null;
    }
  }

  void waitFor(long timeoutMs) throws InterruptedException {
    if (resultHandler != null) {
      resultHandler.waitFor(timeoutMs);
    }
  }

  @VisibleForTesting
  int retries() {
    return retries;
  }
}
