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
package com.sonar.orchestrator.config;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Licenses {
  private static final Logger LOG = LoggerFactory.getLogger(Licenses.class);

  private final String rootUrl;
  private final Map<String, String> cache;

  Licenses(String rootUrl) {
    Preconditions.checkArgument(StringUtils.isNotBlank(rootUrl), "Blank root URL");

    this.rootUrl = rootUrl;
    this.cache = new HashMap<>();
  }

  public Licenses() {
    this("https://raw.githubusercontent.com/SonarSource/licenses/master/it/");
  }

  private static String findGithubToken() {
    return Configuration.createEnv().getString("github.token", System.getenv("GITHUB_TOKEN"));
  }

  private String downloadFromGithub(String pluginKey) {
    String url = rootUrl + pluginKey + ".txt";
    DefaultHttpClient client = new DefaultHttpClient();
    try {
      HttpGet request = new HttpGet(url);
      request.addHeader("Authorization", "token " + findGithubToken());
      LoggerFactory.getLogger(getClass()).info("Requesting license " + request.getURI());
      return StringUtils.defaultString(client.execute(request, new BasicResponseHandler()));
    } catch (ClientProtocolException e) {
      LOG.debug("Exception hold ", e);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to request license: " + url, e);
    } finally {
      client.close();
    }

    return "";
  }

  @CheckForNull
  public String get(String pluginKey) {
    if (!cache.containsKey(pluginKey)) {
      cache.put(pluginKey, downloadFromGithub(pluginKey));
    }
    return cache.get(pluginKey);
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
