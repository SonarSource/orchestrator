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

import com.sonar.orchestrator.junit.PropertyFilterRunner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(PropertyFilterRunner.class)
public class URLLocatorTest {

  private URLLocator urlLocator = new URLLocator();
  URLLocation location;
  URLLocation locationWithFilename;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

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
    URL anotherURL = null;
    try {
      anotherURL = new URL("http://docs.oracle.com/javase/7/docs/api/java/net/URL.html");
      URLLocation anotherURLLocation = URLLocation.create(anotherURL);
      URLLocation anotherURLLocationSameURL = URLLocation.create(anotherURL);

      assertThat(location.equals(location)).isTrue();
      assertThat(!location.equals(new String("wrong"))).isTrue();
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

    urlLocator.locate(location);
  }

  @Test
  public void copyToDirectory() throws Exception {
    File toDir = temp.newFolder();

    File copy = urlLocator.copyToDirectory(location, toDir);
    File copy2 = urlLocator.copyToDirectory(locationWithFilename, toDir);

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

    File copy = urlLocator.copyToFile(location, toFile);

    assertThat(copy).exists().isFile();
    assertThat(copy).isSameAs(toFile);
    assertThat(FileUtils.readFileToString(copy)).isEqualTo("foo");
  }

  @Test
  public void openInputStream() throws Exception {
    InputStream input = urlLocator.openInputStream(location);

    assertThat(IOUtils.toString(input)).isEqualTo("foo");
  }
}
