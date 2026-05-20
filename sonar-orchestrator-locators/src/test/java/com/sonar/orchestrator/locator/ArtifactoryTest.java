/*
 * Orchestrator Locators
 * Copyright (C) SonarSource Sàrl
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ArtifactoryTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private final Artifactory underTest = new Artifactory(null, "https://example.com", null, null) {
    @Override
    public Optional<String> resolveVersion(MavenLocation location) {
      throw new UnsupportedOperationException();
    }

    @Override
    protected boolean doDownload(MavenLocation location, Path destination) {
      throw new UnsupportedOperationException();
    }
  };

  @Test
  public void moveFile_moves_source_into_target() throws IOException {
    Path source = temp.newFile().toPath();
    Files.writeString(source, "payload", StandardCharsets.UTF_8);
    Path target = new File(temp.newFolder(), "target.jar").toPath();

    underTest.moveFile(source, target);

    assertThat(target).exists().hasContent("payload");
    assertThat(source).doesNotExist();
  }

  @Test
  public void moveFile_creates_missing_parent_directory_of_target() throws IOException {
    Path source = temp.newFile().toPath();
    Files.writeString(source, "payload", StandardCharsets.UTF_8);
    Path missingParent = temp.newFolder().toPath().resolve("nested").resolve("dir");
    Path target = missingParent.resolve("target.jar");

    underTest.moveFile(source, target);

    assertThat(target).exists().hasContent("payload");
    assertThat(missingParent).isDirectory();
  }

  @Test
  public void moveFile_returns_silently_when_target_already_exists_as_concurrent_winner() throws IOException {
    Path source = temp.newFolder().toPath().resolve("missing-source.tmp");
    Path target = new File(temp.newFolder(), "target.jar").toPath();
    Files.writeString(target, "already_there", StandardCharsets.UTF_8);

    underTest.moveFile(source, target);

    assertThat(target).hasContent("already_there");
    assertThat(source).doesNotExist();
  }

  @Test
  public void moveFile_throws_ISE_when_source_does_not_exist_and_target_does_not_exist() throws IOException {
    Path source = temp.newFolder().toPath().resolve("missing-source.tmp");
    Path target = new File(temp.newFolder(), "missing-target.jar").toPath();

    assertThatThrownBy(() -> underTest.moveFile(source, target))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageStartingWith("Fail to move file " + target);
  }
}
