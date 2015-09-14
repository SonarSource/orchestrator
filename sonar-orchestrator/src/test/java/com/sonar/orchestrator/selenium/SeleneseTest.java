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
package com.sonar.orchestrator.selenium;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class SeleneseTest {

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailIfNoFiles() {
    Selenese.builder().build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailIfSuiteDoesNotExist() {
    File unknownFile = new File("target/SeleneseTest/unknown.html");
    Selenese.builder().setHtmlSuite(unknownFile).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailIfTestDoesNotExist() {
    File unknownFile = new File("target/SeleneseTest/unknown.html");
    Selenese.builder().setHtmlTests("my suite", unknownFile).build();
  }

  @Test
  public void shouldUseHtmlTests() throws IOException {
    File test1 = createFile("target/SeleneseTest/test1.html");
    File test2 = createFile("target/SeleneseTest/test2.html");
    Selenese selenese = Selenese.builder().setHtmlTests("my suite", test1, test2).build();

    assertThat(selenese.getSuiteName()).isEqualTo("my suite");
    assertThat(selenese.getHtmlTests()).hasSize(2);
  }

  @Test
  public void shouldUseHtmlSuite() throws IOException {
    File suite = createFile("target/SeleneseTest/shouldUseHtmlSuite.html");
    Selenese selenese = Selenese.builder().setHtmlSuite(suite).build();

    assertThat(selenese.getSuiteName()).isEqualTo("shouldUseHtmlSuite");
    assertThat(selenese.getHtmlSuite()).exists();
    assertThat(selenese.getHtmlTests()).isNull();
  }

  private static File createFile(String filename) throws IOException {
    File file = new File(filename);
    FileUtils.forceMkdir(file.getParentFile());
    file.createNewFile();
    return file;
  }

}
