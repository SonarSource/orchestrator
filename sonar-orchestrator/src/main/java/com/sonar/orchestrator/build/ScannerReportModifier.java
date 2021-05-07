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

import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.scanner.protocol.output.ScannerReportWriter;

public class ScannerReportModifier {

  public static void modifyAnalysisDateInTheReport(BuildCache.CachedReport report) {
    ScannerReportReader reader = new ScannerReportReader(report.getReportDirectory().toFile());
    ScannerReport.Metadata metadata = reader.readMetadata();

    ScannerReportWriter writer = new ScannerReportWriter(report.getReportDirectory().toFile());

    ScannerReport.Metadata tamperedMetadata = ScannerReport.Metadata.newBuilder(metadata).setAnalysisDate(System.currentTimeMillis()).build();

    writer.writeMetadata(tamperedMetadata);
  }
}
