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
    this.fileLocator = new FileLocator();
    this.mavenLocator = new MavenLocator(config);
    this.resourceLocator = new ResourceLocator();
    this.urlLocator = new URLLocator();
    this.pluginLocator = new PluginLocator(config, mavenLocator, urlLocator);
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
    if (location instanceof PluginLocation) {
      return pluginLocator.locate((PluginLocation) location);
    }
    if (location instanceof FileLocation) {
      return fileLocator.locate((FileLocation) location);
    }
    if (location instanceof MavenLocation) {
      return mavenLocator.locate((MavenLocation) location);
    }
    if (location instanceof ResourceLocation) {
      return resourceLocator.locate((ResourceLocation) location);
    }
    if (location instanceof URLLocation) {
      return urlLocator.locate((URLLocation) location);
    }

    throw throwNotSupported(location);
  }

  /**
   * Copy file to directory.
   *
   * @return the copied file in the target directory, null if the file can not be found
   */
  public File copyToDirectory(Location location, File toDir) {
    if (location instanceof PluginLocation) {
      return pluginLocator.copyToDirectory((PluginLocation) location, toDir);
    }
    if (location instanceof FileLocation) {
      return fileLocator.copyToDirectory((FileLocation) location, toDir);
    }
    if (location instanceof MavenLocation) {
      return mavenLocator.copyToDirectory((MavenLocation) location, toDir);
    }
    if (location instanceof ResourceLocation) {
      return resourceLocator.copyToDirectory((ResourceLocation) location, toDir);
    }
    if (location instanceof URLLocation) {
      return urlLocator.copyToDirectory((URLLocation) location, toDir);
    }

    throw throwNotSupported(location);
  }

  public File copyToFile(Location location, File toFile) {
    if (location instanceof PluginLocation) {
      return pluginLocator.copyToFile((PluginLocation) location, toFile);
    }
    if (location instanceof FileLocation) {
      return fileLocator.copyToFile((FileLocation) location, toFile);
    }
    if (location instanceof MavenLocation) {
      return mavenLocator.copyToFile((MavenLocation) location, toFile);
    }
    if (location instanceof ResourceLocation) {
      return resourceLocator.copyToFile((ResourceLocation) location, toFile);
    }
    if (location instanceof URLLocation) {
      return urlLocator.copyToFile((URLLocation) location, toFile);
    }

    throw throwNotSupported(location);
  }

  public InputStream openInputStream(Location location) {
    if (location instanceof PluginLocation) {
      return pluginLocator.openInputStream((PluginLocation) location);
    }
    if (location instanceof FileLocation) {
      return fileLocator.openInputStream((FileLocation) location);
    }
    if (location instanceof MavenLocation) {
      return mavenLocator.openInputStream((MavenLocation) location);
    }
    if (location instanceof ResourceLocation) {
      return resourceLocator.openInputStream((ResourceLocation) location);
    }
    if (location instanceof URLLocation) {
      return urlLocator.openInputStream((URLLocation) location);
    }

    throw throwNotSupported(location);
  }

  private static IllegalArgumentException throwNotSupported(Location location) {
    return new IllegalArgumentException("Unknown location type: " + location.getClass());
  }
}
