/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package com.sonar.orchestrator.server;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import org.apache.commons.exec.LogOutputStream;

import static java.util.Objects.requireNonNull;

class StartupLogListener extends LogOutputStream {

  private final AtomicBoolean started = new AtomicBoolean(false);
  private final String logPrefix;
  private final StartupLogWatcher watcher;

  StartupLogListener(StartupLogWatcher watcher, @Nullable String logPrefix) {
    this.watcher = requireNonNull(watcher);
    this.logPrefix = (logPrefix == null ? "> " : logPrefix + "> ");
  }

  @Override
  protected void processLine(String line, @SuppressWarnings("unused") int logLevel) {
    if (watcher.isStarted(line)) {
      started.set(true);
    }
    System.out.println(logPrefix + line);
  }

  boolean isStarted() {
    return started.get();
  }
}
