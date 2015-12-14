/*
 * Orchestrator
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package com.sonar.orchestrator.container;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.sonar.orchestrator.config.Configuration;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerWrapper {

  private static final Logger LOG = LoggerFactory.getLogger(ServerWrapper.class);
  private static final int STOP_TIMEOUT_MS = 300000;

  private final File workingDir;
  private final Configuration config;
  private final File javaHome;
  private final DefaultExecutor executor;
  private final ServerWatcher watcher;
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final Server server;

  public ServerWrapper(Server server, Configuration config, File javaHome) {
    this.server = server;
    this.workingDir = server.getHome();
    this.config = config;
    this.javaHome = javaHome;
    this.executor = new DefaultExecutor();
    this.executor.setWatchdog(new ExecuteWatchdog(-1L));
    this.executor.setProcessDestroyer(new ShutdownHookProcessDestroyer());
    this.watcher = new ServerWatcher(server);
  }

  @VisibleForTesting
  ServerWrapper(Server server, Configuration config, File javaHome, DefaultExecutor executor, ServerWatcher watcher) {
    this.server = server;
    this.workingDir = server.getHome();
    this.config = config;
    this.javaHome = javaHome;
    this.executor = executor;
    this.watcher = watcher;
  }

  public void start() {
    if (started.get()) {
      throw new IllegalStateException("Server is already started");
    }

    LOG.info("Start server " + server.getUrl() + " in " + workingDir);
    if (!server.version().isGreaterThanOrEquals("4.5")) {
      throw new IllegalStateException("Minimum supported version of SonarQube is 4.5. Got " + server.version());
    }

    CommandLine command = buildCommandLine();

    executor.setWorkingDirectory(workingDir);
    try {
      watcher.execute(executor, command);
      started.set(true);
    } catch (Exception e) {
      stopAndClean();
      throw Throwables.propagate(e);
    }
  }

  @VisibleForTesting
  CommandLine buildCommandLine() {
    CommandLine command;
    if (javaHome == null) {
      command = new CommandLine("java");
    } else {
      command = new CommandLine(FileUtils.getFile(javaHome, "bin", "java"));
    }
    command.addArgument("-Xmx32m");
    command.addArgument("-server");
    command.addArgument("-Djava.awt.headless=true");
    command.addArgument("-Dsonar.search.port=0");
    command.addArgument("-Dsonar.enableStopCommand=true");
    for (String arg : server.getDistribution().serverAdditionalJvmArguments()) {
      command.addArgument(arg);
    }
    IOFileFilter appJarFilter = FileFilterUtils.and(FileFilterUtils.prefixFileFilter("sonar-application-"), FileFilterUtils.suffixFileFilter("jar"));
    File libDir = new File(workingDir, "lib");
    Collection<File> files = FileUtils.listFiles(libDir, appJarFilter, FileFilterUtils.trueFileFilter());
    if (files.size() != 1) {
      throw new IllegalStateException("No or too many sonar-application-*.jar files found in: " + libDir);
    }
    command.addArgument("-jar");
    // use relative path but not absolute path in order to support path containing whitespace
    command.addArgument("lib/" + files.iterator().next().getName());
    return command;
  }

  public void stop() {
    try {
      if (!started.getAndSet(false)) {
        // already stopped
        return;
      }

      LOG.info("Stop sonar");
      shutdownAfter45();
      watcher.waitForExit(STOP_TIMEOUT_MS);
      killProcess();

      LOG.info("Sonar is stopped");
    } catch (Exception e) {
      LOG.error("Can't stop sonar", e);
    }
  }

  private void shutdownAfter45() throws IOException {
    // file-based inter-process protocol : no RMI, no socket but good old files !
    FileUtils.touch(new File(workingDir, "temp/app.stop"));
    // Use Shared Memory for SQ 5.1+
    try (RandomAccessFile sharedMemory = new RandomAccessFile(new File(workingDir, "temp/sharedmemory"), "rw")) {
      // Using values from org.sonar.process.ProcessCommands
      MappedByteBuffer mappedByteBuffer = sharedMemory.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, 50 * 10);

      // Now we are stopping all processes as quick as possible
      mappedByteBuffer.put(1, (byte) 0xFF); // stopping app process
    }
  }

  public void stopAndClean() {
    stop();
    deleteWorkspace();
  }

  private void killProcess() throws InterruptedException {
    executor.getWatchdog().destroyProcess();
    watcher.waitForExit(STOP_TIMEOUT_MS);
  }

  private void deleteWorkspace() {
    String value = config.getString("orchestrator.keepWorkspace", "false");
    boolean keepWorkspace = StringUtils.isNotBlank(value) ? Boolean.valueOf(value) : false;

    if (!keepWorkspace && workingDir != null && workingDir.exists()) {
      for (File dir : workingDir.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY)) {
        if (!"logs".equals(dir.getName())) {
          FileUtils.deleteQuietly(dir);
        }
      }
    }
  }

  @VisibleForTesting
  DefaultExecutor executor() {
    return executor;
  }

  @VisibleForTesting
  boolean isStarted() {
    return started.get();
  }
}
