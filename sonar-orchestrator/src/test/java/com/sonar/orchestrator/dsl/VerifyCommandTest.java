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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarqube.ws.WsMeasures;
import org.sonarqube.ws.WsMeasures.Measure;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.measure.ComponentWsRequest;
import org.sonarqube.ws.client.measure.MeasuresService;

import static com.google.common.collect.Sets.newHashSet;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VerifyCommandTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private WsClient client = mock(WsClient.class);
  private MeasuresService measuresService = mock(MeasuresService.class);

  @Before
  public void setUp() throws Exception {
    when(client.measures()).thenReturn(measuresService);
  }

  @Test
  public void should_match_numeric_measures() {
    VerifyCommand command = new VerifyCommand("struts");
    command.setExpectedMeasure("ncloc", "250");
    command.setExpectedMeasure("coverage", "60.75");
    when(measuresService.component(argThat(new ComponentWsRequestMatcher("struts", "ncloc", "coverage"))))
      .thenReturn(newComponentWsResponse(250, 60.75, null));
    command.verifyMeasures(client);
  }

  @Test
  public void should_not_match_int_measures() {
    thrown.expect(AssertionError.class);
    thrown.expectMessage("Measure mismatch for 'struts' on metric 'ncloc'. Expected '251' but was '250'.");

    VerifyCommand command = new VerifyCommand("struts");
    command.setExpectedMeasure("ncloc", "251");
    command.setExpectedMeasure("coverage", "60.75");
    when(measuresService.component(argThat(new ComponentWsRequestMatcher("struts", "ncloc", "coverage"))))
      .thenReturn(newComponentWsResponse(250, 60.75, null));
    command.verifyMeasures(client);
  }

  @Test
  public void should_not_match_double_measures() {
    thrown.expect(AssertionError.class);
    thrown.expectMessage("Measure mismatch for 'struts' on metric 'coverage'. Expected '60.75' but was '20.0'.");

    VerifyCommand command = new VerifyCommand("struts");
    command.setExpectedMeasure("ncloc", "250");
    command.setExpectedMeasure("coverage", "60.75");
    when(measuresService.component(argThat(new ComponentWsRequestMatcher("struts", "ncloc", "coverage"))))
      .thenReturn(newComponentWsResponse(250, 20.0, null));
    command.verifyMeasures(client);
  }

  @Test
  public void should_not_match_missing_double_measure() {
    thrown.expect(AssertionError.class);
    thrown.expectMessage("Measure mismatch for 'struts' on metric 'coverage'. Expected '60.75' but was null.");

    VerifyCommand command = new VerifyCommand("struts");
    command.setExpectedMeasure("ncloc", "250");
    command.setExpectedMeasure("coverage", "60.75");
    when(measuresService.component(argThat(new ComponentWsRequestMatcher("struts", "ncloc", "coverage"))))
      .thenReturn(newComponentWsResponse(250, null, null));
    command.verifyMeasures(client);
  }

  @Test
  public void should_match_string_measure() {
    VerifyCommand command = new VerifyCommand("struts");
    command.setExpectedMeasure("ncloc", "250");
    command.setExpectedMeasure("profile", "Sonar way");
    when(measuresService.component(argThat(new ComponentWsRequestMatcher("struts", "ncloc", "profile"))))
      .thenReturn(newComponentWsResponse(250, null, "Sonar way"));
    command.verifyMeasures(client);
  }

  @Test
  public void should_not_match_string_measure() {
    thrown.expect(AssertionError.class);
    thrown.expectMessage("Measure mismatch for 'struts' on metric 'profile'. Expected 'Sonar way' but was 'Other profile'.");

    VerifyCommand command = new VerifyCommand("struts");
    command.setExpectedMeasure("ncloc", "250");
    command.setExpectedMeasure("profile", "Sonar way");
    when(measuresService.component(argThat(new ComponentWsRequestMatcher("struts", "ncloc", "profile"))))
      .thenReturn(newComponentWsResponse(250, null, "Other profile"));
    command.verifyMeasures(client);
  }

  @Test
  public void should_not_match_missing_resource() {
    thrown.expect(AssertionError.class);
    thrown.expectMessage("Resource does not exist: struts");

    VerifyCommand command = new VerifyCommand("struts");
    command.setExpectedMeasure("ncloc", "250");
    command.setExpectedMeasure("coverage", "60.75");

    doThrow(new HttpException("http://localhost/api/mesures?componentKey=struts&metricKeys=ncloc,ccoverage", 404)).when(measuresService).component(any(ComponentWsRequest.class));
    command.verifyMeasures(client);
  }

  @Test
  public void fail_when_unknown_error() {
    thrown.expect(AssertionError.class);
    thrown.expectMessage("Error when getting measures of : struts");

    VerifyCommand command = new VerifyCommand("struts");
    command.setExpectedMeasure("ncloc", "250");
    command.setExpectedMeasure("coverage", "60.75");

    doThrow(new HttpException("http://localhost/api/mesures?componentKey=struts&metricKeys=ncloc,ccoverage", 400)).when(measuresService).component(any(ComponentWsRequest.class));
    command.verifyMeasures(client);
  }

  static class ComponentWsRequestMatcher extends BaseMatcher<ComponentWsRequest> {
    private String[] metrics;
    private String componentKey;

    ComponentWsRequestMatcher(String componentKey, String... metrics) {
      this.componentKey = componentKey;
      this.metrics = metrics;
    }

    @Override
    public boolean matches(Object o) {
      ComponentWsRequest query = (ComponentWsRequest) o;
      return componentKey.equals(query.getComponentKey()) && newHashSet(metrics).equals(newHashSet(query.getMetricKeys()));
    }

    @Override
    public void describeTo(Description description) {
    }
  }

  WsMeasures.ComponentWsResponse newComponentWsResponse(int ncloc, @Nullable Double coverage, @Nullable String profile) {
    List<Measure> measures = new ArrayList<>();
    measures.add(Measure.newBuilder().setMetric("ncloc").setValue(Integer.toString(ncloc)).build());
    if (coverage != null) {
      measures.add(Measure.newBuilder().setMetric("coverage").setValue(Double.toString(coverage)).build());
    }
    if (profile != null) {
      measures.add(Measure.newBuilder().setMetric("profile").setValue(profile).build());
    }
    WsMeasures.ComponentWsResponse.Builder response = WsMeasures.ComponentWsResponse.newBuilder();
    response.setComponent(WsMeasures.Component.newBuilder().addAllMeasures(measures));
    return response.build();
  }
}
