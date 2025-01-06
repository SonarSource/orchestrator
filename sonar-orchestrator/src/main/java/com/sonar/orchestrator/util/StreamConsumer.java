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
package com.sonar.orchestrator.util;

import java.io.IOException;
import java.io.Writer;
import org.slf4j.LoggerFactory;

public interface StreamConsumer {

  void consumeLine(String line);

  class Pipe implements StreamConsumer {

    private final Writer writer;

    public Pipe(Writer writer) {
      this.writer = writer;
    }

    @Override
    public void consumeLine(String line) {
      try {
        System.out.println(line);
        writer.write(line);
        writer.write("\n");
      } catch (IOException e) {
        LoggerFactory.getLogger(Pipe.class).error("Fail to write : " + line, e);
      }
    }
  }


}
