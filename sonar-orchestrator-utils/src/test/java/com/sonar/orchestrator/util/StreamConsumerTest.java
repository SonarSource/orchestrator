/*
 * Orchestrator Utils
 * Copyright (C) 2011-2025 SonarSource Sàrl
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

import java.io.StringWriter;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StreamConsumerTest {

  @Test
  public void consumeLine() {
    StringWriter writer = new StringWriter();
    StreamConsumer.Pipe pipe = new StreamConsumer.Pipe(writer);

    pipe.consumeLine("foo");
    pipe.consumeLine("bar");

    // https://jira.sonarsource.com/browse/ORCH-342 keep newlines
    assertThat(writer.toString()).isEqualTo("foo\nbar\n");
  }
}
