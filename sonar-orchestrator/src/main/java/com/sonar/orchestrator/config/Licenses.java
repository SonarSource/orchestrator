/*
 * Orchestrator
 * Copyright (C) 2011-2017 SonarSource SA
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
package com.sonar.orchestrator.config;

import com.sonar.orchestrator.http.HttpClientFactory;
import com.sonar.orchestrator.http.HttpResponse;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import okhttp3.HttpUrl;

import static com.sonar.orchestrator.util.OrchestratorUtils.checkArgument;
import static com.sonar.orchestrator.util.OrchestratorUtils.defaultIfNull;
import static com.sonar.orchestrator.util.OrchestratorUtils.isEmpty;
import static java.lang.String.format;

public class Licenses {

  private final String rootUrl;
  private final Map<String, String> cacheV2;
  private String cacheV3;

  Licenses(String rootUrl) {
    checkArgument(!isEmpty(rootUrl), "Blank root URL");

    this.rootUrl = rootUrl;
    this.cacheV2 = new HashMap<>();
  }

  public Licenses() {
    this("https://raw.githubusercontent.com/SonarSource/licenses/");
  }

  private static String findGithubToken() {
    return Configuration.createEnv().getString("github.token", System.getenv("GITHUB_TOKEN"));
  }

  private String downloadV2FromGithub(String pluginKey) {
    String url = rootUrl + "master/it/" + pluginKey + ".txt";
    return downloadFromGitHub(pluginKey, url);
  }

  private String downloadV3FromGithub() {
    String url = rootUrl + "master/it/dev.txt";
    return downloadFromGitHub("v3", url);
  }

  private static String downloadFromGitHub(String licenseKey, String url) {
    HttpResponse response = HttpClientFactory.create().newCall(HttpUrl.parse(url))
      .setHeader("Authorization", "token " + findGithubToken())
      .executeUnsafely();
    if (response.isSuccessful()) {
      return defaultIfNull(response.getBodyAsString(), "");
    }
    if (response.getCode() == HttpURLConnection.HTTP_NOT_FOUND) {
      return "";
    }
    throw new IllegalStateException(format("Fail to download development license [%s]. URL [%s] returned code [%d]", licenseKey, url, response.getCode()));
  }

  @CheckForNull
  public String get(String pluginKey) {
    if (!cacheV2.containsKey(pluginKey)) {
      cacheV2.put(pluginKey, downloadV2FromGithub(pluginKey));
    }
    return cacheV2.get(pluginKey);
  }

  @CheckForNull
  public String getV3() {
    if (cacheV3 == null) {
      cacheV3 = downloadV3FromGithub();
    }
    return cacheV3;
  }

  public String licensePropertyKey(String pluginKey) {
    switch (pluginKey) {
      case "cobol":
      case "natural":
      case "plsql":
      case "vb":
        return "sonarsource." + pluginKey + ".license.secured";
      case "sqale":
        return "sqale.license.secured";
      default:
        return "sonar." + pluginKey + ".license.secured";
    }
  }
}
