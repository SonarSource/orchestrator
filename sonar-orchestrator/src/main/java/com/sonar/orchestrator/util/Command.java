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
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Command {

  static class Os {
    boolean isWindows() {
      return SystemUtils.IS_OS_WINDOWS;
    }
  }

  private static final String DOUBLE_QUOTE = "\"";

  private final Os os;
  private String executable;
  private List<String> arguments = Lists.newArrayList();
  private File directory;
  private Map<String, String> env = Maps.newHashMap(System.getenv());

  @VisibleForTesting
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

  public Command addArguments(String[] args) {
    arguments.addAll(Arrays.asList(args));
    return this;
  }

  public Command addSystemArgument(String key, String value) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(value);

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
    List<String> command = Lists.newArrayList();
    if (os.isWindows()) {
      StringBuilder sb = new StringBuilder();
      sb.append(DOUBLE_QUOTE);
      sb.append(executable);
      for (String argument : arguments) {
        // all the characters that need parameter to be escaped
        if (os.isWindows() && argument.matches(".*[&<>\\(\\)\\[\\]\\{\\}\\^=;!\\+,`~\"'\\s]+.*")) {
          sb.append(" \"").append(argument);
          sb.append("\"");
        } else {
          sb.append(" ").append(argument);
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
    return Joiner.on(" ").join(toStrings());
  }

  @Override
  public String toString() {
    return toCommandLine();
  }

  /**
   * Create a command line without any arguments
   */
  public static Command create(String executable) {
    if (StringUtils.isBlank(executable)) {
      throw new IllegalArgumentException("Command executable can not be blank");
    }
    return new Command(executable, new Os());
  }
}
