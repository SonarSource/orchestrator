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
package com.sonar.orchestrator.build;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.util.Command;
import com.sonar.orchestrator.util.CommandExecutor;
import com.sonar.orchestrator.util.StreamConsumer;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AntBuildExecutorTest {
  @Test
  public void execute_command() {
    final FileLocation buildFile = FileLocation.of("src/test/resources/com/sonar/orchestrator/build/AntBuildTest/build.xml");
    AntBuild build = AntBuild.create()
      .setBuildLocation(buildFile)
      .setTimeoutSeconds(30)
      .setTargets("sonar");

    Map<String, String> props = new TreeMap<>();
    props.put("sonar.jdbc.dialect", "h2");

    CommandExecutor executor = mock(CommandExecutor.class);
    when(executor.execute(any(Command.class), any(StreamConsumer.class), anyLong())).thenReturn(2);

    new AntBuildExecutor().execute(build, Configuration.create(), props, executor);

    verify(executor).execute(argThat(c -> c.toCommandLine().contains("ant") &&
      c.toCommandLine().contains("-f") &&
      c.toCommandLine().contains("-Dsonar.jdbc.dialect=")), any(), eq(30000L));
  }
}
