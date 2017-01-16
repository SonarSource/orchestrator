/*
 * Orchestrator
 * Copyright (C) 2011-2017 SonarSource SA
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
package com.sonar.orchestrator.dsl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.sonar.orchestrator.Orchestrator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonarqube.ws.WsMeasures;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.measure.ComponentWsRequest;

import static org.sonarqube.ws.WsMeasures.Measure;

public class VerifyCommand extends Command {

  private String resourceKey;
  private Map<String, String> expectedMeasures = new HashMap<>();

  public VerifyCommand(String resourceKey) {
    this.resourceKey = resourceKey;
  }

  public String getResourceKey() {
    return resourceKey;
  }

  public VerifyCommand setExpectedMeasure(String key, String value) {
    expectedMeasures.put(key, value);
    return this;
  }

  public Map<String, String> getExpectedMeasures() {
    return expectedMeasures;
  }

  @Override
  public void execute(Dsl.Context context) {
    Orchestrator orchestrator = context.getOrchestrator();
    Preconditions.checkState(orchestrator != null, "The command 'start server' has not been executed");

    WsClient client = newWsClient(orchestrator);
    verifyMeasures(client);
  }

  @VisibleForTesting
  void verifyMeasures(WsClient client) {
    Map<String, WsMeasures.Measure> measures = execute(client, new ComponentWsRequest().setComponentKey(resourceKey).setMetricKeys(new ArrayList<>(expectedMeasures.keySet())))
      .getComponent().getMeasuresList()
      .stream()
      .collect(Collectors.toMap(Measure::getMetric, Function.identity()));

    for (Map.Entry<String, String> assertion : expectedMeasures.entrySet()) {
      verifyMeasure(measures.get(assertion.getKey()), assertion.getKey(), assertion.getValue());
    }
  }

  private WsMeasures.ComponentWsResponse execute(WsClient client, ComponentWsRequest request) {
    try {
      return client.measures().component(request);
    } catch (HttpException e) {
      if (e.code() == 404) {
        throw new AssertionError("Resource does not exist: " + resourceKey);
      } else {
        throw new AssertionError("Error when getting measures of : " + resourceKey, e);
      }
    }
  }

  private void verifyMeasure(@Nullable Measure measure, String metricKey, String expectedValue) {
    if (measure == null) {
      throw new AssertionError(String.format(
        "Measure mismatch for '%s' on metric '%s'. Expected '%s' but was null.", resourceKey, metricKey, expectedValue));
    }
    Object expected = expectedValue;
    Object got = measure.getValue();
    if (!expected.equals(got)) {
      throw new AssertionError(String.format(
        "Measure mismatch for '%s' on metric '%s'. Expected '%s' but was '%s'.", resourceKey, metricKey, expected, got));
    }
  }

  private static WsClient newWsClient(Orchestrator orchestrator) {
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(orchestrator.getServer().getUrl())
      .build());
  }

}
