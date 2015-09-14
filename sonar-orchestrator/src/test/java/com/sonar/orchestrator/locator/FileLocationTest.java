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

import com.google.common.collect.ImmutableMap;
import com.sonar.orchestrator.PropertyAndEnvTest;
import org.apache.commons.io.FilenameUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.sonar.orchestrator.TestModules.setEnv;
import static org.assertj.core.api.Assertions.assertThat;

public class FileLocationTest extends PropertyAndEnvTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

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
  public void ofShared() {
    URL url = getClass().getResource("/com/sonar/orchestrator/locator/FileLocationTest/index.txt");
    FileLocation location = FileLocation.ofShared("abap/foo.txt", FilenameUtils.getFullPath(url.getPath()));
    assertThat(location.getFile().isFile()).isTrue();
    assertThat(location.getFile().exists()).isTrue();
    assertThat(location.getFile().getName()).isEqualTo("foo.txt");
  }

  @Test
  public void ofSharedWithEnv() {
    URL url = getClass().getResource("/com/sonar/orchestrator/locator/FileLocationTest/index.txt");

    Map<String,String> envVariables = new HashMap<>(System.getenv());
    envVariables.put("SONAR_IT_SOURCES", FilenameUtils.getFullPath(url.getPath()));
    setEnv(ImmutableMap.copyOf(envVariables));

    FileLocation location = FileLocation.ofShared("abap/foo.txt");
    assertThat(location.getFile().isFile()).isTrue();
    assertThat(location.getFile().exists()).isTrue();
    assertThat(location.getFile().getName()).isEqualTo("foo.txt");
  }

  @Test
  public void ofSharedWithSystemProperty() {
    URL url = getClass().getResource("/com/sonar/orchestrator/locator/FileLocationTest/index.txt");
    System.setProperty("orchestrator.it_sources", FilenameUtils.getFullPath(url.getPath()));
    FileLocation location = FileLocation.ofShared("abap/foo.txt");
    assertThat(location.getFile().isFile()).isTrue();
    assertThat(location.getFile().exists()).isTrue();
    assertThat(location.getFile().getName()).isEqualTo("foo.txt");
  }

  @Test
  public void ofShared_bad_shared_dir() {
    thrown.expect(IllegalStateException.class);
    FileLocation.ofShared("abap/foo.txt", "/bad/path");
  }
}
