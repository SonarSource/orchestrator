/*
 * Orchestrator
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package com.sonar.orchestrator.selenium;

import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;

/**
 * Selenium HTML tests, generally written with Selenium IDE
 */
public final class Selenese {

  private File htmlSuite;
  private File[] htmlTests;

  // used only when htmlTests are set
  private String suiteName;

  public Selenese(Builder builder) {
    this.htmlSuite = builder.htmlSuite;
    this.htmlTests = builder.htmlTests;
    this.suiteName = builder.suiteName;
  }

  public File getHtmlSuite() {
    return htmlSuite;
  }

  public File[] getHtmlTests() {
    return htmlTests;
  }

  public String getSuiteName() {
    return suiteName;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private File htmlSuite;
    private File[] htmlTests;
    private String suiteName;

    private Builder() {
    }

    public Builder setHtmlSuite(File htmlSuite) {
      this.suiteName = FilenameUtils.getBaseName(htmlSuite.getName());
      this.htmlSuite = htmlSuite;
      return this;
    }

    public Builder setHtmlTests(String suiteName, File... htmlTests) {
      this.suiteName = suiteName;
      this.htmlTests = htmlTests;
      return this;
    }

    public Builder setHtmlTestsInClasspath(String suiteName, String... htmlTestPaths) {
      this.suiteName = suiteName;
      this.htmlTests = new File[htmlTestPaths.length];
      for (int index = 0; index < htmlTestPaths.length; index++) {
        htmlTests[index] = FileUtils.toFile(getClass().getResource(htmlTestPaths[index]));
      }
      return this;
    }

    public Selenese build() {
      if (htmlSuite != null) {
        checkPresence(htmlSuite);
      }
      if (htmlTests != null) {
        for (File htmlTest : htmlTests) {
          checkPresence(htmlTest);
        }
      }
      if (htmlSuite == null && (htmlTests == null || htmlTests.length == 0)) {
        throw new IllegalArgumentException("HTML suite or tests are missing");
      }
      return new Selenese(this);
    }

    private static void checkPresence(@Nullable File file) {
      if (file == null || !file.isFile() || !file.exists()) {
        throw new IllegalArgumentException("HTML file does not exist: " + file);
      }
    }
  }

  public static Selenese createForHtmlSuite(File htmlSuite) {
    return new Builder().setHtmlSuite(htmlSuite).build();
  }
}
