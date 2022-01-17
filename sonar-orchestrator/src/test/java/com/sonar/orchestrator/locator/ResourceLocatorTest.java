/*
 * Orchestrator
 * Copyright (C) 2011-2022 SonarSource SA
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
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class ResourceLocatorTest {

  private ResourceLocator resourceLocator = new ResourceLocator();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void locate_not_supported() {
    thrown.expect(UnsupportedOperationException.class);

    ResourceLocation location = ResourceLocation.create("/com/sonar/orchestrator/locator/ResourceLocatorTest/foo.txt");
    resourceLocator.locate(location);
  }

  @Test
  public void copyToDirectory() throws Exception {
    File toDir = temp.newFolder();

    ResourceLocation location = ResourceLocation.create("/com/sonar/orchestrator/locator/ResourceLocatorTest/foo.txt");
    File copy = resourceLocator.copyToDirectory(location, toDir);

    assertThat(copy).exists().isFile();
    assertThat(copy.getName()).isEqualTo("foo.txt");
    assertThat(FileUtils.readFileToString(copy)).isEqualTo("foo");
  }

  @Test
  public void copyToFile() throws Exception {
    File toFile = temp.newFile();

    ResourceLocation location = ResourceLocation.create("/com/sonar/orchestrator/locator/ResourceLocatorTest/foo.txt");
    File copy = resourceLocator.copyToFile(location, toFile);

    assertThat(copy).exists().isFile();
    assertThat(copy).isSameAs(toFile);
    assertThat(FileUtils.readFileToString(copy)).isEqualTo("foo");
  }

  @Test
  public void openInputStream() throws Exception {
    ResourceLocation location = ResourceLocation.create("/com/sonar/orchestrator/locator/ResourceLocatorTest/foo.txt");
    InputStream input = resourceLocator.openInputStream(location);

    assertThat(IOUtils.toString(input)).isEqualTo("foo");
  }
}
