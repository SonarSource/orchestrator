/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package com.sonar.orchestrator.locator;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import com.sonar.orchestrator.http.HttpCall;
import com.sonar.orchestrator.http.HttpClientFactory;
import okhttp3.HttpUrl;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class GitHubImpl implements GitHub {
  private static final Logger LOG = LoggerFactory.getLogger(GitHubImpl.class);
  private static final Object cacheLock = new Object();
  private static volatile String cachedVersion;
  private final String baseUrl;

  public GitHubImpl() {
    this("https://api.github.com");
  }

  GitHubImpl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  @Override
  public synchronized String getLatestScannerReleaseVersion() {
    // GitHub API has a rate limit of 60 requests per hour for unauthenticated users.
    // To avoid reaching that limit when running integration tests, the returned value needs to be cached.
    synchronized (cacheLock) {
      if (cachedVersion != null) {
        return cachedVersion;
      }
    }
    LOG.info("Retrieving the latest scanner release.");
    HttpUrl url = HttpUrl
      .parse(baseUrl)
      .newBuilder()
      .addPathSegments("repos/SonarSource/sonar-scanner-msbuild/releases/latest")
      .build();

    HttpCall call = HttpClientFactory
      .create()
      .newCall(url)
      // GitHub recommendation: https://docs.github.com/en/rest/releases/releases?apiVersion=2022-11-28#get-the-latest-release
      .setHeader("Accept","application/vnd.github+json")
      .setHeader("X-GitHub-Api-Version", "2022-11-28");

    try {
      File jsonFile = new File("scanner-latest-release.json");

      LOG.info("Downloading {}", url);
      call.downloadToFile(jsonFile);
      LOG.info("SonarScanner for .Net latest release details downloaded to {}", jsonFile);

      String jsonContent = FileUtils.readFileToString(jsonFile, StandardCharsets.UTF_8);
      JsonValue response = Json.parse(jsonContent);
      Files.delete(jsonFile.toPath());
      cachedVersion = response.asObject().getString("tag_name", null);
      return cachedVersion;
  } catch (Exception exception) {
      throw new IllegalStateException("Fail to download the latest release details for SonarScanner for .Net", exception);
    }
  }

  public static synchronized void resetCache() {
    synchronized (cacheLock) {
      cachedVersion = null;
    }
  }
}
