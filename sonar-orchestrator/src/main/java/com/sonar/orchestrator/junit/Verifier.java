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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import javax.annotation.Nonnull;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.Suite.SuiteClasses;
import org.junit.runners.model.Statement;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;

/**
 * This rule is used to verify that all test classes have been added to the global test suite.
 * Usage in the global test suite:
 * <pre>
 *   {@literal @}ClassRule
 *   public static TestRule VERIFIER = Verifier.allTestsInSuite().except(SqaleTest.class);
 * </pre>
 *
 */
public class Verifier implements TestRule {

  private Set<Class> excludedTestClasses = Sets.newHashSet();

  private Verifier() {
  }

  public static Verifier allTestsInSuite() {
    return new Verifier();
  }

  /**
   * Used to explicitly exclude some classes from the test suite.
   */
  public Verifier except(Class<?>... excludedTestClasses) {
    this.excludedTestClasses.addAll(Arrays.asList(excludedTestClasses));
    return this;
  }

  @Override
  public Statement apply(Statement base, Description description) {
    verify(description);
    return base;
  }

  @VisibleForTesting
  public Set<Class> getExcludedTestClasses() {
    return excludedTestClasses;
  }

  @VisibleForTesting
  void verify(Description description) {
    String className = description.getClassName();
    String packageName = className.substring(0, className.lastIndexOf("."));
    Set<Method> testMethods = (Set) new Reflections(new String[] {packageName}, new MethodAnnotationsScanner()).getMethodsAnnotatedWith(Test.class);
    Set<Class> testClasses = Sets.newHashSet(Collections2.transform(testMethods, new Function<Method, Class>() {
      @Override
      public Class apply(@Nonnull Method method) {
        return method.getDeclaringClass();
      }
    }));

    SuiteClasses annotation = description.getAnnotation(SuiteClasses.class);
    Set<Class> includedTestClasses = Sets.newHashSet(excludedTestClasses);
    if (annotation != null) {
      includedTestClasses.addAll(Arrays.asList(annotation.value()));
    }
    Sets.SetView<Class> diff = Sets.difference(testClasses, includedTestClasses);
    if (!diff.isEmpty()) {
      throw new IllegalStateException(String.format("Test classes '%s' must be included in %s", Joiner.on(", ").join(diff.toArray()), description.getClassName()));
    }
  }
}
