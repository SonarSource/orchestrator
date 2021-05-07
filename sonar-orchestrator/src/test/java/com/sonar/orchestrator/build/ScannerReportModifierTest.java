/*
 * Orchestrator
 * Copyright (C) 2011-2021 SonarSource SA
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
package com.sonar.orchestrator.build;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.scanner.protocol.output.ScannerReportReader;

import static org.assertj.core.api.Assertions.assertThat;

public class ScannerReportModifierTest {
  private final TestSystem2 system2 = new TestSystem2();
  private final ScannerReportModifier scannerReportModifier = new ScannerReportModifier(system2);

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void should_update_analysis_date() throws URISyntaxException, IOException {
    Path scannerReportDir = Paths.get(this.getClass().getResource("BuildCacheTest/scanner-report").toURI());
    File tempDir = temp.newFolder();
    FileUtils.copyDirectory(scannerReportDir.toFile(), tempDir);
    system2.setNow(1000L);

    scannerReportModifier.modifyAnalysisDateInTheReport(tempDir.toPath());
    assertThat(new ScannerReportReader(tempDir).readMetadata().getAnalysisDate()).isEqualTo(1000L);
  }
}
