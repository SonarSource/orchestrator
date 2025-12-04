/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
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
package com.sonar.orchestrator.build.util;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ZipUtilsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private File zip = FileUtils.toFile(getClass().getResource("ZipUtilsTest/shouldUnzipFile.zip"));

  @Test
  public void unzip_in_existing_directory() throws IOException {
    File toDir = temp.newFolder();

    ZipUtils.unzip(zip, toDir);

    assertThat(toDir.list()).hasSize(3);
  }

  @Test
  public void unzip_creates_target_directory() throws IOException {
    File toDir = temp.newFolder();
    FileUtils.deleteDirectory(toDir);

    ZipUtils.unzip(zip, toDir);

    assertThat(toDir.list()).hasSize(3);
  }

  @Test
  public void unzip_can_be_executed_multiple_times() throws IOException {
    File toDir = temp.newFolder();

    ZipUtils.unzip(zip, toDir);
    ZipUtils.unzip(zip, toDir);

    assertThat(toDir.list()).hasSize(3);
  }

  @Test
  public void unzip_using_java_implementation() throws IOException {
    File toDir = temp.newFolder();

    ZipUtils.javaUnzip(zip, toDir);

    assertThat(toDir.list()).hasSize(3);
  }

  @Test
  public void fail_if_unzipping_file_outside_target_directory() throws Exception {
    File zip = new File(getClass().getResource("ZipUtilsTest/zip-slip.zip").toURI());
    File toDir = temp.newFolder();

    try {
      ZipUtils.javaUnzip(zip, toDir);
      fail();
    } catch (Exception e) {
      assertThat(e.getCause()).isInstanceOfAny(IllegalStateException.class);
      assertThat(e.getCause().getMessage()).isEqualTo(
        "Unzipping an entry outside the target directory is not allowed: ../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../tmp/evil.txt");
    }
  }
}
