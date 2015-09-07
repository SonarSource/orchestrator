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
package com.sonar.orchestrator.locator;

import com.sonar.orchestrator.config.Configuration;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MavenLocator implements Locator<MavenLocation> {
  private static final Logger LOG = LoggerFactory.getLogger(MavenLocator.class);
  private Configuration config;

  public MavenLocator(Configuration config) {
    this.config = config;
  }

  @Override
  public File locate(MavenLocation location) {
    File result = locateInLocalRepositoryAndLog(location);
    if (result == null) {
      File tempFile = new File(getTempDir(), location.getFilename());
      result = downloadToFile(location, tempFile);
    }
    return result;
  }

  private File downloadToFile(MavenLocation location, File toFile) {
    if (config.fileSystem().mavenNexusUrl() == null || StringUtils.isBlank(config.fileSystem().mavenNexusRepository())) {
      return null;
    }
    File result = null;
    URL url = null;
    HttpURLConnection connection = null;
    try {
      url = getRemoteLocation(location);
      LOG.info("Downloading: " + url);
      connection = openConnection(url);
      int code = connection.getResponseCode();
      if (code == 200) {
        downloadToFile(location, toFile, connection);
        result = toFile;

      } else if (code == 404) {
        logNotFound(location);
      } else {
        throw new IllegalStateException(String.format("Can not download: %s [status: %d, message= %s, followRedirects= %s]",
          url, code, connection.getResponseMessage(), HttpURLConnection.getFollowRedirects()));
      }

    } catch (IOException e) {
      throw new IllegalStateException("Can not download: " + url, e);

    } finally {
      disconnectQuietly(connection);
    }
    return result;
  }

  private static void downloadToFile(MavenLocation location, File toFile, HttpURLConnection connection) throws IOException {
    logFound(location);
    // Support parallel downloads :
    // 1. download to a unique file : filename.1319012959244
    File tempFile = new File(toFile.getAbsolutePath() + "." + System.currentTimeMillis());
    FileUtils.copyInputStreamToFile(connection.getInputStream(), tempFile);

    // 2. rename the temp file
    if (!toFile.exists() || FileUtils.deleteQuietly(toFile)) {
      FileUtils.moveFile(tempFile, toFile);
    }
  }

  private URL getRemoteLocation(MavenLocation location) throws MalformedURLException {
    return appendPathToUrl(nexusQueryPath(config.fileSystem().mavenNexusRepository(), location), config.fileSystem().mavenNexusUrl());
  }

  private static HttpURLConnection openConnection(URL url) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    connection.setDoOutput(false);
    connection.setDoInput(true);
    connection.setAllowUserInteraction(true);
    connection.setInstanceFollowRedirects(true);
    connection.setReadTimeout(10 * 60000);
    if (url.getUserInfo() != null) {
      connection.setRequestProperty("Authorization", "Basic " + Base64.encodeBase64URLSafeString(url.getUserInfo().getBytes()));
    }
    return connection;
  }

  static URL appendPathToUrl(String path, URL baseUrl) throws MalformedURLException {
    return new URL(StringUtils.removeEnd(baseUrl.toString(), "/") + "/" + path);
  }

  private File locateInLocalRepository(MavenLocation location) {
    if (config.fileSystem().mavenLocalRepository() != null && config.fileSystem().mavenLocalRepository().exists()) {
      File file = new File(config.fileSystem().mavenLocalRepository(), path(location));
      if (file.exists()) {
        return file;
      }
    }
    return null;
  }

  private File locateInLocalRepositoryAndLog(MavenLocation location) {
    File result = locateInLocalRepository(location);
    if (result != null) {
      LOG.info("Found {} in maven local repository at {}", location, result);
    }
    return result;
  }

  @Override
  public File copyToDirectory(MavenLocation location, File toDir) {
    File target = locateInLocalRepositoryAndLog(location);
    if (target != null) {
      try {
        FileUtils.copyFileToDirectory(target, toDir);
        return new File(toDir, target.getName());
      } catch (IOException e) {
        throw new IllegalStateException("Fail to copy file to dir: " + toDir, e);
      }
    }
    return downloadToFile(location, new File(toDir, location.getFilename()));
  }

  @Override
  public File copyToFile(MavenLocation location, File toFile) {
    File target = locateInLocalRepositoryAndLog(location);
    if (target != null) {
      try {
        FileUtils.copyFile(target, toFile);
        return toFile;
      } catch (IOException e) {
        throw new IllegalStateException("Fail to copy to file: " + toFile, e);
      }
    }
    return downloadToFile(location, toFile);
  }

  private static File getTempDir() {
    return new File(System.getProperty("java.io.tmpdir"));
  }

  @Override
  public InputStream openInputStream(MavenLocation location) {
    File target = locateInLocalRepositoryAndLog(location);
    if (target != null) {
      return openFromLocalRepository(target);
    } else {
      return openFromRemoteRepository(location);
    }
  }

  private InputStream openFromRemoteRepository(MavenLocation location) {
    // remote repository
    HttpURLConnection connection = null;
    URL url = null;
    try {
      url = getRemoteLocation(location);
      LOG.info("Open stream: " + url);
      connection = openConnection(url);
      int code = connection.getResponseCode();
      if (code == 200) {
        logFound(location);

        // in-memory copy
        byte[] bytes = IOUtils.toByteArray(connection.getInputStream());
        return new ByteArrayInputStream(bytes);

      } else if (code == 404) {
        logNotFound(location);
      } else {
        throw new IllegalStateException(String.format("Can not download: %s [status: %d, message= %s]", url, code, connection.getResponseMessage()));
      }

    } catch (IOException e) {
      throw new IllegalStateException("Can not open: " + url, e);

    } finally {
      disconnectQuietly(connection);
    }
    return null;
  }

  private static InputStream openFromLocalRepository(File target) {
    try {
      return FileUtils.openInputStream(target);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to open " + target, e);
    }
  }

  private static void logNotFound(MavenLocation location) {
    LOG.info("Not found {} in maven remote repository", location);
  }

  private static void logFound(MavenLocation location) {
    LOG.info("Found {} in maven remote repository", location);
  }

  private static String path(MavenLocation location) {
    return StringUtils.replace(location.getGroupId(), ".", "/") + "/" + location.getArtifactId() + "/" + location.version() + "/" + location.getFilename();
  }

  private static String nexusQueryPath(String repoName, MavenLocation location) {
    return String.format("service/local/artifact/maven/redirect?r=%s&g=%s&a=%s&v=%s&e=%s&c=%s", repoName, location.getGroupId(), location.getArtifactId(),
      location.version().toString(), location.getPackaging(), location.getClassifier());
  }

  private static void disconnectQuietly(@Nullable HttpURLConnection connection) {
    if (connection != null) {
      connection.disconnect();
    }
  }
}
