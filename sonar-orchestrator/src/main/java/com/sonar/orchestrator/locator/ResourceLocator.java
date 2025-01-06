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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;

class ResourceLocator implements Locator<ResourceLocation> {

  @Override
  public File locate(ResourceLocation location) {
    throw new UnsupportedOperationException();
  }

  @Override
  public File copyToDirectory(ResourceLocation location, File toDir) {
    return copyToFile(location, new File(toDir, location.getFilename()));
  }

  @Override
  public InputStream openInputStream(ResourceLocation location) {
    return ResourceLocator.class.getResourceAsStream(location.getPath());
  }

  @Override
  public File copyToFile(ResourceLocation location, File toFile) {
    try (InputStream input = ResourceLocator.class.getResourceAsStream(location.getPath());
      OutputStream output = new FileOutputStream(toFile, false)) {
      IOUtils.copyLarge(input, output);
      return toFile;
    } catch (IOException e) {
      throw new IllegalStateException("Fail to copy " + location + " to file: " + toFile, e);
    }
  }
}
