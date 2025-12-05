/*
 * Orchestrator Build
 * Copyright (C) 2011-2025 SonarSource Sàrl
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
package com.sonar.orchestrator.build.util;

import com.sonar.orchestrator.build.command.Command;
import com.sonar.orchestrator.build.command.CommandExecutor;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

public final class ZipUtils {
  private ZipUtils() {
    // only static methods
  }

  public static void unzip(File zip, File toDir) {
    try {
      if (!toDir.exists()) {
        FileUtils.forceMkdir(toDir);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to create folder " + toDir, e);
    }

    if (SystemUtils.IS_OS_WINDOWS) {
      javaUnzip(zip, toDir);
    } else {
      nativeUnzip(zip, toDir);
    }
  }

  static void javaUnzip(File zip, File toDir) {
    Path targetDirNormalizedPath = toDir.toPath().normalize();
    try {
      try (ZipFile zipFile = new ZipFile(zip)) {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
          ZipEntry entry = entries.nextElement();
          File to = targetDirNormalizedPath.resolve(entry.getName()).toFile();
          verifyInsideTargetDirectory(entry, to.toPath(), targetDirNormalizedPath);
          if (entry.isDirectory()) {
            FileUtils.forceMkdir(to);
          } else {
            File parent = to.getParentFile();
            if (parent != null) {
              FileUtils.forceMkdir(parent);
            }

            try (OutputStream fos = new BufferedOutputStream(Files.newOutputStream(to.toPath()))) {
              IOUtils.copy(zipFile.getInputStream(entry), fos);
            }
          }
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to unzip " + zip + " to " + targetDirNormalizedPath, e);
    }
  }

  private static void nativeUnzip(File zip, File toDir) {
    Command command = Command.create("unzip");
    command.addArgument("-o");
    command.addArgument("-q");
    command.addArgument(zip.getAbsolutePath());
    command.setDirectory(toDir);
    int result = CommandExecutor.create().execute(command, 60L * 60 * 1000);
    if (result != 0) {
      throw new IllegalStateException("Fail to unzip " + zip + " to " + toDir);
    }
  }

  private static void verifyInsideTargetDirectory(ZipEntry entry, Path entryPath, Path targetDirNormalizedPath) {
    if (!entryPath.normalize().startsWith(targetDirNormalizedPath)) {
      // vulnerability - trying to create a file outside the target directory
      throw new IllegalStateException("Unzipping an entry outside the target directory is not allowed: " + entry.getName());
    }
  }
}
