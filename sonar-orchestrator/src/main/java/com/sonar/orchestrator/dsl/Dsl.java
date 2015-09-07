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
package com.sonar.orchestrator.dsl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.sonar.orchestrator.Orchestrator;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

public class Dsl {

  private final SonarItDslInterpreter dsl;

  @VisibleForTesting
  Dsl(SonarItDslInterpreter dsl) {
    this.dsl = dsl;
  }

  public Dsl() {
    this(new SonarItDslInterpreter());
  }

  public void execute(String dslText, Context context) {
    List<Command> commands = dsl.interpret(dslText);
    try {
      for (Command command : commands) {
        execute(command, context);
      }
    } finally {
      stop(context);
    }
  }

  private static void execute(Command command, Context context) {
    try {
      command.execute(context);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to execute command: " + command, e);
    }
  }

  private void stop(Context context) {
    context.stop();
  }

  public static class Context {
    private Orchestrator orchestrator = null;
    private Map<String, String> settings = Maps.newHashMap();
    private Map<String, Object> state = Maps.newHashMap();

    public Orchestrator getOrchestrator() {
      return orchestrator;
    }

    public Context setOrchestrator(Orchestrator o) {
      this.orchestrator = o;
      return this;
    }

    public Map<String, String> getSettings() {
      return settings;
    }

    public Context setSettings(Map<String, String> settings) {
      this.settings.clear();
      this.settings.putAll(Maps.newHashMap(settings));
      return this;
    }

    public Object getState(String key) {
      return state.get(key);
    }

    public Context setState(String key, @Nullable Object val) {
      state.put(key, val);
      return this;
    }

    public Context removeState(String key) {
      state.remove(key);
      return this;
    }

    public void stop() {
      if (orchestrator!=null) {
        orchestrator.stop();
      }
    }
  }
}
