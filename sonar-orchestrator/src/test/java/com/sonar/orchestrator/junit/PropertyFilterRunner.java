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

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import java.util.Arrays;

public class PropertyFilterRunner extends BlockJUnit4ClassRunner {
  private static final String EMBEDDED = "embedded";

  /**
   * Creates a BlockJUnit4ClassRunner to run {@code klass}
   *
   * @param klass
   * @throws org.junit.runners.model.InitializationError if the test class is malformed.
   */
  public PropertyFilterRunner(Class<?> klass) throws InitializationError {
    super(klass);
  }

  @Override
  protected boolean isIgnored(FrameworkMethod child) {
    return super.isIgnored(child) || shouldIgnoreMethod(child);
  }

  private boolean shouldIgnoreMethod(FrameworkMethod method) {
    if (System.getProperty("database") == null || System.getProperty("database").equals(EMBEDDED)) {
      return false;
    }

    TestOnlyIf annotation = method.getAnnotation(TestOnlyIf.class);
    if (annotation == null) {
      // Look at the annotation on the class if it does exist on the method
      annotation = method.getDeclaringClass().getAnnotation(TestOnlyIf.class);
    }
    if (annotation == null) {
      return true;
    }

    boolean result = true;
    if (annotation.os().length > 0) {
      result &= ! Arrays.asList(annotation.os()).contains(System.getProperty("operatingsystem"));
    }
    if (annotation.database().length > 0) {
      result &= ! Arrays.asList(annotation.database()).contains(System.getProperty("database"));
    }
    return result;
  }
}
