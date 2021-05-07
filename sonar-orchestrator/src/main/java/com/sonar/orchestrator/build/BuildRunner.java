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

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.container.Server;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

public class BuildRunner {

  public static final String SONAR_HOST_URL = "sonar.host.url";
  private final Server server;
  private final Configuration config;
  private final BuildCache buildCache = new BuildCache();
  private final ScannerReportSubmitter scannerReportSubmitter;
  private final ScannerReportModifier scannerReportModifier = new ScannerReportModifier();

  public BuildRunner(Server server, Configuration config) {
    this.server = server;
    this.config = config;
    this.scannerReportSubmitter = new ScannerReportSubmitter(server);
  }

  public BuildResult run(Build<?> build) {
    return run(build, null);
  }

  public BuildResult run(Build<?> build, @Nullable String cacheId) {
    BuildResult result = runQuietly(build, cacheId);
    if (!result.isSuccess()) {
      throw new BuildFailureException(build, result);
    }
    return result;
  }

  public BuildResult runQuietly(Build<?> build) {
    return runQuietly(build, null);
  }

  public BuildResult runQuietly(Build<?> build, @Nullable String cacheId) {
    if (cacheId != null) {
      return runCached(build, cacheId);
    } else {
      return build.execute(config, adjustProperties(build));
    }
  }

  private BuildResult runCached(Build<?> build, String cacheId) {
    Optional<BuildCache.CachedReport> cached = buildCache.getCached(cacheId);
    if (cached.isPresent()) {
      BuildCache.CachedReport cachedReport = cached.get();
      scannerReportModifier.modifyAnalysisDateInTheReport(cachedReport.getReportDirectory());
      return scannerReportSubmitter.submit(cachedReport);
    }
    BuildResult result = build.execute(config, adjustProperties(build));
    if (result.isSuccess()) {
      buildCache.cache(cacheId, build);
    }
    return result;
  }

  Map<String, String> adjustProperties(Build<?> build) {
    Map<String, String> adjustedProperties = new HashMap<>();
    if (!(build instanceof ScannerForMSBuild) || !build.arguments().contains("end")) {
      adjustedProperties.put(SONAR_HOST_URL, server.getUrl());
      adjustedProperties.put("sonar.scm.disabled", "true");
      adjustedProperties.put("sonar.branch.autoconfig.disabled", "true");
      adjustedProperties.put("sonar.scanner.keepReport", "true");
    }
    // build properties override predefined properties
    adjustedProperties.putAll(build.getProperties());

    return adjustedProperties;
  }

  public void clearCache() {
    buildCache.clear();
  }
}
