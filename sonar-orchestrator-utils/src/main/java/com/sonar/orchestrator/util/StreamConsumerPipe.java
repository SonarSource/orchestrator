/*
 * Orchestrator Utils
 * Copyright (C) SonarSource Sàrl
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

import java.io.IOException;
import java.io.Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes each consumed line to the given {@link Writer} and echoes it to the console.
 * <p>
 * Console echo goes through the SLF4J logger named {@code com.sonar.orchestrator.BuildOutput}
 * instead of {@code System.out}. The bundled logback configuration prints this logger's lines
 * unchanged ({@code %msg%n} pattern). With a different logging setup, the visibility and format of
 * build output depend on how that logger is configured.
 */
public class StreamConsumerPipe implements StreamConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(StreamConsumerPipe.class);

  private final Writer writer;

  public StreamConsumerPipe(Writer writer) {
    this.writer = writer;
  }

  @Override
  public void consumeLine(String line) {
    try {
      BuildOutput.echo(line);
      writer.write(line);
      writer.write("\n");
    } catch (IOException e) {
      LOGGER.error("Fail to write : " + line, e);
    }
  }

  /**
   * Dedicated logger used to echo build output lines to the console.
   */
  private static final class BuildOutput {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.sonar.orchestrator.BuildOutput");

    private BuildOutput() {
      // only static methods
    }

    static void echo(String line) {
      LOGGER.info(line);
    }
  }
}
