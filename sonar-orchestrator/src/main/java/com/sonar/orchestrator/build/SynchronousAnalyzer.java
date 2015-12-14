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
package com.sonar.orchestrator.build;

import com.google.common.util.concurrent.Uninterruptibles;
import com.sonar.orchestrator.container.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * See http://jira.sonarsource.com/browse/ORCH-263
 */
public class SynchronousAnalyzer {

  private static final Logger LOGGER = LoggerFactory.getLogger(SynchronousAnalyzer.class);
  static final String RELATIVE_URL = "/api/analysis_reports/is_queue_empty";

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
    if (server.version().isGreaterThanOrEquals("5.0")) {
      doWaitForDone();
    }
  }

  long getDelayMs() {
    return delayMs;
  }

  int getLogFrequency() {
    return logFrequency;
  }

  private void doWaitForDone() {
    boolean empty = false;
    int count = 0;
    while (!empty) {
      if (count % logFrequency == 0) {
        LOGGER.info("Waiting for analysis reports to be integrated");
      }
      String response = server.post(RELATIVE_URL, Collections.<String, Object>emptyMap());
      empty = "true".equals(response);
      Uninterruptibles.sleepUninterruptibly(delayMs, TimeUnit.MILLISECONDS);
      count++;
    }
  }
}
