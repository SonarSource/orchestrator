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
package com.sonar.orchestrator.container;

import com.sonar.orchestrator.config.FileSystem;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

class Zips {
  // Declared as static to share information between orchestrators
  static Set<File> upToDateSnapshots = new HashSet<>();

  private FileSystem fileSystem;

  Zips(FileSystem fileSystem) {
    this.fileSystem = fileSystem;
  }

  File get(SonarDistribution distrib) {
    if (distrib.isRelease()) {
      // releases are kept in user cache
      return new File(fileSystem.sonarInstallsDir(), distrib.zipFilename());
    }
    File snapshotZip = new File(fileSystem.workspace(), distrib.zipFilename());
    if (snapshotZip.exists() && !isUpToDate(snapshotZip)) {
      LoggerFactory.getLogger(Zips.class).info("Delete deprecated zip: " + snapshotZip);
      FileUtils.deleteQuietly(snapshotZip);
    }
    return snapshotZip;
  }

  private static boolean isUpToDate(File snapshotZip) {
    return upToDateSnapshots.contains(snapshotZip);
  }

  void setAsUpToDate(@Nullable File file) {
    if (file != null && file.exists()) {
      upToDateSnapshots.add(file);
    }
  }
}
