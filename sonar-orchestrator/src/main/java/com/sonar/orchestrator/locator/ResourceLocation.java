/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package com.sonar.orchestrator.locator;

import java.net.URL;
import org.apache.commons.lang.StringUtils;

import static com.sonar.orchestrator.util.OrchestratorUtils.checkArgument;
import static java.util.Objects.requireNonNull;

public class ResourceLocation implements Location {

  private final String path;

  private ResourceLocation(String path) {
    checkArgument(path.startsWith("/"), "Path must start with slash");
    URL resource = ResourceLocation.class.getResource(path);
    requireNonNull(resource, "Resource not found: " + path);
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
