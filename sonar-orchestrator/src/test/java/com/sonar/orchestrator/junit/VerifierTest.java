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
package com.sonar.orchestrator.junit;

import com.sonar.orchestrator.junit.sample.ExcludedSampleTest;
import com.sonar.orchestrator.junit.sample.SampleSuite;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

@RunWith(PropertyFilterRunner.class)
public class VerifierTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void verifier_should_fail_if_missing_test() {
    thrown.expect(IllegalStateException.class);
    thrown
      .expectMessage("Test classes 'class com.sonar.orchestrator.junit.sample.ExcludedSampleTest' must be included in com.sonar.orchestrator.junit.sample.SampleSuite");

    Verifier verifier = Verifier.allTestsInSuite();
    verifier.verify(Description.createSuiteDescription(SampleSuite.class));
  }

  @Test
  public void verifier_should_not_fail_if_missing_test_is_explicitly_excluded() {
    Verifier verifier = Verifier.allTestsInSuite().except(ExcludedSampleTest.class);
    verifier.verify(Description.createSuiteDescription(SampleSuite.class));
  }

}
