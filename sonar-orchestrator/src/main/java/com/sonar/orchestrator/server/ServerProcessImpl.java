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
package com.sonar.orchestrator.server;

import com.google.common.annotations.VisibleForTesting;
import com.sonar.orchestrator.container.Server;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.Objects.requireNonNull;

public class ServerProcessImpl implements ServerProcess {

  private static final String MIN_SQ_SUPPORTED_VERSION = "4.5";
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerProcessImpl.class);
  private static final long START_RETRY_TIMEOUT_MS = 100L;
  private static final long START_TIMEOUT_MS = 300_000L;
  private static final long STOP_TIMEOUT_MS = 300_000L;

  private final ServerCommandLineFactory serverCommandLineFactory;
  private final Server server;
  private final StartupLogWatcher startupLogWatcher;
  private long startTimeoutMs;
  private long stopTimeoutMs;

  private DefaultExecuteResultHandler processResultHandler;
  private DefaultExecutor executor;
  private Thread shutdhownHook;

  public ServerProcessImpl(ServerCommandLineFactory serverCommandLineFactory, Server server,
    @Nullable StartupLogWatcher startupLogWatcher) {
    this.serverCommandLineFactory = requireNonNull(serverCommandLineFactory);
    this.server = requireNonNull(server);
    this.startTimeoutMs = START_TIMEOUT_MS;
    this.stopTimeoutMs = STOP_TIMEOUT_MS;
    if (startupLogWatcher == null) {
      this.startupLogWatcher = StartupLogWatcherImpl.create(server.version());
    } else {
      this.startupLogWatcher = startupLogWatcher;
    }
  }

  @VisibleForTesting
  void setStartupTimeout(long l) {
    this.startTimeoutMs = l;
  }

  @VisibleForTesting
  void setStopTimeout(long l) {
    this.stopTimeoutMs = l;
  }

  @Override
  public void start() {
    checkState(processResultHandler == null, "Server is already started");
    checkState(server.version().isGreaterThanOrEquals(MIN_SQ_SUPPORTED_VERSION),
      "Minimum supported version of SonarQube is %s. Got %s.", MIN_SQ_SUPPORTED_VERSION, server.version());

    LOGGER.info("Start server " + server.version() + " from " + server.getHome().getAbsolutePath());
    CommandLine command = serverCommandLineFactory.create(server);
    executor = new DefaultExecutor();
    executor.setWatchdog(new ExecuteWatchdog(-1L));
    executor.setWorkingDirectory(server.getHome());

    StartupLogListener listener = new StartupLogListener(startupLogWatcher);
    executor.setStreamHandler(new PumpStreamHandler(listener));
    processResultHandler = new DefaultExecuteResultHandler();
    try {
      executor.execute(command, freshEnv(), processResultHandler);
    } catch (IOException e) {
      throw fail("Can not execute command: " + command, e);
    }

    for (int i = 0; i < startTimeoutMs / START_RETRY_TIMEOUT_MS; i++) {
      if (listener.isStarted()) {
        shutdhownHook = new Thread(new StopShutdownHook());
        Runtime.getRuntime().addShutdownHook(shutdhownHook);
        return;
      }
      if (processResultHandler.hasResult()) {
        // process is down
        throw fail("Server startup failure", processResultHandler.getException());
      }
      sleepUninterruptibly(START_RETRY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }
    stop();
    throw fail("Server did not start in timely fashion", null);
  }

  @Override
  public void stop() {
    if (!isProcessAlive()) {
      return;
    }
    try {
      LOGGER.info("Stop server");
      askForStop();
      waitForExit();
      if (isProcessAlive()) {
        LOGGER.warn("Server is still up. Killing it.");
        forceKillProcess();
      }
      cleanState();
    } catch (Exception e) {
      throw fail("Can not stop server", e);
    }
  }

  private void askForStop() throws IOException {
    // file-based inter-process protocol : no RMI, no socket but good old files !
    FileUtils.touch(new File(server.getHome(), "temp/app.stop"));
    // Use Shared Memory for SQ 5.1+
    try (RandomAccessFile sharedMemory = new RandomAccessFile(new File(server.getHome(), "temp/sharedmemory"), "rw")) {
      // Using values from org.sonar.process.ProcessCommands
      MappedByteBuffer mappedByteBuffer = sharedMemory.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, 50L * 10);

      // Now we are stopping all processes as quick as possible
      // by asking for stop of "app" process
      mappedByteBuffer.put(1, (byte) 0xFF);
    }
  }

  private void forceKillProcess() throws InterruptedException {
    if (executor != null) {
      executor.getWatchdog().destroyProcess();
      waitForExit();
    }
  }

  private void waitForExit() throws InterruptedException {
    if (processResultHandler != null) {
      processResultHandler.waitFor(stopTimeoutMs);
    }
  }

  @VisibleForTesting
  boolean isProcessAlive() {
    return processResultHandler != null && !processResultHandler.hasResult();
  }

  private RuntimeException fail(String message, @Nullable Exception cause) {
    cleanState();
    throw new IllegalStateException(message, cause);
  }

  private void cleanState() {
    processResultHandler = null;
    executor = null;
    shutdhownHook = null;
  }

  private static Map<String, String> freshEnv() {
    Map<String, String> env = new HashMap<>();
    env.putAll(System.getenv());
    env.remove("GEM_PATH");
    env.remove("GEM_HOME");
    env.remove("RAILS_ENV");
    return env;
  }

  private class StopShutdownHook implements Runnable {
    @Override
    public void run() {
      stop();
    }
  }
}
