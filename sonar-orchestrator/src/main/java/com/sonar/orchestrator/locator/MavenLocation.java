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
import com.sonar.orchestrator.version.Version;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

public class MavenLocation implements Location {

  private String groupId;
  private String artifactId;
  private Version version;
  private String filename;
  private String packaging;
  private String classifier;

  public MavenLocation(Builder builder) {
    this.groupId = builder.groupId;
    this.artifactId = builder.artifactId;
    this.version = builder.version;
    this.packaging = builder.packaging;
    this.filename = builder.filename;
    this.classifier = StringUtils.trimToEmpty(builder.classifier);
    if (StringUtils.isBlank(filename)) {
      if (StringUtils.isBlank(classifier)) {
        filename = artifactId + "-" + version.toString() + "." + packaging;
      } else {
        filename = artifactId + "-" + version.toString() + "-" + classifier + "." + packaging;
      }
    }
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public Version version() {
    return version;
  }

  public String getFilename() {
    return filename;
  }

  public String getPackaging() {
    return packaging;
  }

  public String getClassifier() {
    return classifier;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MavenLocation that = (MavenLocation) o;
    return groupId.equals(that.groupId) && artifactId.equals(that.artifactId) && packaging.equals(that.packaging) && version.equals(that.version);
  }

  @Override
  public int hashCode() {
    int result = groupId.hashCode();
    result = 31 * result + artifactId.hashCode();
    result = 31 * result + version.hashCode();
    result = 31 * result + packaging.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return String.format("[%s:%s:%s:%s]", groupId, artifactId, version, packaging);
  }

  public static MavenLocation create(String groupId, String artifactId, String version) {
    return builder().setGroupId(groupId).setArtifactId(artifactId).setVersion(version).build();
  }

  /**
   * @since 2.10.1. Shortcut for {@link #create(String, String, String)}
   */
  public static MavenLocation of(String groupId, String artifactId, String version) {
    return create(groupId, artifactId, version);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder<G extends Builder<G>> {
    private String groupId;
    private String artifactId;
    private Version version;
    private String filename = null;
    private String classifier;
    private String packaging = "jar";

    Builder() {
    }

    public G setGroupId(String groupId) {
      this.groupId = groupId;
      return (G) this;
    }

    public G setArtifactId(String artifactId) {
      this.artifactId = artifactId;
      return (G) this;
    }

    public G setVersion(String version) {
      this.version = Version.create(version);
      return (G) this;
    }

    public G setVersion(Version version) {
      this.version = version;
      return (G) this;
    }

    public G setClassifier(String classifier) {
      this.classifier = classifier;
      return (G) this;
    }

    public G setKey(String groupId, String artifactId, String version) {
      return setGroupId(groupId).setArtifactId(artifactId).setVersion(version);
    }

    public G withFilename(String s) {
      this.filename = s;
      return (G) this;
    }

    public G withPackaging(String s) {
      this.packaging = s;
      return (G) this;
    }

    public MavenLocation build() {
      Preconditions.checkArgument(StringUtils.isNotBlank(groupId), "Group Id must be set");
      Preconditions.checkArgument(StringUtils.isNotBlank(artifactId), "Artifact Id must be set");
      Preconditions.checkArgument(StringUtils.isNotBlank(ObjectUtils.toString(version)), "Version must be set");
      Preconditions.checkArgument(StringUtils.isNotBlank(packaging), "Packaging must be set");
      return new MavenLocation(this);
    }
  }
}
