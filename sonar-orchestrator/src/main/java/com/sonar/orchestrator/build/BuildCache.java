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

import com.sonar.orchestrator.util.ZipUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;

import static java.util.Optional.ofNullable;

public class BuildCache {
  private final Map<String, CachedReport> cacheMap = new HashMap<>();

  public void cache(String id, Build<?> build) {
    // TODO implement getScannerReportDirectory for all scanners
    if (!build.getScannerReportDirectory().isPresent() || !Files.isDirectory(build.getScannerReportDirectory().get())) {
      return;
    }
    try {
      Path cachedDir = Files.createTempDirectory("sonar-scanner-report");
      Path reportDir = build.getScannerReportDirectory().get();
      Files.copy(reportDir, cachedDir, StandardCopyOption.REPLACE_EXISTING);
      CachedReport entry = toCacheReport(cachedDir);
      ofNullable(cacheMap.put(id, entry)).ifPresent(e -> FileUtils.deleteQuietly(e.getReportDirectory().toFile()));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public Optional<CachedReport> getCached(String id) {
    // TODO change analysis date before returning entry?
    return Optional.ofNullable(cacheMap.get(id));
  }

  private CachedReport toCacheReport(Path reportDir) {
    // TODO parse from scanner report files
    return new CachedReport(reportDir, "sample", "sample", null, null);
  }

  public void clear() {
    cacheMap.values().forEach(c -> FileUtils.deleteQuietly(c.getReportDirectory().toFile()));
    cacheMap.clear();
  }

  public static class CachedReport {
    private final Path reportDirectory;
    private final String projectKey;
    private final String projectName;
    private final String branchName;
    private final String prKey;

    private CachedReport(Path reportDirectory, String projectKey, String projectName, @Nullable String branchName, @Nullable String prKey) {
      this.reportDirectory = reportDirectory;
      this.projectKey = projectKey;
      this.projectName = projectName;
      this.branchName = branchName;
      this.prKey = prKey;
    }

    public Path getReportDirectory() {
      return reportDirectory;
    }

    public String getProjectKey() {
      return projectKey;
    }

    public String getProjectName() {
      return projectName;
    }

    @CheckForNull
    public String getBranchName() {
      return branchName;
    }

    @CheckForNull
    public String getPrKey() {
      return prKey;
    }
  }
}
