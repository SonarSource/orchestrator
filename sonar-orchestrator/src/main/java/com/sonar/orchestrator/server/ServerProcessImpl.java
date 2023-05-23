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

import com.sonar.orchestrator.container.Server;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonar.orchestrator.util.OrchestratorUtils.checkState;
import static java.util.Objects.requireNonNull;

public class ServerProcessImpl implements ServerProcess {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServerProcessImpl.class);
  private static final long START_RETRY_TIMEOUT_MS = 100L;
  private static final long START_TIMEOUT_MS = 600_000L;
  private static final long STOP_TIMEOUT_MS = 300_000L;

  private final ServerCommandLineFactory serverCommandLineFactory;
  private final Server server;
  private final StartupLogWatcher startupLogWatcher;
  private long startTimeoutMs;
  private long stopTimeoutMs;

  private DefaultExecuteResultHandler processResultHandler;
  private DefaultExecutor executor;
  private Thread shutdownHook;

  public ServerProcessImpl(ServerCommandLineFactory serverCommandLineFactory, Server server,
    @Nullable StartupLogWatcher startupLogWatcher) {
    this.serverCommandLineFactory = requireNonNull(serverCommandLineFactory);
    this.server = requireNonNull(server);
    this.startTimeoutMs = START_TIMEOUT_MS;
    this.stopTimeoutMs = STOP_TIMEOUT_MS;
    if (startupLogWatcher == null) {
      this.startupLogWatcher = StartupLogWatcherImpl.create();
    } else {
      this.startupLogWatcher = startupLogWatcher;
    }
  }

  void setStartupTimeout(long l) {
    this.startTimeoutMs = l;
  }

  void setStopTimeout(long l) {
    this.stopTimeoutMs = l;
  }

  @Override
  public void start() {
    checkState(processResultHandler == null, "Server is already started");
    checkState(server.version().isGreaterThanOrEquals(6, 2),
      "Minimum supported version of SonarQube is 6.2. Got %s.", server.version());

    LOGGER.info("Start server {} from {}", server.version(), server.getHome().getAbsolutePath());
    CommandLine command = serverCommandLineFactory.create(server);
    executor = new DefaultExecutor();
    executor.setWatchdog(new ExecuteWatchdog(-1L));
    executor.setWorkingDirectory(server.getHome());

    StartupLogListener listener = new StartupLogListener(startupLogWatcher, server.getClusterNodeName().orElse(null));
    executor.setStreamHandler(new PumpStreamHandler(listener));
    processResultHandler = new DefaultExecuteResultHandler();
    try {
      executor.execute(command, freshEnv(), processResultHandler);
    } catch (IOException e) {
      throw fail("Can not execute command: " + command, e);
    }

    for (int i = 0; i < startTimeoutMs / START_RETRY_TIMEOUT_MS; i++) {
      if (listener.isStarted()) {
        shutdownHook = new Thread(new StopShutdownHook());
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        return;
      }
      if (processResultHandler.hasResult()) {
        // process is down
        throw fail("Server startup failure", processResultHandler.getException());
      }
      try {
        Thread.sleep(START_RETRY_TIMEOUT_MS);
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
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

  private void forceKillProcess() {
    if (executor != null) {
      executor.getWatchdog().destroyProcess();
      waitForExit();
    }
  }

  private void waitForExit() {
    if (processResultHandler != null) {
      try {
        processResultHandler.waitFor(stopTimeoutMs);
      } catch (InterruptedException e) {
        // Ignore it, we hitting the exception if everything goes well
        // Do not set the flag through Thread.currentThread().interrupt()
        // since next "wait" method will catch an InterruptedException
      }
    }
  }

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
    shutdownHook = null;
  }

  private static Map<String, String> freshEnv() {
    Map<String, String> env = new HashMap<>(System.getenv());
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
