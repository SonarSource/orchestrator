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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.sonar.orchestrator.dsl.StartServerCommand.Plugin;
import com.sonar.sslr.api.AstNode;

import java.util.List;

public class DslTransformation {

  public List<Command> transform(AstNode node) {
    return dslUnit(node);
  }

  private static List<Command> dslUnit(AstNode node) {
    Preconditions.checkArgument(node.is(SonarItDslGrammar.DSL_UNIT));
    List<Command> commands = Lists.newArrayList();
    for (AstNode commandNode : node.getChildren()) {
      if (commandNode.is(SonarItDslGrammar.START_SERVER_COMMAND)) {
        commands.add(startServerCommand(commandNode));
      } else if (commandNode.is(SonarItDslGrammar.STOP_SERVER_COMMAND)) {
        commands.add(stopServerCommand(commandNode));
      } else if (commandNode.is(SonarItDslGrammar.CD_COMMAND)) {
        commands.add(cdCommand(commandNode));
      } else if (commandNode.is(SonarItDslGrammar.SONAR_RUNNER_COMMAND)) {
        commands.add(sonarRunnerCommand(commandNode));
      } else if (commandNode.is(SonarItDslGrammar.VERIFY_COMMAND)) {
        commands.add(assertCommand(commandNode));
      } else if (commandNode.is(SonarItDslGrammar.PAUSE_COMMAND)) {
        commands.add(pauseCommand(commandNode));
      } else {
        throw new IllegalStateException();
      }
    }
    return commands;
  }

  private static StartServerCommand startServerCommand(AstNode node) {
    Preconditions.checkArgument(node.is(SonarItDslGrammar.START_SERVER_COMMAND));
    StartServerCommand startServer = new StartServerCommand();
    for (AstNode pluginNode : node.getDescendants(SonarItDslGrammar.PLUGIN)) {
      startServer.addPlugin(plugin(pluginNode));
    }
    for (AstNode propertyNode : node.getDescendants(SonarItDslGrammar.PROPERTY)) {
      Property property = property(propertyNode);
      startServer.setProperty(property.key, property.value);
    }
    return startServer;
  }

  private static Property property(AstNode node) {
    Preconditions.checkArgument(node.is(SonarItDslGrammar.PROPERTY));
    String key = node.getFirstDescendant(SonarItDslGrammar.PROPERTY_KEY).getTokenValue();
    String value = node.getFirstDescendant(SonarItDslGrammar.PROPERTY_VALUE).getTokenValue();
    return new Property(key, value);
  }

  private static Plugin plugin(AstNode node) {
    Preconditions.checkArgument(node.is(SonarItDslGrammar.PLUGIN));
    String key = node.getFirstDescendant(SonarItDslGrammar.PLUGIN_KEY).getTokenValue();
    String version = node.getFirstDescendant(SonarItDslGrammar.PLUGIN_VERSION).getTokenValue();
    return new Plugin(key, version);
  }

  private static StopServerCommand stopServerCommand(AstNode node) {
    Preconditions.checkArgument(node.is(SonarItDslGrammar.STOP_SERVER_COMMAND));
    return new StopServerCommand();
  }

  private static CdCommand cdCommand(AstNode node) {
    Preconditions.checkArgument(node.is(SonarItDslGrammar.CD_COMMAND));
    String path = node.getFirstDescendant(SonarItDslGrammar.PATH).getTokenValue();
    return new CdCommand(path);
  }

  private static SonarRunnerCommand sonarRunnerCommand(AstNode node) {
    Preconditions.checkArgument(node.is(SonarItDslGrammar.SONAR_RUNNER_COMMAND));
    SonarRunnerCommand sonarRunner = new SonarRunnerCommand();
    for (AstNode propertyNode : node.getDescendants(SonarItDslGrammar.PROPERTY)) {
      Property property = property(propertyNode);
      sonarRunner.setProperty(property.key, property.value);
    }
    return sonarRunner;
  }

  private static VerifyCommand assertCommand(AstNode node) {
    Preconditions.checkArgument(node.is(SonarItDslGrammar.VERIFY_COMMAND));
    VerifyCommand verifyCommand = new VerifyCommand(node.getFirstDescendant(SonarItDslGrammar.RESOURCE_KEY).getTokenValue());
    for (AstNode expectedMeasure : node.getDescendants(SonarItDslGrammar.MEASURE_ASSERTION)) {
      String metric = expectedMeasure.getFirstDescendant(SonarItDslGrammar.METRIC).getTokenValue();
      String measure = expectedMeasure.getFirstDescendant(SonarItDslGrammar.EXPECTED_MEASURE).getTokenValue();
      verifyCommand.setExpectedMeasure(metric, measure);
    }
    return verifyCommand;
  }

  private static PauseCommand pauseCommand(AstNode node) {
    Preconditions.checkArgument(node.is(SonarItDslGrammar.PAUSE_COMMAND));
    return new PauseCommand();
  }

  private static class Property {
    final String value;
    final String key;

    public Property(String key, String value) {
      this.key = key;
      this.value = value;
    }
  }

}
