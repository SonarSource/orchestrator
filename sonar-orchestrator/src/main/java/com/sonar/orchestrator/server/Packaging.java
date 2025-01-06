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
package com.sonar.orchestrator.server;

import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import javax.annotation.concurrent.Immutable;

@Immutable
public class Packaging {
  private final Edition edition;
  private final Version version;
  private final File zip;

  public Packaging(Edition edition, Version version, File zip) {
    this.edition = edition;
    this.version = version;
    this.zip = zip;
  }

  public Edition getEdition() {
    return edition;
  }

  public Version getVersion() {
    return version;
  }

  public File getZip() {
    return zip;
  }
}
