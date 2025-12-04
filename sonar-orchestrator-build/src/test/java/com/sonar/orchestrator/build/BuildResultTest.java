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
package com.sonar.orchestrator.build;

import java.io.Writer;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BuildResultTest {

  @Test
  public void test_log_filtering() throws Exception {
    BuildResult buildResult = new BuildResult();
    Writer writer = buildResult.getLogsWriter();
    writer.append("Hello,\n");
    writer.append("World!\n");
    writer.append("This is a pretty good logging feature.\n");
    writer.append("Goodbye now.\n");

    assertThat(buildResult.getLogsLines(s -> true)).hasSize(4);
    assertThat(buildResult.getLogsLines(s -> false)).isEmpty();
    assertThat(buildResult.getLogsLines(s -> s.contains("Hello") || s.contains("now")))
      .hasSize(2)
      .containsExactly("Hello,", "Goodbye now.");
  }

}
