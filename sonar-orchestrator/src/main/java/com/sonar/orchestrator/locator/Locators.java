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

import com.google.common.annotations.VisibleForTesting;
import com.sonar.orchestrator.config.Configuration;

import java.io.File;
import java.io.InputStream;

public class Locators {

  private final FileLocator fileLocator;
  private final MavenLocator mavenLocator;
  private final ResourceLocator resourceLocator;
  private final URLLocator urlLocator;
  private final PluginLocator pluginLocator;

  public Locators(Configuration config) {
    fileLocator = new FileLocator();
    mavenLocator = new MavenLocator(config);
    resourceLocator = new ResourceLocator();
    urlLocator = new URLLocator();
    pluginLocator = new PluginLocator(config, mavenLocator, urlLocator);
  }

  @VisibleForTesting
  Locators(FileLocator fileLocator, MavenLocator mavenLocator, ResourceLocator resourceLocator, URLLocator urlLocator, PluginLocator pluginLocator) {
    this.fileLocator = fileLocator;
    this.mavenLocator = mavenLocator;
    this.resourceLocator = resourceLocator;
    this.urlLocator = urlLocator;
    this.pluginLocator = pluginLocator;
  }

  public File locate(Location location) {
    File file;
    if (location instanceof PluginLocation) {
      file = pluginLocator.locate((PluginLocation) location);
    } else if (location instanceof FileLocation) {
      file = fileLocator.locate((FileLocation) location);
    } else if (location instanceof MavenLocation) {
      file = mavenLocator.locate((MavenLocation) location);
    } else if (location instanceof ResourceLocation) {
      file = resourceLocator.locate((ResourceLocation) location);
    } else if (location instanceof URLLocation) {
      file = urlLocator.locate((URLLocation) location);
    } else {
      throw throwNotSupported(location);
    }
    return file;
  }

  private IllegalArgumentException throwNotSupported(Location location) {
    return new IllegalArgumentException("Unknown location type: " + location.getClass());
  }

  /**
   * Copy file to directory.
   *
   * @return the copied file in the target directory, null if the file can not be found
   */
  public File copyToDirectory(Location location, File toDir) {
    File file;
    if (location instanceof PluginLocation) {
      file = pluginLocator.copyToDirectory((PluginLocation) location, toDir);
    } else if (location instanceof FileLocation) {
      file = fileLocator.copyToDirectory((FileLocation) location, toDir);
    } else if (location instanceof MavenLocation) {
      file = mavenLocator.copyToDirectory((MavenLocation) location, toDir);
    } else if (location instanceof ResourceLocation) {
      file = resourceLocator.copyToDirectory((ResourceLocation) location, toDir);
    } else if (location instanceof URLLocation) {
      file = urlLocator.copyToDirectory((URLLocation) location, toDir);
    } else {
      throw throwNotSupported(location);
    }
    return file;
  }

  public File copyToFile(Location location, File toFile) {
    File file;
    if (location instanceof PluginLocation) {
      file = pluginLocator.copyToFile((PluginLocation) location, toFile);
    } else if (location instanceof FileLocation) {
      file = fileLocator.copyToFile((FileLocation) location, toFile);
    } else if (location instanceof MavenLocation) {
      file = mavenLocator.copyToFile((MavenLocation) location, toFile);
    } else if (location instanceof ResourceLocation) {
      file = resourceLocator.copyToFile((ResourceLocation) location, toFile);
    } else if (location instanceof URLLocation) {
      file = urlLocator.copyToFile((URLLocation) location, toFile);
    } else {
      throw throwNotSupported(location);
    }
    return file;
  }

  public InputStream openInputStream(Location location) {
    InputStream input;
    if (location instanceof PluginLocation) {
      input = pluginLocator.openInputStream((PluginLocation) location);
    } else if (location instanceof FileLocation) {
      input = fileLocator.openInputStream((FileLocation) location);
    } else if (location instanceof MavenLocation) {
      input = mavenLocator.openInputStream((MavenLocation) location);
    } else if (location instanceof ResourceLocation) {
      input = resourceLocator.openInputStream((ResourceLocation) location);
    } else if (location instanceof URLLocation) {
      input = urlLocator.openInputStream((URLLocation) location);
    } else {
      throw throwNotSupported(location);
    }
    return input;
  }

}
