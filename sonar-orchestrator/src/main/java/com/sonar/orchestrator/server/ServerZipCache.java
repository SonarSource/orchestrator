/*
 * Orchestrator
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package com.sonar.orchestrator.server;

import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.apache.commons.io.FileUtils;

import static java.lang.String.format;

/**
 * Local cache of SonarQube ZIP files. Releases are stored in a user
 * directory shared between executions of Orchestrator. Snapshots
 * are ephemeral as they are stored in workspace of current orchestrator
 * execution.
 */
public class ServerZipCache {

  private static final Map<Version, File> SNAPSHOTS = new HashMap<>();
  private final FileSystem fs;

  public ServerZipCache(FileSystem fs) {
    this.fs = fs;
  }

  @CheckForNull
  public File get(Version version) {
    if (version.isRelease()) {
      File file = locateRelease(version);
      return file.exists() ? file : null;
    }
    return SNAPSHOTS.get(version);
  }

  public File moveToCache(Version version, File file) {
    File to = addToCache(version, file);
    try {
      FileUtils.moveFile(file, to);
    } catch (IOException e) {
      throw new IllegalStateException(format("Fail to move SonarQube zip [%s] to cache [%s]", file, to), e);
    }
    return to;
  }

  public File copyToCache(Version version, File file) {
    File to = addToCache(version, file);
    try {
      FileUtils.copyFile(file, to);
    } catch (IOException e) {
      throw new IllegalStateException(format("Fail to move SonarQube zip [%s] to cache [%s]", file, to), e);
    }
    return to;
  }

  private File addToCache(Version version, File file) {
    File to;
    if (version.isRelease()) {
      to = locateRelease(version);
    } else {
      // do not rename the file, as it can differ from version
      to = new File(fs.workspace(), file.getName());
      SNAPSHOTS.put(version, to);
    }
    return to;
  }

  public void clear() {
    SNAPSHOTS.clear();
  }

  private File locateRelease(Version version) {
    String filename = format("sonarqube-%s.zip", version.toString());
    return new File(fs.sonarInstallsDir(), filename);
  }
}
