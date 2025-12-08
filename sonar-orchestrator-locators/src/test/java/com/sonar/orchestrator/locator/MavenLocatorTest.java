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

import com.sonar.orchestrator.config.FileSystem;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;

public class MavenLocatorTest {

  private final FileSystem fileSystem = Mockito.mock(FileSystem.class, Mockito.RETURNS_DEEP_STUBS);
  private final Artifactory artifactory = Mockito.mock(Artifactory.class, Mockito.RETURNS_DEEP_STUBS);
  private final MavenLocator underTest = new MavenLocator(fileSystem, artifactory);
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    File cacheDir = temp.newFolder();
    Mockito.when(fileSystem.getCacheDir()).thenReturn(cacheDir.toPath());
  }

  @Test
  public void find_in_cache() throws Exception {
    MavenLocation location = MavenLocation.of("foo", "bar", "1.0");
    File cachedFile = fileSystem.getCacheDir().resolve(MavenLocator.cacheKeyOf(location)).resolve("the_file.jar").toFile();
    FileUtils.touch(cachedFile);

    File file = underTest.locateResolvedVersion(location);

    assertThat(file).exists().hasName("the_file.jar");
    Mockito.verifyNoInteractions(artifactory);
  }

  @Test
  public void find_in_maven_local_repository_if_defined() throws IOException {
    File localRepository = temp.newFolder();
    FileUtils.touch(new File(localRepository, "foo/bar/1.0/bar-1.0.jar"));
    Mockito.when(fileSystem.mavenLocalRepository()).thenReturn(localRepository.toPath());
    markAsAbsentFromArtifactory();

    File file = underTest.locateResolvedVersion(MavenLocation.of("foo", "bar", "1.0"));
    assertThat(file).exists().hasName("bar-1.0.jar");

    Mockito.verifyNoInteractions(artifactory);
    // do not copy in cache
    verifyEmptyCache();
  }

  @Test
  public void search_but_not_find_in_maven_local_repository_if_defined() throws IOException {
    File localRepository = temp.newFolder();
    FileUtils.touch(new File(localRepository, "foo/bar/1.0/bar-1.0.jar"));
    Mockito.when(fileSystem.mavenLocalRepository()).thenReturn(localRepository.toPath());
    markAsAbsentFromArtifactory();

    File file = underTest.locateResolvedVersion(MavenLocation.of("foo", "bar", "1.1"));
    assertThat(file).isNull();
    verifyEmptyCache();
  }

  @Test
  public void download_from_artifactory_and_add_to_cache() throws Exception {
    Mockito.when(artifactory.downloadToFile(ArgumentMatchers.any(), ArgumentMatchers.any())).thenAnswer((Answer<Boolean>) invocationOnMock -> {
      File file = (File) invocationOnMock.getArguments()[1];
      FileUtils.write(file, "content of file", StandardCharsets.UTF_8);
      return true;
    });

    File file = underTest.locateResolvedVersion(MavenLocation.of("foo", "bar", "1.1"));
    assertThat(file)
      .exists()
      .isFile()
      .usingCharset(StandardCharsets.UTF_8)
      .hasContent("content of file");
    assertThat(file.getAbsolutePath()).startsWith(fileSystem.getCacheDir().toFile().getCanonicalPath());
  }

  private void verifyEmptyCache() {
    assertThat(fileSystem.getCacheDir()).isEmptyDirectory();
  }

  private void markAsAbsentFromArtifactory() {
    Mockito.when(artifactory.downloadToFile(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(false);
  }

  private void markVersionsAsResolved() {
    Mockito.when(artifactory.resolveVersion(ArgumentMatchers.any())).thenAnswer((Answer<Optional<String>>) invocationOnMock -> {
      MavenLocation location = (MavenLocation) invocationOnMock.getArguments()[0];
      return Optional.of(location.getVersion());
    });
  }

  @Test
  public void copyToDirectory_existing_artifact() throws IOException {
    MavenLocation location = MavenLocation.of("foo", "bar", "1.0");
    File cachedFile = new File(fileSystem.getCacheDir() + File.separator + MavenLocator.cacheKeyOf(location), "the_file.jar");
    FileUtils.touch(cachedFile);
    markVersionsAsResolved();
    File toDir = temp.newFolder();

    File file = underTest.copyToDirectory(location, toDir);

    assertThat(file)
      .exists()
      .isFile()
      .hasName("the_file.jar")
      .hasParent(toDir);
  }

  @Test
  public void copyToDirectory_returns_null_if_artifact_not_found() throws IOException {
    MavenLocation location = MavenLocation.of("foo", "bar", "1.0");
    markVersionsAsResolved();
    File toDir = temp.newFolder();
    File result = underTest.copyToDirectory(location, toDir);

    assertThat(result).isNull();
    assertThat(toDir).isEmptyDirectory();
  }

  @Test
  public void copyToFile_existing_artifact() throws IOException {
    MavenLocation location = MavenLocation.of("foo", "bar", "1.0");
    File cachedFile = new File(fileSystem.getCacheDir() + File.separator + MavenLocator.cacheKeyOf(location), "the_file.jar");
    FileUtils.touch(cachedFile);
    markVersionsAsResolved();
    File toFile = temp.newFile();

    File result = underTest.copyToFile(location, toFile);

    assertThat(result)
      .exists()
      .isFile()
      .isEqualTo(toFile);
  }

  @Test
  public void copyToFile_returns_null_if_artifact_not_found() throws IOException {
    MavenLocation location = MavenLocation.of("foo", "bar", "1.0");
    markVersionsAsResolved();
    File toFile = temp.newFile();
    File result = underTest.copyToFile(location, toFile);

    assertThat(result).isNull();
  }

  @Test
  public void openInputStream_existing_artifact() throws IOException {
    MavenLocation location = MavenLocation.of("foo", "bar", "1.0");
    File cachedFile = new File(fileSystem.getCacheDir() + File.separator + MavenLocator.cacheKeyOf(location), "the_file.jar");
    FileUtils.write(cachedFile, "content", StandardCharsets.UTF_8);
    markVersionsAsResolved();

    try (InputStream result = underTest.openInputStream(location)) {
      assertThat(result).isNotNull();
      assertThat(IOUtils.toString(result, StandardCharsets.UTF_8)).isEqualTo("content");
    }
  }

  @Test
  public void openInputStream_returns_null_if_artifact_not_found() {
    MavenLocation location = MavenLocation.of("foo", "bar", "1.0");
    markVersionsAsResolved();

    assertThat(underTest.openInputStream(location)).isNull();
  }
}
