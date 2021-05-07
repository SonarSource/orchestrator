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

import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.http.HttpCall;
import com.sonar.orchestrator.http.HttpMethod;
import com.sonar.orchestrator.http.HttpResponse;
import com.sonar.orchestrator.util.ZipUtils;
import java.io.IOException;
import org.sonar.api.utils.Preconditions;

public class ScannerReportSubmitter {
  private final Server server;

  ScannerReportSubmitter(Server server) {
    this.server = server;
  }

  public BuildResult submit(BuildCache.CachedReport cachedReport) {
    Preconditions.checkArgument(cachedReport.getPrKey() == null || cachedReport.getBranchName() == null, "Cannot be pull request and branch at the same time");
    try {
      byte[] zippedReport = ZipUtils.zipDir(cachedReport.getReportDirectory().toFile());
      HttpCall httpCall = server.newHttpCall("api/ce/submit")
        .setMultipartContent(zippedReport)
        .setMethod(HttpMethod.MULTIPART_SCANNER_REPORT)
        .setParam("projectKey", cachedReport.getProjectKey())
        .setParam("projectName", cachedReport.getProjectName())
        .setAdminCredentials();
      if (cachedReport.getBranchName() != null) {
        httpCall.setParam("branch", cachedReport.getBranchName());
        httpCall.setParam("branchType", "BRANCH");
      } else if (cachedReport.getPrKey() != null) {
        httpCall.setParam("pullRequest", cachedReport.getPrKey());
      }
      HttpResponse response = httpCall.execute();
      return new BuildResult().addStatus(response.isSuccessful() ? 0 : 1);
    } catch(IOException e) {
      throw new IllegalStateException("Failed to zip scanner report", e);
    }
  }
}
