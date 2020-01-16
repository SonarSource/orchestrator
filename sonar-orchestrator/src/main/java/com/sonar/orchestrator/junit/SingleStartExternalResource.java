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
package com.sonar.orchestrator.junit;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.rules.ExternalResource;

/**
 * Extends JUnit {@link ExternalResource} to prevent nested execution (for exemple
 * when using test suites).
 * @author Julien HENRY
 *
 */
public abstract class SingleStartExternalResource extends ExternalResource {

  private static final String ALREADY_STOPPED = "Already stopped...";
  private AtomicInteger nestedCount = new AtomicInteger(0);
  private boolean started = false;
  private boolean stopped = false;

  @Override
  protected final void before() throws Throwable {
    synchronized (nestedCount) {
      nestedCount.incrementAndGet();
      if (stopped) {
        throw new RuntimeException(ALREADY_STOPPED);
      }
      if (!started) {
        beforeAll();
        started = true;
      }
    }
  }

  protected abstract void beforeAll();

  @Override
  protected final void after() {
    synchronized (nestedCount) {
      int value = nestedCount.decrementAndGet();
      if (stopped) {
        throw new RuntimeException(ALREADY_STOPPED);
      }
      if (started && value == 0) {
        afterAll();
        stopped = true;
      }
    }
  }

  protected abstract void afterAll();
}
