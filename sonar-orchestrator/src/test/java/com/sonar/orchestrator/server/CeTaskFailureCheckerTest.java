/*
 * Orchestrator
 * Copyright (C) SonarSource Sàrl
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

import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.util.Version;
import java.io.IOException;
import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;
import mockwebserver3.junit4.MockWebServerRule;
import okhttp3.Credentials;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CeTaskFailureCheckerTest {

  @Rule
  public MockWebServerRule mockWebServerRule = new MockWebServerRule();

  private final CeTaskFailureChecker underTest = new CeTaskFailureChecker();

  @Test
  public void failIfComputeEngineTaskFailed_whenLogsHaveNoTask_doesNotCallServer() {
    underTest.failIfComputeEngineTaskFailed(newServer(), new BuildResult());

    assertThat(mockWebServerRule.getServer().getRequestCount()).isZero();
  }

  @Test
  public void failIfComputeEngineTaskFailed_whenTaskSucceeded_doesNotFail() throws Exception {
    enqueueTask("task-1", "SUCCESS", null, null, null);

    underTest.failIfComputeEngineTaskFailed(newServer(), resultWithTask("task-1"));

    RecordedRequest request = mockWebServerRule.getServer().takeRequest();
    assertThat(request.getTarget()).contains("/" + CeTaskFailureChecker.TASK_PATH);
    assertThat(request.getTarget()).contains("id=task-1");
    assertThat(request.getTarget()).contains("additionalFields=stacktrace");
    assertThat(request.getHeaders().get("Accept")).isEqualTo("application/json");
    assertThat(request.getHeaders().get("Authorization")).isEqualTo(Credentials.basic("admin", "admin"));
  }

  @Test
  public void failIfComputeEngineTaskFailed_whenTaskFailed_includesErrorLog() throws IOException {
    enqueueTask("AY123", "FAILED", "Invalid new code period", "java.lang.IllegalStateException: Invalid new code period", "TIMEOUT");
    Server server = newServer();
    BuildResult result = resultWithTask("AY123");

    assertThatThrownBy(() -> underTest.failIfComputeEngineTaskFailed(server, result))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("id: AY123")
      .hasMessageContaining("status: FAILED")
      .hasMessageContaining("errorType: TIMEOUT")
      .hasMessageContaining("errorMessage: Invalid new code period")
      .hasMessageContaining("errorStacktrace: java.lang.IllegalStateException: Invalid new code period");
  }

  @Test
  public void failIfComputeEngineTaskFailed_whenSeveralBuilds_reportsEveryFailedTask() throws Exception {
    enqueueTask("ok-task", "SUCCESS", null, null, null);
    enqueueTask("fail-1", "FAILED", "first failure", "stack-1", null);
    enqueueTask("fail-2", "FAILED", "second failure", "stack-2", null);

    Server server = newServer();
    BuildResult first = resultWithTask("ok-task");
    BuildResult second = resultWithTask("fail-1");
    BuildResult third = resultWithTask("fail-2");

    assertThatThrownBy(() -> underTest.failIfComputeEngineTaskFailed(server, first, second, third))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("id: fail-1")
      .hasMessageContaining("errorMessage: first failure")
      .hasMessageContaining("errorStacktrace: stack-1")
      .hasMessageContaining("id: fail-2")
      .hasMessageContaining("errorMessage: second failure")
      .hasMessageContaining("errorStacktrace: stack-2");
    assertThat(mockWebServerRule.getServer().getRequestCount()).isEqualTo(3);
  }

  @Test
  public void failIfComputeEngineTaskFailed_whenSameTaskLoggedTwice_isCheckedOnce() throws Exception {
    enqueueTask("task-1", "SUCCESS", null, null, null);
    BuildResult result = resultWithTask("task-1");
    result.getLogsWriter().append("INFO: More about the report processing at http://localhost:9000/api/ce/task?id=task-1\n");

    underTest.failIfComputeEngineTaskFailed(newServer(), result);

    assertThat(mockWebServerRule.getServer().getRequestCount()).isEqualTo(1);
  }

  private Server newServer() {
    return new Server(null, null, Edition.COMMUNITY, Version.create("7.3.0.1000"), mockWebServerRule.getServer().url(""), 9001, null);
  }

  private void enqueueTask(String taskId, String status, String errorMessage, String errorStacktrace, String errorType) {
    StringBuilder body = new StringBuilder();
    body.append("{\"task\":{\"id\":\"").append(taskId).append("\",\"componentKey\":\"my-project\",\"status\":\"").append(status).append("\"");
    appendJsonField(body, "errorMessage", errorMessage);
    appendJsonField(body, "errorStacktrace", errorStacktrace);
    appendJsonField(body, "errorType", errorType);
    body.append("}}");
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(body.toString()).build());
  }

  private static void appendJsonField(StringBuilder body, String name, String value) {
    if (value != null) {
      body.append(",\"").append(name).append("\":\"").append(value).append("\"");
    }
  }

  private static BuildResult resultWithTask(String taskId) throws IOException {
    BuildResult result = new BuildResult();
    result.getLogsWriter().append("INFO: ANALYSIS SUCCESSFUL\n");
    result.getLogsWriter().append("INFO: More about the report processing at http://localhost:9000/api/ce/task?id=").append(taskId).append("\n");
    return result;
  }

}
