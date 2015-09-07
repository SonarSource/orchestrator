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

import com.sonar.orchestrator.junit.PropertyFilterRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(PropertyFilterRunner.class)
public class SonarItDslInterpreterTest {

  SonarItDslInterpreter interpreter = new SonarItDslInterpreter();

  @Test
  public void fullGrammar() {
    List<Command> commands = interpreter.interpret(
      "start server with plugin Cobol 1.12 and plugin php 1.2 "
        + "and property sonar.something value "
        + "cd /dir1/dir2 "
        + "sonar-runner with property sonar.key value and sonar.something=value "
        + "verify sonar.org:core measure ncloc is 34"
        + "stop server "
        + "pause"
    );
    assertThat(commands).hasSize(6);
    assertThat(commands.get(0)).isInstanceOf(StartServerCommand.class);
    assertThat(commands.get(1)).isInstanceOf(CdCommand.class);

    assertThat(commands.get(2)).isInstanceOf(SonarRunnerCommand.class);

    assertThat(commands.get(3)).isInstanceOf(VerifyCommand.class);
    VerifyCommand verifyCommand = (VerifyCommand) commands.get(3);
    assertThat(verifyCommand.getResourceKey()).isEqualTo("sonar.org:core");
    assertThat(verifyCommand.getExpectedMeasures().size()).isEqualTo(1);

    assertThat(commands.get(4)).isInstanceOf(StopServerCommand.class);

    assertThat(commands.get(5)).isInstanceOf(PauseCommand.class);
  }

  @Test
  public void startServerCommand() {
    List<Command> commands = interpreter.interpret(
      "start server with plugin Cobol 1.12 and plugin php 2.3 and sonar.key=value"
    );
    assertThat(commands).hasSize(1);
    assertThat(commands.get(0)).isInstanceOf(StartServerCommand.class);
    StartServerCommand startServer = (StartServerCommand) commands.get(0);
    assertThat(startServer.getPlugins().get(0).key).isEqualTo("cobol");
    assertThat(startServer.getPlugins().get(0).version).isEqualTo("1.12");

    assertThat(startServer.getPlugins().get(1).key).isEqualTo("php");
    assertThat(startServer.getPlugins().get(1).version).isEqualTo("2.3");

    assertThat(startServer.getProperties().get("sonar.key")).isEqualTo("value");
  }

  @Test
  public void startAndStopServerCommands() {
    List<Command> commands = interpreter.interpret("start server stop server");
    assertThat(commands).hasSize(2);
    assertThat(commands.get(0)).isInstanceOf(StartServerCommand.class);
    assertThat(commands.get(1)).isInstanceOf(StopServerCommand.class);
  }

  @Test
  public void stopServerCommand() {
    List<Command> commands = interpreter.interpret("stop server");
    assertThat(commands).hasSize(1);
    assertThat(commands.get(0)).isInstanceOf(StopServerCommand.class);
  }

  @Test
  public void sonarRunnerCommand() {
    List<Command> commands = interpreter.interpret(
      "sonar-runner with property sonar.key value and sonar.key2=value2"
    );
    assertThat(commands).hasSize(1);
    assertThat(commands.get(0)).isInstanceOf(SonarRunnerCommand.class);
    SonarRunnerCommand sonarRunner = (SonarRunnerCommand) commands.get(0);
    assertThat(sonarRunner.getProperties().get("sonar.key")).isEqualTo("value");
  }

  @Test
  public void verifyResourceWithMinusCharacter() {
    List<Command> commands = interpreter.interpret(
      "verify java-sample measure ncloc is 1"
    );
    assertThat(commands).hasSize(1);
    assertThat(commands.get(0)).isInstanceOf(VerifyCommand.class);
    VerifyCommand command = (VerifyCommand) commands.get(0);
    assertThat(command.getResourceKey()).isEqualTo("java-sample");
  }

  @Test
  public void cdCommand() {
    List<Command> commands = interpreter.interpret("cd /dir1/dir2");
    assertThat(commands).hasSize(1);
    assertThat(commands.get(0)).isInstanceOf(CdCommand.class);
    assertThat(((CdCommand) commands.get(0)).getPath()).isEqualTo("/dir1/dir2");
  }

  @Test
  public void pauseCommand() {
    List<Command> commands = interpreter.interpret("pause");
    assertThat(commands).hasSize(1);
    assertThat(commands.get(0)).isInstanceOf(PauseCommand.class);
  }

}
