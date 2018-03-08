/*
 * Orchestrator
 * Copyright (C) 2011-2018 SonarSource SA
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

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class FileLocatorTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void copy_to_directory() throws Exception {
    FileLocator locator = new FileLocator();
    File fileToCopy = new File(getClass().getResource("/com/sonar/orchestrator/locator/FileLocatorTest/foo.txt").toURI());

    File toDir = temp.newFolder();
    File copy = locator.copyToDirectory(FileLocation.of(fileToCopy), toDir);
    assertThat(copy).isNotNull();
    assertThat(copy).isEqualTo(new File(toDir, "foo.txt"));
    assertThat(copy.length()).isGreaterThan(0L);
  }

  @Test
  public void copy_to_file() throws Exception {
    FileLocator locator = new FileLocator();
    File fileToCopy = new File(getClass().getResource("/com/sonar/orchestrator/locator/FileLocatorTest/foo.txt").toURI());

    File toFile = temp.newFile();
    File copy = locator.copyToFile(FileLocation.of(fileToCopy), toFile);
    assertThat(copy).isNotNull();
    assertThat(copy).isSameAs(toFile);
    assertThat(copy.length()).isGreaterThan(0L);
  }

  @Test
  public void open_stream() throws Exception {
    FileLocator locator = new FileLocator();
    File fileToCopy = new File(getClass().getResource("/com/sonar/orchestrator/locator/FileLocatorTest/foo.txt").toURI());

    InputStream input = locator.openInputStream(FileLocation.of(fileToCopy));
    assertThat(IOUtils.toString(input)).isEqualTo("foo");
  }
}
