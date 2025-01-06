/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
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

import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.http.HttpClientFactory;
import com.sonar.orchestrator.http.HttpResponse;
import com.sonar.orchestrator.version.Version;
import java.util.EnumMap;
import java.util.Map;
import okhttp3.HttpUrl;
import org.apache.commons.lang.StringUtils;

import static com.sonar.orchestrator.util.OrchestratorUtils.defaultIfNull;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class Licenses {

  private static final String TOKEN_PROPERTY = "github.token";
  private static final String TOKEN_ENV_VARIABLE = "GITHUB_TOKEN";

  private final Configuration configuration;
  private final String baseUrl;
  private final Map<Edition, String> licensesPerEdition = new EnumMap<>(Edition.class);

  Licenses(Configuration configuration, String baseUrl) {
    this.configuration = configuration;
    this.baseUrl = baseUrl;
  }

  public Licenses(Configuration configuration) {
    this(configuration, "https://raw.githubusercontent.com/SonarSource/licenses/");
  }

  public String getLicense(Edition edition, Version version) {
    if (!version.isGreaterThanOrEquals(7, 9)) {
      throw new IllegalArgumentException(String.format("Commercial licenses of SonarQube %s are no longer supported", version));
    }
    return licensesPerEdition.computeIfAbsent(edition, e -> {
      String filename;
      switch (e) {
        case DEVELOPER:
          filename = "de.txt";
          break;
        case ENTERPRISE:
          filename = "ee.txt";
          break;
        case DATACENTER:
          filename = "dce.txt";
          break;
        default:
          throw new IllegalStateException("License does not exist for edition " + e);
      }
      return download(baseUrl + "master/edition_testing/" + filename);
    });
  }

  private String download(String url) {
    HttpResponse response = HttpClientFactory.create().newCall(HttpUrl.parse(url))
      .setHeader("Authorization", "token " + loadGithubToken())
      .executeUnsafely();
    if (response.isSuccessful()) {
      return cleanUpLicense(response.getBodyAsString());
    }
    throw new IllegalStateException(format("Fail to download license. URL [%s] returned code [%d]", url, response.getCode()));
  }

  private static String cleanUpLicense(String body) {
    String s = defaultIfNull(body, "");
    if (s.contains("-")) {
      s = StringUtils.substringAfterLast(s, "-");
    }
    return StringUtils.trim(s);
  }

  private String loadGithubToken() {
    String token = configuration.getString(TOKEN_PROPERTY, configuration.getString(TOKEN_ENV_VARIABLE));
    requireNonNull(token, () -> format("Please provide your GitHub token with the property %s or the env variable %s", TOKEN_PROPERTY, TOKEN_ENV_VARIABLE));
    return token;
  }
}
