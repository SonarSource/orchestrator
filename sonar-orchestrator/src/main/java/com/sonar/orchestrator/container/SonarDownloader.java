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
package com.sonar.orchestrator.container;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.util.ZipUtils;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.updatecenter.common.Release;

public class SonarDownloader {
  private static final Logger LOG = LoggerFactory.getLogger(SonarDownloader.class);

  private final FileSystem fileSystem;
  private final Configuration configuration;
  private final Zips zips;

  public SonarDownloader(FileSystem fileSystem, Configuration configuration) {
    this.fileSystem = fileSystem;
    this.configuration = configuration;
    this.zips = new Zips(fileSystem);
  }

  public synchronized File downloadAndUnzip(SonarDistribution distrib) {
    LOG.info("Search " + distrib.zipFilename());

    String key = InstallationKeys.instance().next();
    File toDir = new File(fileSystem.workspace(), key);
    if (toDir.exists()) {
      try {
        FileUtils.cleanDirectory(toDir);
      } catch (IOException e) {
        throw new IllegalStateException("Fail to clean directory: " + toDir, e);
      }
    }

    LOG.info("Download " + distrib.zipFilename() + " in " + toDir.getAbsolutePath());
    File zip = downloadZip(distrib);

    LOG.info("Unzip " + zip);
    ZipUtils.unzip(zip, toDir);

    return new File(toDir, distrib.unzippedDirname());
  }

  public File downloadZip(SonarDistribution distrib) {
    File zip = zips.get(distrib);
    if (zip.exists()) {
      LOG.info("Distribution found: " + zip);
    } else {
      zip = downloadZipToFile(distrib, zip);
      zips.setAsUpToDate(zip);
    }
    return zip;
  }

  private File downloadZipToFile(SonarDistribution distrib, File toFile) {
    File zip = searchInMavenRepositories(distrib, toFile);
    if (zip == null || !zip.isFile()) {
      zip = downloadFromDist(distrib, toFile);
    }
    if (zip == null || !zip.isFile()) {
      throw new IllegalStateException("Can not find " + distrib.zipFilename());
    }
    return zip;
  }

  private File searchInMavenRepositories(SonarDistribution distribution, File toFile) {
    LOG.info("Searching for zip in Maven repositories");
    // search for zip in maven repositories
    MavenLocation location = MavenLocation.builder()
      .setGroupId("org.codehaus.sonar")
      .setArtifactId("sonar-application")
      .setVersion(distribution.version())
      .withPackaging("zip")
      .build();
    File result = fileSystem.copyToFile(location, toFile);
    if (result == null || !result.exists()) {
      location = MavenLocation.builder()
        .setGroupId("org.sonarsource.sonarqube")
        .setArtifactId("sonar-application")
        .setVersion(distribution.version())
        .withPackaging("zip")
        .build();
      result = fileSystem.copyToFile(location, toFile);
    }
    if (result != null && result.exists()) {
      LOG.info("Found zip in Maven repositories");
    }
    return result;
  }

  @VisibleForTesting
  String getDownloadUrl(SonarDistribution distribution) {
    for (Release release : configuration.updateCenter().getSonar().getReleases()) {
      if (release.getVersion().getName().equals(distribution.version().toString())) {
        return release.getDownloadUrl();
      }
    }

    throw new IllegalArgumentException("This version is not listed on update center: " + distribution.version());
  }

  private File downloadFromDist(SonarDistribution distribution, File toFile) {
    if (!distribution.isRelease()) {
      return null;
    }

    String fileUrl = getDownloadUrl(distribution);
    File tempFile = null;
    try {
      FileUtils.forceMkdir(toFile.getParentFile());

      tempFile = File.createTempFile("sonarqube", "zip");
      downloadUrl(fileUrl, tempFile);
      if (!checkMD5(tempFile, fileUrl + ".md5")) {
        throw new IllegalStateException("File downloaded has an incorrect MD5 checksum.");
      }

      if (toFile.exists()) {
        LOG.warn("File {} exists after downloading, returning the existing file", toFile.getAbsolutePath());
      } else {
        Files.move(tempFile, toFile);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to download distribution", e);
    } finally {
      FileUtils.deleteQuietly (tempFile);
    }
    return toFile;
  }

  private static boolean checkMD5(File fromFile, String url) throws IOException {
    return Files.hash(fromFile, com.google.common.hash.Hashing.md5()).toString()
      .equals(Resources.toString(new URL(url), Charset.forName("UTF-8")).trim());
  }

  private static File downloadUrl(String url, File toFile) {
    try {
      FileUtils.forceMkdir(toFile.getParentFile());

      URL u = new URL(url);

      LOG.info("Download: " + u);
      Resources.asByteSource(u).copyTo(Files.asByteSink(toFile));
      LOG.info("Downloaded to: " + toFile);

      return toFile;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
