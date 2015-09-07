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

import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;

import java.net.URL;

public final class ResourceLocation implements Location {

  private String path;

  private ResourceLocation(String path) {
    Preconditions.checkNotNull(path);
    Preconditions.checkArgument(path.startsWith("/"), "Path must start with slash");
    URL resource = ResourceLocation.class.getResource(path);
    Preconditions.checkNotNull(resource, "Resource not found: " + path);
    this.path = path;
  }

  public static ResourceLocation create(String pathStartingWithSlash) {

    return new ResourceLocation(pathStartingWithSlash);
  }

  public String getPath() {
    return path;
  }

  public String getFilename() {
    return StringUtils.substringAfterLast(path, "/");
  }

  @Override
  public String toString() {
    return getPath();
  }
}
