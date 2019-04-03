/*
 * Orchestrator
 * Copyright (C) 2011-2019 SonarSource SA
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
package com.sonar.orchestrator.locator;

import com.sonar.orchestrator.http.HttpCall;
import com.sonar.orchestrator.http.HttpClientFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonar.orchestrator.util.OrchestratorUtils.isEmpty;
import static java.lang.String.format;

class URLLocator implements Locator<URLLocation> {

  private static final Logger LOG = LoggerFactory.getLogger(URLLocator.class);

  @Override
  public File locate(URLLocation location) {
    throw new UnsupportedOperationException();
  }

  @Override
  public File copyToDirectory(URLLocation location, File toDir) {
    try {
      if (isHttpRequest(location)) {
        HttpCall httpCall = callHttpRequest(location);
        return httpCall.downloadToDirectory(toDir);
      }

      File toFile = new File(toDir, location.getFileName());
      FileUtils.copyURLToFile(location.getURL(), toFile);
      return toFile;
    } catch (IOException e) {
      throw new IllegalStateException(format("Fail to copy %s to directory: %s", location, toDir), e);
    }
  }

  @Override
  public InputStream openInputStream(URLLocation location) {
    try {
      return location.getURL().openStream();
    } catch (IOException e) {
      throw new IllegalStateException("Fail to open " + location, e);
    }
  }

  @Override
  public File copyToFile(URLLocation location, File toFile) {
    try {
      if (isHttpRequest(location)) {
        HttpCall httpCall = callHttpRequest(location);
        httpCall.downloadToFile(toFile);
      } else {
        FileUtils.copyURLToFile(location.getURL(), toFile);
      }
      return toFile;
    } catch (IOException e) {
      throw new IllegalStateException(format("Fail to copy %s to file: %s", location, toFile), e);
    }
  }

  private static boolean isHttpRequest(URLLocation location) {
    return location.getURL().getProtocol().toLowerCase(Locale.ENGLISH).startsWith("http");
  }

  private static HttpCall callHttpRequest(URLLocation location) {
    LOG.info("Downloading: " + location.getURL());

    return HttpClientFactory.create().newCall(HttpUrl.get(location.getURL()));
  }

  @CheckForNull
  static String getFilenameFromContentDispositionHeader(@Nullable String header) {
    if (isEmpty(header)) {
      return null;
    }
    String filename = header.replaceFirst("(?i)^.*filename=\"([^\"]+)\".*$", "$1");
    if (header.equals(filename)) {
      // strange case on bintray: "attachment; filename = sonar-lits-plugin-0.5.jar"
      filename = StringUtils.substringAfterLast(header, "=");
    }
    if (filename != null) {
      filename = StringUtils.remove(filename, "\"");
      filename = StringUtils.remove(filename, "'");
      filename = StringUtils.remove(filename, ";");
      filename = StringUtils.remove(filename, " ");
    }
    return filename;
  }
}
