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

import com.google.common.collect.ImmutableMap;
import com.sonar.orchestrator.PropertyAndEnvTest;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static com.sonar.orchestrator.TestModules.setEnv;
import static org.assertj.core.api.Assertions.assertThat;

public class FileLocationTest extends PropertyAndEnvTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void shouldCreateWithFile() {
    File file = new File("target/foo.txt");
    FileLocation location = FileLocation.of(file);
    assertThat(location.getFile()).isEqualTo(file);
  }

  @Test
  public void testEquals() {
    File file1 = new File("target/one.txt");
    File file2 = new File("target/two.txt");
    assertThat(FileLocation.of(file1)).isEqualTo(FileLocation.of(file1));
    assertThat(FileLocation.of(file1)).isNotEqualTo(FileLocation.of(file2));
    assertThat(FileLocation.of(file2)).isNotEqualTo(FileLocation.of(file1));
  }

  @Test
  public void ofShared() throws URISyntaxException {
    URL url = getClass().getResource("/com/sonar/orchestrator/locator/FileLocationTest/index.txt");
    FileLocation location = FileLocation.ofShared("abap/foo.txt", FilenameUtils.getFullPath(url.toURI().getPath()));
    assertThat(location.getFile().isFile()).isTrue();
    assertThat(location.getFile().exists()).isTrue();
    assertThat(location.getFile().getName()).isEqualTo("foo.txt");
  }

  @Test
  public void ofSharedWithEnv() throws URISyntaxException {
    URL url = getClass().getResource("/com/sonar/orchestrator/locator/FileLocationTest/index.txt");

    Map<String, String> envVariables = new HashMap<>(System.getenv());
    envVariables.put("SONAR_IT_SOURCES", FilenameUtils.getFullPath(url.toURI().getPath()));
    setEnv(ImmutableMap.copyOf(envVariables));

    FileLocation location = FileLocation.ofShared("abap/foo.txt");
    assertThat(location.getFile().isFile()).isTrue();
    assertThat(location.getFile().exists()).isTrue();
    assertThat(location.getFile().getName()).isEqualTo("foo.txt");
  }

  @Test
  public void ofSharedWithSystemProperty() throws URISyntaxException {
    URL url = getClass().getResource("/com/sonar/orchestrator/locator/FileLocationTest/index.txt");
    System.setProperty("orchestrator.it_sources", FilenameUtils.getFullPath(url.toURI().getPath()));
    FileLocation location = FileLocation.ofShared("abap/foo.txt");
    assertThat(location.getFile().isFile()).isTrue();
    assertThat(location.getFile().exists()).isTrue();
    assertThat(location.getFile().getName()).isEqualTo("foo.txt");
  }

  @Test
  public void ofShared_bad_shared_dir() {
    expectedException.expect(IllegalStateException.class);
    FileLocation.ofShared("abap/foo.txt", "/bad/path");
  }

  @Test
  public void byWildcardFilename_matches_one_file() throws IOException {
    File dir = temp.newFolder();
    File file = new File(dir, "sonar-foo-plugin-0.1-SNAPSHOT.jar");
    FileUtils.touch(file);

    FileLocation location = FileLocation.byWildcardFilename(dir, "sonar-foo-plugin-*.jar");
    assertThat(location.getFile()).isEqualTo(file);
  }

  @Test
  public void byWildcardFilename_fails_if_matches_multiple_files() throws IOException {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Multiple files match [sonar-foo-plugin-*.jar] in directory");

    File dir = temp.newFolder();
    FileUtils.touch(new File(dir, "sonar-foo-plugin-0.1-SNAPSHOT.jar"));
    FileUtils.touch(new File(dir, "sonar-foo-plugin-0.2-SNAPSHOT.jar"));

    FileLocation.byWildcardFilename(dir, "sonar-foo-plugin-*.jar");
  }

  @Test
  public void byWildcardFilename_fails_if_does_not_match() throws IOException {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("No files match [something-*.jar] in directory");

    File dir = temp.newFolder();
    FileUtils.touch(new File(dir, "sonar-foo-plugin-0.1-SNAPSHOT.jar"));

    FileLocation.byWildcardFilename(dir, "something-*.jar");
  }

  @Test
  public void byWildcardMavenFilename_fails_if_directory_does_not_exist() throws IOException {
    File dir = temp.newFolder();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Directory [" + dir + "] does not exist");

    dir.delete();
    FileLocation.byWildcardMavenFilename(dir, "sonar-foo-plugin-*.jar");
  }

  @Test
  public void byWildcardMavenFilename_matches_one_file() throws IOException {
    File dir = temp.newFolder();
    File file = new File(dir, "sonar-foo-plugin-0.1-SNAPSHOT.jar");
    FileUtils.touch(file);

    FileLocation location = FileLocation.byWildcardMavenFilename(dir, "sonar-foo-plugin-*.jar");
    assertThat(location.getFile()).isEqualTo(file);
  }

  @Test
  public void byWildcardMavenFilename_fails_if_matches_multiple_files() throws IOException {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Multiple files match [sonar-foo-plugin-*.jar] in directory");

    File dir = temp.newFolder();
    FileUtils.touch(new File(dir, "sonar-foo-plugin-0.1-SNAPSHOT.jar"));
    FileUtils.touch(new File(dir, "sonar-foo-plugin-0.2-SNAPSHOT.jar"));

    FileLocation.byWildcardMavenFilename(dir, "sonar-foo-plugin-*.jar");
  }

  @Test
  public void byWildcardMavenFilename_fails_if_does_not_match() throws IOException {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("No files match [something-*.jar] in directory");

    File dir = temp.newFolder();
    FileUtils.touch(new File(dir, "sonar-foo-plugin-0.1-SNAPSHOT.jar"));

    FileLocation.byWildcardMavenFilename(dir, "something-*.jar");
  }

  @Test
  public void byWildcardFilename_fails_if_directory_does_not_exist() throws IOException {
    File dir = temp.newFolder();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Directory [" + dir + "] does not exist");

    dir.delete();
    FileLocation.byWildcardMavenFilename(dir, "sonar-foo-plugin-*.jar");
  }

  @Test
  public void byWildcardFilename_excludes_sources() throws IOException {
    File dir = temp.newFolder();
    File file = new File(dir, "sonar-foo-plugin-0.1-SNAPSHOT.jar");
    File sources = new File(dir, "sonar-foo-plugin-0.1-SNAPSHOT-sources.jar");
    FileUtils.touch(file);
    FileUtils.touch(sources);

    assertThat(FileLocation.byWildcardMavenFilename(dir, "sonar-foo-plugin-*.jar").getFile()).isEqualTo(file);
  }

  @Test
  public void byWildcardFilename_excludes_tests() throws IOException {
    File dir = temp.newFolder();
    File file = new File(dir, "sonar-foo-plugin-0.1-SNAPSHOT.jar");
    File tests = new File(dir, "sonar-foo-plugin-0.1-SNAPSHOT-tests.jar");
    FileUtils.touch(file);
    FileUtils.touch(tests);

    assertThat(FileLocation.byWildcardMavenFilename(dir, "sonar-foo-plugin-*.jar").getFile()).isEqualTo(file);
  }

  @Test
  public void byWildcardFilename_excludes_javadoc() throws IOException {
    File dir = temp.newFolder();
    File file = new File(dir, "sonar-foo-plugin-0.1-SNAPSHOT.jar");
    File tests = new File(dir, "sonar-foo-plugin-0.1-SNAPSHOT-javadoc.jar");
    FileUtils.touch(file);
    FileUtils.touch(tests);

    assertThat(FileLocation.byWildcardMavenFilename(dir, "sonar-foo-plugin-*.jar").getFile()).isEqualTo(file);
  }
}
