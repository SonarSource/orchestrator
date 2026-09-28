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

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.container.Server;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * Fails when an analysis submitted by {@code Orchestrator#executeBuilds()} ends with a Compute Engine error,
 * and reports the task error log.
 */
public class CeTaskFailureChecker {

  private static final Logger LOGGER = LoggerFactory.getLogger(CeTaskFailureChecker.class);

  static final String TASK_PATH = "api/ce/task";
  private static final String JSON = "application/json";
  private static final Pattern TASK_ID_IN_LOG = Pattern.compile("api/ce/task\\?id=(\\S+)");
  private static final Set<String> FAILURE_STATUSES = Set.of("FAILED");
  private static final List<String> FAILURE_FIELDS = List.of("id", "componentKey", "status", "errorType", "errorMessage", "errorStacktrace");

  public void failIfComputeEngineTaskFailed(Server server, BuildResult... results) {
    requireNonNull(server, "server is null");
    List<String> failures = new ArrayList<>();
    for (String taskId : extractTaskIds(results)) {
      JsonObject task = fetchTask(server, taskId);
      String status = getRequiredField(task, "status", taskId);
      if (FAILURE_STATUSES.contains(status)) {
        failures.add(formatFailure(task));
      }
    }
    if (!failures.isEmpty()) {
      String error = "Compute Engine task failed:" + System.lineSeparator()
        + String.join(System.lineSeparator() + System.lineSeparator(), failures);
      LOGGER.error(error);
      throw new IllegalStateException(error);
    }
  }

  private static Set<String> extractTaskIds(BuildResult[] results) {
    Set<String> taskIds = new LinkedHashSet<>();
    for (BuildResult result : results) {
      List<String> logLines = result.getLogsLines(logLine -> logLine.contains("api/ce/task?id="));
      for (String line : logLines) {
        Matcher matcher = TASK_ID_IN_LOG.matcher(line);
        while (matcher.find()) {
          taskIds.add(matcher.group(1));
        }
      }
    }
    return taskIds;
  }

  private static JsonObject fetchTask(Server server, String taskId) {
    String body = server.newHttpCall(TASK_PATH)
      .setAdminCredentials()
      .setHeader("Accept", JSON)
      .setParam("id", taskId)
      .setParam("additionalFields", "stacktrace")
      .execute()
      .getBodyAsString();
    try {
      JsonValue parsed = Json.parse(body);
      JsonValue task = parsed.isObject() ? parsed.asObject().get("task") : null;
      if (task == null || !task.isObject()) {
        throw new IllegalStateException("Unexpected response for Compute Engine task '" + taskId + "': " + body);
      }
      return task.asObject();
    } catch (ParseException e) {
      throw new IllegalStateException("Cannot parse Compute Engine task '" + taskId + "': " + body, e);
    }
  }

  private static String getRequiredField(JsonObject task, String field, String taskId) {
    String value = optionalString(task, field);
    if (value == null) {
      throw new IllegalStateException("Compute Engine task '" + taskId + "' response has no '" + field + "'");
    }
    return value;
  }

  @Nullable
  private static String optionalString(JsonObject object, String name) {
    JsonValue value = object.get(name);
    if (value == null || value.isNull()) {
      return null;
    }
    return value.asString();
  }

  private static String formatFailure(JsonObject task) {
    StringBuilder error = new StringBuilder();
    for (String field : FAILURE_FIELDS) {
      String value = optionalString(task, field);
      if (value != null) {
        if (!error.isEmpty()) {
          error.append(System.lineSeparator());
        }
        error.append(field).append(": ").append(value);
      }
    }
    return error.toString();
  }

}
