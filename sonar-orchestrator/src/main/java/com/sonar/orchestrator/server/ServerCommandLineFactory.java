/*
 * Orchestrator
 * Copyright (C) 2011-2023 SonarSource SA
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
package com.sonar.orchestrator.server;

import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.container.Server;
import java.io.File;
import java.util.Collection;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

public class ServerCommandLineFactory {

  private final FileSystem fs;

  public ServerCommandLineFactory(FileSystem fs) {
    this.fs = fs;
  }

  public CommandLine create(Server server) {
    CommandLine command = createJavaCommandLine();
    command.addArgument("-Xmx32m");
    command.addArgument("-server");
    command.addArgument("-Djava.awt.headless=true");
    command.addArgument("-Dsonar.enableStopCommand=true");
    command.addArgument("-Djava.net.preferIPv4Stack=true");
    IOFileFilter appJarFilter = FileFilterUtils.and(FileFilterUtils.prefixFileFilter("sonar-application-"), FileFilterUtils.suffixFileFilter("jar"));
    File libDir = new File(server.getHome(), "lib");
    Collection<File> files = FileUtils.listFiles(libDir, appJarFilter, FileFilterUtils.trueFileFilter());
    if (files.size() != 1) {
      throw new IllegalStateException("No or too many sonar-application-*.jar files found in: " + libDir.getAbsolutePath());
    }
    command.addArgument("-jar");
    // use relative path but not absolute path in order to support path containing whitespace
    command.addArgument("lib/" + files.iterator().next().getName());
    return command;
  }

  private CommandLine createJavaCommandLine() {
    CommandLine command;
    File javaHome = fs.javaHome();
    if (javaHome == null || !javaHome.exists()) {
      command = new CommandLine("java");
    } else {
      command = new CommandLine(FileUtils.getFile(javaHome, "bin", "java"));
    }
    return command;
  }
}
