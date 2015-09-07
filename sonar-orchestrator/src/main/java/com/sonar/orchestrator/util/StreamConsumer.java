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

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;

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
      } catch (IOException e) {
        LoggerFactory.getLogger(Pipe.class).error("Fail to write : " + line, e);
      }
    }
  }


}
