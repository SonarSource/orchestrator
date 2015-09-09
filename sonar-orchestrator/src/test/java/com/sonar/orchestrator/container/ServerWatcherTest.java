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
package com.sonar.orchestrator.container;

import com.sonar.orchestrator.junit.PropertyFilterRunner;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.connectors.ConnectionException;
import org.sonar.wsclient.services.Server;
import org.sonar.wsclient.services.ServerQuery;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PropertyFilterRunner.class)
public class ServerWatcherTest {

  Sonar wsClient = mock(Sonar.class, Mockito.RETURNS_DEEP_STUBS);
  DefaultExecuteResultHandler resultHandler = mock(DefaultExecuteResultHandler.class);
  ServerWatcher.CommonsExec commonsExec = mock(ServerWatcher.CommonsExec.class);
  ServerWatcher watcher = new ServerWatcher(wsClient, 2, commonsExec);

  @Test
  public void wait_for_startup() throws IOException {
    when(wsClient.find(any(ServerQuery.class))).thenReturn(
      migratingServer(),
      upServer()
    );

    watcher.execute(mock(DefaultExecutor.class), mock(CommandLine.class));
  }

  @Test(expected = RuntimeException.class)
  public void exceed_max_duration() throws IOException {
    when(wsClient.find(any(ServerQuery.class))).thenReturn(
      migratingServer(),
      migratingServer(),
      migratingServer()
    );

    watcher.execute(mock(DefaultExecutor.class), mock(CommandLine.class));
  }

  @Test(expected = RuntimeException.class)
  public void server_startup_failure_1() throws IOException {
    when(wsClient.find(any(ServerQuery.class))).thenThrow(new ConnectionException());
    when(resultHandler.hasResult()).thenReturn(true);
    when(resultHandler.getException()).thenReturn(new ExecuteException("error", 1));

    watcher.execute(mock(DefaultExecutor.class), mock(CommandLine.class));
  }

  @Test(expected = RuntimeException.class)
  public void server_startup_failure_2() throws IOException {
    when(wsClient.find(any(ServerQuery.class))).thenThrow(new ConnectionException());
    when(resultHandler.hasResult()).thenReturn(true);
    when(resultHandler.getExitValue()).thenReturn(1);

    watcher.execute(mock(DefaultExecutor.class), mock(CommandLine.class));
  }

  @Test(expected = RuntimeException.class)
  public void server_startup_failure_500_http_code() throws IOException {
    when(wsClient.find(any(ServerQuery.class))).thenThrow(new ConnectionException("HTTP error: 500 Nuclear Failure"));

    watcher.execute(mock(DefaultExecutor.class), mock(CommandLine.class));
  }

  @Test
  public void wait_until_ws_is_available() throws IOException {
    when(wsClient.find(any(ServerQuery.class)))
      // not started
      .thenThrow(new ConnectionException())
      // ok
      .thenReturn(upServer());
    when(commonsExec.newResultHandler()).thenReturn(resultHandler);
    when(resultHandler.hasResult()).thenReturn(false);

    watcher.execute(mock(DefaultExecutor.class), mock(CommandLine.class));
  }

  @Test
  public void ready_if_db_migration_is_required() throws IOException {
    when(wsClient.find(any(ServerQuery.class))).thenReturn(needMigrationServer());
    when(resultHandler.hasResult()).thenReturn(false);

    watcher.execute(mock(DefaultExecutor.class), mock(CommandLine.class));
  }

  @Test
  public void wait_during_5_minutes() {
    com.sonar.orchestrator.container.Server server = mock(com.sonar.orchestrator.container.Server.class);
    when(server.getUrl()).thenReturn("http://localhost:9000");
    watcher = new ServerWatcher(server);

    assertThat(watcher.retries()).isEqualTo(1500);
    assertThat(watcher.retries() * ServerWatcher.RETRY_TIMEOUT_MS).isEqualTo(ServerWatcher.MAX_TIMEOUT_MS);
  }

  private static Server upServer() {
    return new Server().setStatus(Server.Status.UP);
  }

  private static Server migratingServer() {
    return new Server().setStatus(Server.Status.MIGRATION_RUNNING);
  }

  private static Server needMigrationServer() {
    return new Server().setStatus(Server.Status.SETUP);
  }
}
