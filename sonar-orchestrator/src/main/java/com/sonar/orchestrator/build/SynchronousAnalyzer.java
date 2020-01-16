/*
 * Orchestrator
 * Copyright (C) 2011-2020 SonarSource SA
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
package com.sonar.orchestrator.build;

import com.sonar.orchestrator.container.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * See http://jira.sonarsource.com/browse/ORCH-263
 */
public class SynchronousAnalyzer {

  private static final Logger LOGGER = LoggerFactory.getLogger(SynchronousAnalyzer.class);

  static final String RELATIVE_PATH = "/api/analysis_reports/is_queue_empty";

  private final Server server;
  private final long delayMs;
  private final int logFrequency;

  public SynchronousAnalyzer(Server server) {
    // check every 100ms and log every seconds
    this(server, 100L, 10);
  }

  SynchronousAnalyzer(Server server, long delayMs, int logFrequency) {
    this.server = server;
    this.delayMs = delayMs;
    this.logFrequency = logFrequency;
  }

  public void waitForDone() {
    boolean empty = false;
    int count = 0;
    while (!empty) {
      if (count % logFrequency == 0) {
        LOGGER.info("Waiting for analysis reports to be integrated");
      }
      try {
        String response = server.newHttpCall(RELATIVE_PATH)
          .setAdminCredentials()
          .execute()
          .getBodyAsString();
        empty = "true".equals(response);
        Thread.sleep(delayMs);
        count++;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }

  long getDelayMs() {
    return delayMs;
  }

  int getLogFrequency() {
    return logFrequency;
  }

}
