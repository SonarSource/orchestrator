/*
 * Orchestrator Locators
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
package com.sonar.orchestrator.locator;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import mockwebserver3.MockResponse;
import mockwebserver3.junit4.MockWebServerRule;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class URLLocatorTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public MockWebServerRule mockWebServerRule = new MockWebServerRule();
  private URLLocator underTest = new URLLocator();
  private URLLocation location;
  private URLLocation locationWithFilename;

  @Before
  public void prepare() {
    URL url = this.getClass().getResource("/com/sonar/orchestrator/locator/URLLocatorTest/foo.txt");
    location = URLLocation.create(url);
    locationWithFilename = URLLocation.create(url, "bar.txt");
  }

  @Test
  public void testToString() {
    assertThat(location.toString()).contains("com/sonar/orchestrator/locator/URLLocatorTest/foo.txt");
  }

  @Test
  public void testEquals() {
    URL anotherURL;
    try {
      anotherURL = new URL("http://docs.oracle.com/javase/7/docs/api/java/net/URL.html");
      URLLocation anotherURLLocation = URLLocation.create(anotherURL);
      URLLocation anotherURLLocationSameURL = URLLocation.create(anotherURL);

      assertThat(location.equals(location)).isTrue();
      assertThat(!location.equals("wrong")).isTrue();
      assertThat(anotherURLLocation.equals(anotherURLLocationSameURL)).isTrue();
      assertThat(!location.equals(anotherURLLocation)).isTrue();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testNotAnUri() throws MalformedURLException {
    URL url = new URL("http://docs.oracle.com/javase/7/docs/api/java/net/URL.html");
    URLLocation urlLocation = URLLocation.create(url);
    URLLocation notAnURIUrlLocation = URLLocation.create(new URL("http:// "));

    assertThat(notAnURIUrlLocation.equals(urlLocation)).isFalse();
    assertThat(notAnURIUrlLocation.equals(url)).isFalse();
    assertThat(notAnURIUrlLocation.hashCode()).isNotEqualTo(0);
  }

  @Test
  public void testHashCode() {
    assertThat(location.hashCode()).isNotEqualTo(0);
  }

  @Test
  public void locate_not_supported() {
    thrown.expect(UnsupportedOperationException.class);

    underTest.locate(location);
  }

  @Test
  public void copyToDirectory() throws Exception {
    File toDir = temp.newFolder();

    File copy = underTest.copyToDirectory(location, toDir);
    File copy2 = underTest.copyToDirectory(locationWithFilename, toDir);

    assertThat(copy).exists().isFile();
    assertThat(copy2).exists().isFile();
    assertThat(copy.getName()).isEqualTo("foo.txt");
    assertThat(copy2.getName()).isEqualTo("bar.txt");
    assertThat(FileUtils.readFileToString(copy)).isEqualTo("foo");
    assertThat(FileUtils.readFileToString(copy2)).isEqualTo("foo");
  }

  @Test
  public void copyToFile() throws Exception {
    File toFile = temp.newFile();

    File copy = underTest.copyToFile(location, toFile);

    assertThat(copy).exists().isFile();
    assertThat(copy).isSameAs(toFile);
    assertThat(FileUtils.readFileToString(copy)).isEqualTo("foo");
  }

  @Test
  public void openInputStream() throws Exception {
    InputStream input = underTest.openInputStream(location);

    assertThat(IOUtils.toString(input)).isEqualTo("foo");
  }

  @Test
  public void test_getFilenameFromContentDispositionHeader() {
    assertThat(URLLocator.getFilenameFromContentDispositionHeader(null)).isNull();
    assertThat(URLLocator.getFilenameFromContentDispositionHeader("")).isNull();
    assertThat(URLLocator.getFilenameFromContentDispositionHeader("Content-Disposition: attachment; filename=foo.jar")).isEqualTo("foo.jar");
    assertThat(URLLocator.getFilenameFromContentDispositionHeader("Content-Disposition: attachment; filename=foo.jar;")).isEqualTo("foo.jar");
  }

  @Test
  public void test_copyToFile() throws Exception {
    File toFile = temp.newFile();
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body("hello world").build());

    underTest.copyToFile(URLLocation.create(mockWebServerRule.getServer().url("/").url()), toFile);
    assertThat(toFile).exists().isFile().hasContent("hello world");
  }

  @Test
  public void copyToDir_gets_filename_from_http_header() throws Exception {
    File toDir = temp.newFolder();
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body("hello world").setHeader("Content-Disposition", "attachment; filename=\"foo.txt\"").build());

    // URL is about bar.txt but HTTP header is about foo.txt -> the latter wins
    underTest.copyToDirectory(URLLocation.create(mockWebServerRule.getServer().url("/bar.txt").url()), toDir);
    File toFile = new File(toDir, "foo.txt");
    assertThat(toFile).exists().isFile().hasContent("hello world");
  }

  @Test
  public void copyToDir_gets_filename_from_url_if_http_header_is_missing() throws Exception {
    File toDir = temp.newFolder();
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body("hello world").build());

    underTest.copyToDirectory(URLLocation.create(mockWebServerRule.getServer().url("/foo.txt").url()), toDir);
    File toFile = new File(toDir, "foo.txt");
    assertThat(toFile).exists().isFile().hasContent("hello world");
  }
}
