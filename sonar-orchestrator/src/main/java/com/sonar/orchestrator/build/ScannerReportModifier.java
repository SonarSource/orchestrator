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

import java.nio.file.Path;
import org.sonar.api.utils.System2;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.scanner.protocol.output.ScannerReportWriter;

public class ScannerReportModifier {
  private final System2 system;

  public ScannerReportModifier() {
    this(System2.INSTANCE);
  }

  public ScannerReportModifier(System2 system) {
    this.system = system;
  }

  public void modifyAnalysisDateInTheReport(Path reportDir) {
    ScannerReportReader reader = new ScannerReportReader(reportDir.toFile());
    ScannerReport.Metadata metadata = reader.readMetadata();

    ScannerReportWriter writer = new ScannerReportWriter(reportDir.toFile());

    ScannerReport.Metadata tamperedMetadata = ScannerReport.Metadata.newBuilder(metadata).setAnalysisDate(system.now()).build();

    writer.writeMetadata(tamperedMetadata);
  }
}
