/*
 * Orchestrator Locators
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
package com.sonar.orchestrator.locator;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.config.FileSystem;
import java.io.File;
import java.io.InputStream;

public class Locators {

  private final FileLocator fileLocator;
  private final MavenLocator mavenLocator;
  private final ResourceLocator resourceLocator;
  private final URLLocator urlLocator;

  public Locators(Configuration configuration) {
    this(configuration.fileSystem(), ArtifactoryFactory.createArtifactory(configuration));
  }

  Locators(FileSystem fileSystem, Artifactory artifactory) {
    fileLocator = new FileLocator();
    mavenLocator = new MavenLocator(fileSystem, artifactory);
    resourceLocator = new ResourceLocator();
    urlLocator = new URLLocator();
  }

  Locators(FileLocator fileLocator, MavenLocator mavenLocator, ResourceLocator resourceLocator, URLLocator urlLocator) {
    this.fileLocator = fileLocator;
    this.mavenLocator = mavenLocator;
    this.resourceLocator = resourceLocator;
    this.urlLocator = urlLocator;
  }

  public MavenLocator maven() {
    return mavenLocator;
  }

  public File locate(Location location) {
    File file;
    if (location instanceof FileLocation fileLocation) {
      file = fileLocator.locate(fileLocation);
    } else if (location instanceof MavenLocation mavenLocation) {
      file = mavenLocator.locate(mavenLocation);
    } else if (location instanceof ResourceLocation resourceLocation) {
      file = resourceLocator.locate(resourceLocation);
    } else if (location instanceof URLLocation urlLocation) {
      file = urlLocator.locate(urlLocation);
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
    if (location instanceof FileLocation fileLocation) {
      file = fileLocator.copyToDirectory(fileLocation, toDir);
    } else if (location instanceof MavenLocation mavenLocation) {
      file = mavenLocator.copyToDirectory(mavenLocation, toDir);
    } else if (location instanceof ResourceLocation resourceLocation) {
      file = resourceLocator.copyToDirectory(resourceLocation, toDir);
    } else if (location instanceof URLLocation urlLocation) {
      file = urlLocator.copyToDirectory(urlLocation, toDir);
    } else {
      throw throwNotSupported(location);
    }
    return file;
  }

  public File copyToFile(Location location, File toFile) {
    File file;
    if (location instanceof FileLocation fileLocation) {
      file = fileLocator.copyToFile(fileLocation, toFile);
    } else if (location instanceof MavenLocation mavenLocation) {
      file = mavenLocator.copyToFile(mavenLocation, toFile);
    } else if (location instanceof ResourceLocation resourceLocation) {
      file = resourceLocator.copyToFile(resourceLocation, toFile);
    } else if (location instanceof URLLocation urlLocation) {
      file = urlLocator.copyToFile(urlLocation, toFile);
    } else {
      throw throwNotSupported(location);
    }
    return file;
  }

  public InputStream openInputStream(Location location) {
    InputStream input;
    if (location instanceof FileLocation fileLocation) {
      input = fileLocator.openInputStream(fileLocation);
    } else if (location instanceof MavenLocation mavenLocation) {
      input = mavenLocator.openInputStream(mavenLocation);
    } else if (location instanceof ResourceLocation resourceLocation) {
      input = resourceLocator.openInputStream(resourceLocation);
    } else if (location instanceof URLLocation urlLocation) {
      input = urlLocator.openInputStream(urlLocation);
    } else {
      throw throwNotSupported(location);
    }
    return input;
  }

}
