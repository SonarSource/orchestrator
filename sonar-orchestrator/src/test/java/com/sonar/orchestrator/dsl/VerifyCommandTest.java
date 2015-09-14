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
package com.sonar.orchestrator.dsl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Model;
import org.sonar.wsclient.services.Query;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import javax.annotation.Nullable;

import java.util.List;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VerifyCommandTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void should_match_numeric_measures() {
    VerifyCommand command = new VerifyCommand("struts");
    command.setExpectedMeasure("ncloc", "250");
    command.setExpectedMeasure("coverage", "60.75");
    Sonar client = mock(Sonar.class);
    when(client.find(argThat(new QueryMatcher("struts", new String[]{"ncloc", "coverage"}))))
      .thenReturn(newResource(250, 60.75, null));
    command.verifyMeasures(client);
  }

  @Test
  public void should_not_match_int_measures() {
    thrown.expect(AssertionError.class);
    thrown.expectMessage("Measure mismatch for 'struts' on metric 'ncloc'. Expected '251.0' but was '250.0'.");

    VerifyCommand command = new VerifyCommand("struts");
    command.setExpectedMeasure("ncloc", "251");
    command.setExpectedMeasure("coverage", "60.75");
    Sonar client = mock(Sonar.class);
    when(client.find(argThat(new QueryMatcher("struts", new String[]{"ncloc", "coverage"}))))
      .thenReturn(newResource(250, 60.75, null));
    command.verifyMeasures(client);
  }

  @Test
  public void should_not_match_double_measures() {
    thrown.expect(AssertionError.class);
    thrown.expectMessage("Measure mismatch for 'struts' on metric 'coverage'. Expected '60.75' but was '20.0'.");

    VerifyCommand command = new VerifyCommand("struts");
    command.setExpectedMeasure("ncloc", "250");
    command.setExpectedMeasure("coverage", "60.75");
    Sonar client = mock(Sonar.class);
    when(client.find(argThat(new QueryMatcher("struts", new String[]{"ncloc", "coverage"}))))
      .thenReturn(newResource(250, 20.0, null));
    command.verifyMeasures(client);
  }

  @Test
  public void should_not_match_missing_double_measure() {
    thrown.expect(AssertionError.class);
    thrown.expectMessage("Measure mismatch for 'struts' on metric 'coverage'. Expected '60.75' but was null.");

    VerifyCommand command = new VerifyCommand("struts");
    command.setExpectedMeasure("ncloc", "250");
    command.setExpectedMeasure("coverage", "60.75");
    Sonar client = mock(Sonar.class);
    when(client.find(argThat(new QueryMatcher("struts", new String[]{"ncloc", "coverage"}))))
      .thenReturn(newResource(250, null, null));
    command.verifyMeasures(client);
  }

  @Test
  public void should_not_match_missing_resource() {
    thrown.expect(AssertionError.class);
    thrown.expectMessage("Resource does not exist: struts");

    VerifyCommand command = new VerifyCommand("struts");
    command.setExpectedMeasure("ncloc", "250");
    command.setExpectedMeasure("coverage", "60.75");
    Sonar client = mock(Sonar.class);
    when(client.find(argThat(new QueryMatcher("struts", new String[]{"ncloc", "coverage"})))).thenReturn(null);
    command.verifyMeasures(client);
  }

  @Test
  public void should_match_string_measure() {
    VerifyCommand command = new VerifyCommand("struts");
    command.setExpectedMeasure("ncloc", "250");
    command.setExpectedMeasure("profile", "Sonar way");
    Sonar client = mock(Sonar.class);
    when(client.find(argThat(new QueryMatcher("struts", new String[]{"ncloc", "profile"}))))
      .thenReturn(newResource(250, null, "Sonar way"));
    command.verifyMeasures(client);
  }

  @Test
  public void should_not_match_string_measure() {
    thrown.expect(AssertionError.class);
    thrown.expectMessage("Measure mismatch for 'struts' on metric 'profile'. Expected 'Sonar way' but was 'Other profile'.");

    VerifyCommand command = new VerifyCommand("struts");
    command.setExpectedMeasure("ncloc", "250");
    command.setExpectedMeasure("profile", "Sonar way");
    Sonar client = mock(Sonar.class);
    when(client.find(argThat(new QueryMatcher("struts", new String[]{"ncloc", "profile"}))))
      .thenReturn(newResource(250, null, "Other profile"));
    command.verifyMeasures(client);
  }

  static class QueryMatcher extends BaseMatcher<Query<Model>> {
    private String[] metrics;
    private String resourceKey;

    QueryMatcher(String resourceKey, String[] metrics) {
      this.resourceKey = resourceKey;
      this.metrics = metrics;
    }

    @Override
    public boolean matches(Object o) {
      ResourceQuery query = (ResourceQuery) o;
      return resourceKey.equals(query.getResourceKeyOrId()) && Sets.newHashSet(metrics).equals(Sets.newHashSet(query.getMetrics()));
    }

    @Override
    public void describeTo(Description description) {
    }
  }

  Resource newResource(int ncloc, @Nullable Double coverage, @Nullable String profile) {
    Resource resource = new Resource();
    List<Measure> measures = Lists.newArrayList();
    measures.add(new Measure().setMetricKey("ncloc").setValue(new Double(ncloc)));
    if (coverage != null) {
      measures.add(new Measure().setMetricKey("coverage").setValue(new Double(coverage)));
    }
    if (profile != null) {
      measures.add(new Measure().setMetricKey("profile").setData(profile));
    }
    resource.setMeasures(measures);
    return resource;
  }
}
