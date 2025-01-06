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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.List;

@JacksonXmlRootElement(localName = "metadata")
public class MavenRepositoryVersion {

  @JacksonXmlProperty(localName = "groupId")
  private String groupId;
  @JacksonXmlProperty(localName = "artifactId")
  private String artifactId;
  @JacksonXmlProperty(localName = "versioning")
  private Versioning versioning;

  public Versioning getVersioning() {
    return versioning;
  }

  public void setVersioning(Versioning versioning) {
    this.versioning = versioning;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public static class Versioning {
    @JacksonXmlProperty(localName = "latest")
    private String latest;

    @JacksonXmlProperty(localName = "release")
    private String release;

    @JacksonXmlProperty(localName = "versions")
    private List<String> versions = new ArrayList<>();

    @JacksonXmlProperty(localName = "lastUpdated")
    private String lastUpdated;

    public List<String> getVersions() {
      return versions;
    }

    public void setVersions(List<String> versions) {
      this.versions = versions;
    }

    public String getLatest() {
      return latest;
    }

    public String getRelease() {
      return release;
    }
  }
}
