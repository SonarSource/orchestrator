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
package com.sonar.orchestrator.test;

import org.junit.rules.ExternalResource;

public final class MockHttpServerInterceptor extends ExternalResource {

  private MockHttpServer server;

  @Override
  protected final void before() throws Throwable {
    server = new MockHttpServer();
    server.start();
  }

  @Override
  protected void after() {
    server.stop();
  }

  public void setMockResponseData(String data) {
    server.setMockResponseData(data);
  }

  public void setMockResponseStatus(int status) {
    server.setMockResponseStatus(status);
  }

  public int getPort() {
    return server.getPort();
  }
}
