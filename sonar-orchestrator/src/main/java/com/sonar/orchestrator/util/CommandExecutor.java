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

import com.google.common.base.Preconditions;
import com.google.common.io.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Synchronously execute a native command line. It's much more limited than the Apache Commons Exec library.
 * For example it does not allow to run asynchronously or to automatically quote command-line arguments.
 *
 * @since 2.7
 */
public class CommandExecutor {

  private static final Logger LOG = LoggerFactory.getLogger(CommandExecutor.class);

  private static final CommandExecutor INSTANCE = new CommandExecutor();

  private CommandExecutor() {
  }

  public static CommandExecutor create() {
    // stateless object, so a single singleton can be shared
    return INSTANCE;
  }

  /**
   * @throws CommandException
   * @since 3.0
   */
  public int execute(Command command, StreamConsumer stdOut, long timeoutMilliseconds) {
    Preconditions.checkNotNull(command);

    ExecutorService executorService = null;
    Process process = null;
    StreamGobbler outputGobbler = null;
    ProcessBuilder builder = new ProcessBuilder(command.toStrings());
    try {
      if (command.getDirectory() != null) {
        builder.directory(command.getDirectory());
      }
      builder.environment().putAll(command.getEnvironmentVariables());
      builder.redirectErrorStream(true);
      process = builder.start();

      outputGobbler = new StreamGobbler(process.getInputStream(), stdOut);
      outputGobbler.start();

      final Process finalProcess = process;
      executorService = Executors.newSingleThreadExecutor();
      Future<Integer> ft = executorService.submit(new Callable<Integer>() {
        @Override
        public Integer call() throws InterruptedException {
          return finalProcess.waitFor();
        }
      });
      int exitCode = ft.get(timeoutMilliseconds, TimeUnit.MILLISECONDS);
      waitUntilFinish(outputGobbler);
      verifyGobbler(command, outputGobbler);
      return exitCode;

    } catch (TimeoutException te) {
      if (process != null) {
        process.destroy();
      }
      throw new CommandException(command, "Timeout exceeded: " + timeoutMilliseconds + " ms", te);

    } catch (CommandException e) {
      throw e;

    } catch (Exception e) {
      throw new CommandException(command, e);

    } finally {
      waitUntilFinish(outputGobbler);
      closeStreams(process);

      if (executorService != null) {
        executorService.shutdown();
      }
    }
  }

  private void verifyGobbler(Command command, StreamGobbler gobbler) {
    if (gobbler.getException() != null) {
      throw new CommandException(command, gobbler.getException());
    }
  }

  /**
   * Execute command and display error and output streams in log.
   * Method {@link #execute(Command, StreamConsumer, long)} is preferable,
   * when fine-grained control of output of command required.
   *
   * @throws CommandException
   */
  public int execute(Command command, long timeoutMilliseconds) {
    LOG.info("Executing command: " + command);
    return execute(command, new DefaultConsumer(), timeoutMilliseconds);
  }

  private static void closeStreams(@Nullable Process process) {
    if (process != null) {
      try {
        Closeables.close(process.getInputStream(), true);
        Closeables.close(process.getOutputStream(), true);
        Closeables.close(process.getErrorStream(), true);
      } catch (IOException e) {
        throw new IllegalStateException("Unexpected IOException since it is supposed to be swallowed", e);
      }
    }
  }

  private static void waitUntilFinish(@Nullable StreamGobbler thread) {
    if (thread != null) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        LOG.error("InterruptedException while waiting finish of " + thread.toString(), e);
      }
    }
  }

  private static class StreamGobbler extends Thread {
    private final InputStream is;
    private final StreamConsumer consumer;
    private volatile Exception exception;

    StreamGobbler(InputStream is, StreamConsumer consumer) {
      super("ProcessStreamGobbler");
      this.is = is;
      this.consumer = consumer;
    }

    @Override
    public void run() {
      try (InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr)) {
        String line;
        while ((line = br.readLine()) != null) {
          consumeLine(line);
        }
      } catch (IOException ioe) {
        exception = ioe;
      }
    }

    private void consumeLine(String line) {
      if (exception == null) {
        try {
          consumer.consumeLine(line);
        } catch (Exception e) {
          exception = e;
        }
      }
    }

    public Exception getException() {
      return exception;
    }
  }

  private static class DefaultConsumer implements StreamConsumer {
    @Override
    public void consumeLine(String line) {
      LOG.info(line);
    }
  }
}
