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

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

final class FileLocator implements Locator<FileLocation> {
  @Override
  public File locate(FileLocation location) {
    return location.getFile();
  }

  @Override
  public File copyToDirectory(FileLocation fileLocation, File toDir) {
    File file = locate(fileLocation);
    if (file != null && file.exists()) {
      try {
        FileUtils.copyFileToDirectory(file, toDir);
        file = new File(toDir, file.getName());

      } catch (IOException e) {
        throw new IllegalStateException("Fail to copy file to directory: " + toDir, e);
      }
    }
    return file;
  }

  @Override
  public File copyToFile(FileLocation fileLocation, File toFile) {
    File file = locate(fileLocation);
    if (file != null && file.exists()) {
      try {
        FileUtils.copyFile(file, toFile);

      } catch (IOException e) {
        throw new IllegalStateException("Fail to copy to file: " + toFile, e);
      }
    }
    return toFile;
  }

  @Override
  public InputStream openInputStream(FileLocation location) {
    try {
      return new FileInputStream(location.getFile());
    } catch (FileNotFoundException e) {
      throw new IllegalStateException("Fail to open stream of " + location, e);
    }
  }
}
