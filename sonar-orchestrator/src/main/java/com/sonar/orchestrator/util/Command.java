/*
 * Orchestrator
 * Copyright (C) 2011-2022 SonarSource SA
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang.SystemUtils;

import static com.sonar.orchestrator.util.OrchestratorUtils.checkArgument;
import static com.sonar.orchestrator.util.OrchestratorUtils.isEmpty;
import static java.util.Objects.requireNonNull;

public class Command {

  static class Os {
    boolean isWindows() {
      return SystemUtils.IS_OS_WINDOWS;
    }
  }

  private static final String DOUBLE_QUOTE = "\"";

  private final Os os;
  private String executable;
  private List<String> arguments = new ArrayList<>();
  private File directory;
  private Map<String, String> env = new HashMap<>(System.getenv());

  Command(String executable, Os os) {
    this.executable = executable;
    this.os = os;
  }

  public String getExecutable() {
    return executable;
  }

  public List<String> getArguments() {
    return Collections.unmodifiableList(arguments);
  }

  public Command addArgument(String arg) {
    arguments.add(arg);
    return this;
  }

  public Command addArguments(List<String> args) {
    arguments.addAll(args);
    return this;
  }

  public Command addArguments(String... args) {
    arguments.addAll(Arrays.asList(args));
    return this;
  }

  public Command addSystemArgument(String key, String value) {
    requireNonNull(key);
    requireNonNull(value);

    StringBuilder sb = new StringBuilder();
    sb.append("-D").append(key).append("=").append(value);
    return addArgument(sb.toString());
  }

  public File getDirectory() {
    return directory;
  }

  /**
   * Sets working directory.
   */
  public Command setDirectory(File d) {
    this.directory = d;
    return this;
  }

  /**
   * @see {@link Command#getEnvironmentVariables()}
   * @since 3.2
   */
  public Command setEnvironmentVariable(String name, String value) {
    this.env.put(name, value);
    return this;
  }

  /**
   * Environment variables that are propagated during command execution.
   * The initial value is a copy of the environment of the current process.
   *
   * @return a non-null and immutable map of variables
   * @since 3.2
   */
  public Map<String, String> getEnvironmentVariables() {
    return Collections.unmodifiableMap(env);
  }

  String[] toStrings() {
    List<String> command = new ArrayList<>();
    if (os.isWindows()) {
      StringBuilder sb = new StringBuilder();
      sb.append(DOUBLE_QUOTE);
      if (executable.matches(".*[&<>\\(\\)\\[\\]\\{\\}\\^=;!\\+,`~\"'\\s]+.*")) {
        sb.append(DOUBLE_QUOTE).append(executable).append(DOUBLE_QUOTE);
      } else {
        sb.append(executable);
      }
      for (String argument : arguments) {
        sb.append(" ");
        // all the characters that need parameter to be escaped
        if (argument.matches(".*[&<>\\(\\)\\[\\]\\{\\}\\^=;!\\+,`~\"'\\s]+.*")) {
          sb.append(DOUBLE_QUOTE).append(argument).append(DOUBLE_QUOTE);
        } else {
          sb.append(argument);
        }
      }
      sb.append(DOUBLE_QUOTE);
      command = Arrays.asList("cmd", "/c", sb.toString());

    } else {
      command.add(executable);
      command.addAll(arguments);
    }
    return command.toArray(new String[command.size()]);
  }

  public String toCommandLine() {
    return Arrays.stream(toStrings()).collect(Collectors.joining(" "));
  }

  @Override
  public String toString() {
    return toCommandLine();
  }

  /**
   * Create a command line without any arguments
   */
  public static Command create(String executable) {
    checkArgument(!isEmpty(executable), "Command executable can not be blank");
    return new Command(executable, new Os());
  }
}
