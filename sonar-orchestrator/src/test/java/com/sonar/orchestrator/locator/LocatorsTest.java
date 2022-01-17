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

import com.sonar.orchestrator.config.FileSystem;
import java.io.File;
import java.io.InputStream;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LocatorsTest {
  FileLocator fileLocator = mock(FileLocator.class);
  MavenLocator mavenLocator = mock(MavenLocator.class);
  ResourceLocator resourceLocator = mock(ResourceLocator.class);
  URLLocator urlLocator = mock(URLLocator.class);

  Locators locators = new Locators(fileLocator, mavenLocator, resourceLocator, urlLocator);

  // Locate

  @Test
  public void should_locate_with_file_locator() {
    FileLocation location = mock(FileLocation.class);
    File expectedFile = new File("found");
    when(fileLocator.locate(location)).thenReturn(expectedFile);

    File actualFile = locators.locate(location);

    assertThat(actualFile).isSameAs(expectedFile);
  }

  @Test
  public void should_locate_with_maven_locator() {
    MavenLocation location = mock(MavenLocation.class);
    File expectedFile = new File("found");
    when(mavenLocator.locate(location)).thenReturn(expectedFile);

    File actualFile = locators.locate(location);

    assertThat(actualFile).isSameAs(expectedFile);
  }

  @Test
  public void should_locate_with_resource_locator() {
    ResourceLocation location = mock(ResourceLocation.class);
    File expectedFile = new File("found");
    when(resourceLocator.locate(location)).thenReturn(expectedFile);

    File actualFile = locators.locate(location);

    assertThat(actualFile).isSameAs(expectedFile);
  }

  @Test
  public void should_locate_with_url_locator() {
    URLLocation location = mock(URLLocation.class);
    File expectedFile = new File("found");
    when(urlLocator.locate(location)).thenReturn(expectedFile);

    File actualFile = locators.locate(location);

    assertThat(actualFile).isSameAs(expectedFile);
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_fail_to_locate_unsupported_location() {
    Locators locators = new Locators(mock(FileSystem.class), mock(Artifactory.class));

    locators.locate(new UnsupportedLocation());
  }

  // Copy to file

  @Test
  public void should_copy_to_file_with_file_locator() {
    FileLocation location = mock(FileLocation.class);
    File expectedFile = new File("found");
    when(fileLocator.copyToFile(location, new File("destination"))).thenReturn(expectedFile);

    File actualFile = locators.copyToFile(location, new File("destination"));

    assertThat(actualFile).isSameAs(expectedFile);
  }

  @Test
  public void should_copy_to_file_with_maven_locator() {
    MavenLocation location = mock(MavenLocation.class);
    File expectedFile = new File("found");
    when(mavenLocator.copyToFile(location, new File("destination"))).thenReturn(expectedFile);

    File actualFile = locators.copyToFile(location, new File("destination"));

    assertThat(actualFile).isSameAs(expectedFile);
  }

  @Test
  public void should_copy_to_file_with_resource_locator() {
    ResourceLocation location = mock(ResourceLocation.class);
    File expectedFile = new File("found");
    when(resourceLocator.copyToFile(location, new File("destination"))).thenReturn(expectedFile);

    File actualFile = locators.copyToFile(location, new File("destination"));

    assertThat(actualFile).isSameAs(expectedFile);
  }

  @Test
  public void should_copy_to_file_with_url_locator() {
    URLLocation location = mock(URLLocation.class);
    File expectedFile = new File("found");
    when(urlLocator.copyToFile(location, new File("destination"))).thenReturn(expectedFile);

    File actualFile = locators.copyToFile(location, new File("destination"));

    assertThat(actualFile).isSameAs(expectedFile);
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_fail_to_copy_to_file_unsupported_location() {
    Locators locators = new Locators(mock(FileSystem.class), mock(Artifactory.class));

    locators.copyToFile(new UnsupportedLocation(), new File("destination"));
  }

  // Copy to directory

  @Test
  public void should_copy_to_directory_with_file_locator() {
    FileLocation location = mock(FileLocation.class);
    File expectedFile = new File("found");
    when(fileLocator.copyToDirectory(location, new File("destination"))).thenReturn(expectedFile);

    File actualFile = locators.copyToDirectory(location, new File("destination"));

    assertThat(actualFile).isSameAs(expectedFile);
  }

  @Test
  public void should_copy_to_directory_with_maven_locator() {
    MavenLocation location = mock(MavenLocation.class);
    File expectedFile = new File("found");
    when(mavenLocator.copyToDirectory(location, new File("destination"))).thenReturn(expectedFile);

    File actualFile = locators.copyToDirectory(location, new File("destination"));

    assertThat(actualFile).isSameAs(expectedFile);
  }

  @Test
  public void should_copy_to_directory_with_resource_locator() {
    ResourceLocation location = mock(ResourceLocation.class);
    File expectedFile = new File("found");
    when(resourceLocator.copyToDirectory(location, new File("destination"))).thenReturn(expectedFile);

    File actualFile = locators.copyToDirectory(location, new File("destination"));

    assertThat(actualFile).isSameAs(expectedFile);
  }

  @Test
  public void should_copy_to_directory_with_url_locator() {
    URLLocation location = mock(URLLocation.class);
    File expectedFile = new File("found");
    when(urlLocator.copyToDirectory(location, new File("destination"))).thenReturn(expectedFile);

    File actualFile = locators.copyToDirectory(location, new File("destination"));

    assertThat(actualFile).isSameAs(expectedFile);
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_fail_to_copy_to_directory_unsupported_location() {
    Locators locators = new Locators(mock(FileSystem.class), mock(Artifactory.class));

    locators.copyToDirectory(new UnsupportedLocation(), new File("destination"));
  }

  // Open Stream

  @Test
  public void should_open_stream_with_file_locator() {
    FileLocation location = mock(FileLocation.class);
    InputStream expectedStream = mock(InputStream.class);
    when(fileLocator.openInputStream(location)).thenReturn(expectedStream);

    InputStream actualStream = locators.openInputStream(location);

    assertThat(actualStream).isSameAs(expectedStream);
  }

  @Test
  public void should_open_stream_with_maven_locator() {
    MavenLocation location = mock(MavenLocation.class);
    InputStream expectedStream = mock(InputStream.class);
    when(mavenLocator.openInputStream(location)).thenReturn(expectedStream);

    InputStream actualStream = locators.openInputStream(location);

    assertThat(actualStream).isSameAs(expectedStream);
  }

  @Test
  public void should_open_stream_with_resource_locator() {
    ResourceLocation location = mock(ResourceLocation.class);
    InputStream expectedStream = mock(InputStream.class);
    when(resourceLocator.openInputStream(location)).thenReturn(expectedStream);

    InputStream actualStream = locators.openInputStream(location);

    assertThat(actualStream).isSameAs(expectedStream);
  }

  @Test
  public void should_open_stream_with_url_locator() {
    URLLocation location = mock(URLLocation.class);
    InputStream expectedStream = mock(InputStream.class);
    when(urlLocator.openInputStream(location)).thenReturn(expectedStream);

    InputStream actualStream = locators.openInputStream(location);

    assertThat(actualStream).isSameAs(expectedStream);
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_fail_to_open_stream_unsupported_location() {
    Locators locators = new Locators(mock(FileSystem.class), mock(Artifactory.class));

    locators.openInputStream(new UnsupportedLocation());
  }

  private static class UnsupportedLocation implements Location {
  }
}
