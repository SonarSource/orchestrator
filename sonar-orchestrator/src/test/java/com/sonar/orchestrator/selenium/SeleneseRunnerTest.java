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

import com.sonar.orchestrator.config.Configuration;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SeleneseRunnerTest {

  @Test
  public void testResultFile() throws IOException {
    File workspace = new File("target/SeleneseRunnerTest/workspace");
    FileUtils.forceMkdir(workspace);
    Configuration config = Configuration.builder().setProperty("orchestrator.workspaceDir", workspace).build();
    SeleneseRunner runner = new SeleneseRunner(config);

    Selenese selenese = Selenese.builder().setHtmlSuite(createFile("target/SeleneseRunnerTest/testResultFile.html")).build();

    File resultFile = runner.prepareReportFile(selenese);
    assertThat(resultFile.getName()).isEqualTo("report-testResultFile.html");
  }

  private static File createFile(String filename) throws IOException {
    File file = new File(filename);
    FileUtils.forceMkdir(file.getParentFile());
    file.createNewFile();
    return file;
  }
}
