/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
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
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandTest {

  private static final Command.Os WINDOWS = new Command.Os() {
    @Override
    boolean isWindows() {
      return true;
    }
  };

  private static final Command.Os NON_WINDOWS = new Command.Os() {
    @Override
    boolean isWindows() {
      return false;
    }
  };

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void fail_if_empty_executable() {
    thrown.expect(IllegalArgumentException.class);

    Command.create("");
  }

  @Test
  public void fail_if_null_executable() {
    thrown.expect(IllegalArgumentException.class);
    Command.create(null);
  }

  @Test
  public void create_command_on_windows() {
    Command command = new Command("java", WINDOWS);
    command.addArgument("-Xmx512m");
    command.addArguments(Arrays.asList("-a", "-b"));
    command.addArguments(new String[] {"-x", "-y"});
    assertThat(command.getExecutable()).isEqualTo("java");
    assertThat(command.getArguments()).hasSize(5);
    assertThat(command.toCommandLine()).isEqualTo("cmd /c \"java -Xmx512m -a -b -x -y\"");
  }

  @Test
  public void escape_argument_on_windows() {
    Command command = new Command("java", WINDOWS);
    command.addArgument("url=mysql?foo;bar&baz");
    assertThat(command.toCommandLine()).isEqualTo("cmd /c \"java \"url=mysql?foo;bar&baz\"\"");
  }

  @Test
  public void escape_executable_on_windows() {
    Command command = new Command("c:\\Program Files\\java.exe", WINDOWS);
    command.addArgument("foo");
    assertThat(command.toCommandLine()).isEqualTo("cmd /c \"\"c:\\Program Files\\java.exe\" foo\"");
  }

  @Test
  public void create_command_on_non_windows() {
    Command command = new Command("java", NON_WINDOWS);
    command.addArgument("-Xmx512m");
    command.addArguments(Arrays.asList("-a", "-b"));
    command.addArguments(new String[] {"-x", "-y"});
    assertThat(command.getExecutable()).isEqualTo("java");
    assertThat(command.getArguments()).hasSize(5);
    assertThat(command.toCommandLine()).isEqualTo("java -Xmx512m -a -b -x -y");
  }

  @Test
  public void addSystemArgument_on_windows() {
    Command command = new Command("java", WINDOWS);
    command.addSystemArgument("foo", "bar");
    command.addSystemArgument("profile", "sonar way");
    assertThat(command.toCommandLine()).isEqualTo("cmd /c \"java \"-Dfoo=bar\" \"-Dprofile=sonar way\"\"");
  }

  @Test
  public void addSystemArgument_on_non_windows() {
    Command command = new Command("java", NON_WINDOWS);
    command.addSystemArgument("foo", "bar");
    command.addSystemArgument("profile", "sonar way");

    assertThat(command.toCommandLine()).isEqualTo("java -Dfoo=bar -Dprofile=sonar way");
  }

  @Test
  public void should_fail_if_add_system_argument_with_null_value() {
    thrown.expect(NullPointerException.class);

    Command command = Command.create("java");
    command.addSystemArgument("foo", null);
  }

  @Test
  public void toString_is_the_command_line() {
    Command command = Command.create("java");
    command.addArgument("-Xmx512m");
    assertThat(command.toString()).isEqualTo(command.toCommandLine());
  }

  @Test
  public void working_directory() {
    Command command = Command.create("java");
    assertThat(command.getDirectory()).isNull();

    File working = new File("working");
    command = Command.create("java").setDirectory(working);
    assertThat(command.getDirectory()).isEqualTo(working);
  }

  @Test
  public void initialize_with_current_env() {
    Command command = Command.create("java");
    assertThat(command.getEnvironmentVariables()).isNotEmpty();
  }

  @Test
  public void override_env_variables() {
    Command command = Command.create("java");
    command.setEnvironmentVariable("JAVA_HOME", "/path/to/java");
    assertThat(command.getEnvironmentVariables().get("JAVA_HOME")).isEqualTo("/path/to/java");
  }
}
