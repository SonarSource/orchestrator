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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

final class URLLocator implements Locator<URLLocation> {

  private static final Logger LOG = LoggerFactory.getLogger(URLLocator.class);

  @Override
  public File locate(URLLocation location) {
    throw new UnsupportedOperationException();
  }

  @Override
  public File copyToDirectory(URLLocation location, File toDir) {
    File toFile = new File(toDir, location.getFileName());
    return copyToFile(location, toFile);
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
      LOG.info("Downloading: " + location.getURL());
      FileUtils.copyURLToFile(location.getURL(), toFile);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to copy " + location + " to file: " + toFile, e);
    }
    return toFile;
  }

}
