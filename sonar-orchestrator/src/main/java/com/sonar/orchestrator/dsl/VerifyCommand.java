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
import java.util.HashMap;
import java.util.Map;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

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

    Sonar client = orchestrator.getServer().getWsClient();
    verifyMeasures(client);
  }

  @VisibleForTesting
  void verifyMeasures(Sonar client) {
    ResourceQuery query = ResourceQuery.create(resourceKey);
    query.setMetrics((String[]) expectedMeasures.keySet().toArray(new String[expectedMeasures.size()]));
    Resource resource = client.find(query);

    if (resource == null) {
      throw new AssertionError("Resource does not exist: " + resourceKey);
    }

    for (Map.Entry<String, String> assertion : expectedMeasures.entrySet()) {
      verifyMeasure(resource, assertion.getKey(), assertion.getValue());
    }
  }

  private void verifyMeasure(Resource resource, String metricKey, String expectedValue) {
    Measure measure = resource.getMeasure(metricKey);
    if (measure == null) {
      throw new AssertionError(String.format(
        "Measure mismatch for '%s' on metric '%s'. Expected '%s' but was null.", resourceKey, metricKey, expectedValue
      ));
    }
    Object expected = expectedValue;
    Object got = measure.getData();
    try {
      expected = Double.valueOf(expectedValue);
      got = measure.getValue();
    } catch (NumberFormatException e) {
      // expected is not a numeric value, check below will fail
    }
    if (!expected.equals(got)) {
      throw new AssertionError(String.format(
          "Measure mismatch for '%s' on metric '%s'. Expected '%s' but was '%s'.", resourceKey, metricKey, expected, got
      ));
    }
  }

}
