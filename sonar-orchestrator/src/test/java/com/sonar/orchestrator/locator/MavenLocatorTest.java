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

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.junit.PropertyFilterRunner;
import com.sonar.orchestrator.test.MockHttpServerInterceptor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(PropertyFilterRunner.class)
public class MavenLocatorTest {

  private File localRepository = FileUtils.toFile(getClass().getResource("/com/sonar/orchestrator/locator/MavenLocatorTest/repository"));

  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldFindInLocalRepository() {
    Configuration config = Configuration.builder()
      .setProperty("maven.localRepository", localRepository)
      .build();
    MavenLocator locator = new MavenLocator(config);

    File file = locator.locate(MavenLocation.builder().setKey("group", "artifact", "1.0").build());

    assertThat(file).exists();
    assertThat(file.getName()).isEqualTo("artifact-1.0.jar");

    Locators locators = new Locators(config);
    locators.locate(MavenLocation.builder().setKey("group", "artifact", "1.0").build());
    assertThat(file).exists();
    assertThat(file.getName()).isEqualTo("artifact-1.0.jar");
  }

  @Test
  public void shouldReturnNullIfFileNotFoundInLocalRepository() {
    Configuration config = Configuration.builder()
      .setProperty("maven.localRepository", localRepository)
      .build();
    MavenLocator locator = new MavenLocator(config);

    File file = locator.locate(MavenLocation.builder().setKey("group", "other", "1.1").build());

    assertThat(file).isNull();
  }

  @Test
  public void shouldNotFailIfLocalRepositoryNotDefined() {
    Configuration config = Configuration.create(new Properties());
    MavenLocator locator = new MavenLocator(config);

    assertThat(config.fileSystem().mavenLocalRepository()).isEqualTo(new File(System.getProperty("user.home"), ".m2/repository"));
    assertThat(locator.locate(MavenLocation.builder().setKey("group", "other", "1.1").build())).isNull();
  }

  @Test
  public void shouldDownloadFileFromRemoteRepository() {
    httpServer.setMockResponseData("This is a jar");

    Configuration config = Configuration.builder()
      .setProperty("maven.nexusUrl", "http://localhost:" + httpServer.getPort() + "/")
      .setProperty("maven.nexusRepository", "ss-repo")
      .build();
    MavenLocator locator = new MavenLocator(config);

    File file = locator.locate(MavenLocation.builder().setKey("org.codehaus.sonar", "sonar-java-api", "2.8").build());

    assertThat(file).isNotNull();
    assertThat(file.getName()).isEqualTo("sonar-java-api-2.8.jar");
    assertThat(file.length()).isEqualTo(13);
  }

  @Test
  public void shouldAppendPathToUrl() throws MalformedURLException {
    URL url = new URL("http://no.end.slash");
    assertThat(MavenLocator.appendPathToUrl("foo/bar.jar", url).toString()).isEqualTo("http://no.end.slash/foo/bar.jar");

    url = new URL("http://end.slash/");
    assertThat(MavenLocator.appendPathToUrl("foo/bar.jar", url).toString()).isEqualTo("http://end.slash/foo/bar.jar");
  }

  @Test
  public void copy_local_file_to_directory() throws IOException {
    File toDir = temp.newFolder();

    Configuration config = Configuration.builder()
      .setProperty("maven.localRepository", localRepository)
      .build();
    MavenLocator locator = new MavenLocator(config);

    File file = locator.copyToDirectory(MavenLocation.builder().setKey("group", "artifact", "1.0").build(), toDir);

    assertThat(file.exists()).isTrue();
    assertThat(file.getName()).isEqualTo("artifact-1.0.jar");
  }

  @Test
  public void copy_local_file_to_file() throws IOException {
    File toDir = temp.newFolder();

    Configuration config = Configuration.builder()
      .setProperty("maven.localRepository", localRepository)
      .build();
    MavenLocator locator = new MavenLocator(config);

    File file = locator.copyToFile(MavenLocation.builder().setKey("group", "artifact", "1.0").build(), new File(toDir, "foo.jar"));

    assertThat(file.exists()).isTrue();
    assertThat(file.getName()).isEqualTo("foo.jar");
  }

  @Test
  public void copy_remote_file_to_directory() throws Exception {
    httpServer.setMockResponseData("This is a jar");
    File toDir = temp.newFolder();

    Configuration config = Configuration.builder()
      .setProperty("maven.nexusUrl", "http://localhost:" + httpServer.getPort() + "/")
      .setProperty("maven.nexusRepository", "ss-repo")
      .build();
    MavenLocator locator = new MavenLocator(config);

    File file = locator.copyToDirectory(MavenLocation.builder().setKey("org.codehaus.sonar", "sonar-java-api", "2.8").build(), toDir);

    assertThat(file).exists();
    assertThat(file.getName()).isEqualTo("sonar-java-api-2.8.jar");
    assertThat(file.length()).isEqualTo(13);
  }

  @Test
  public void copy_remote_file_to_file() throws Exception {
    httpServer.setMockResponseData("This is a jar");
    File toDir = temp.newFolder();

    Configuration config = Configuration.builder()
      .setProperty("maven.nexusUrl", "http://localhost:" + httpServer.getPort() + "/")
      .setProperty("maven.nexusRepository", "ss-repo")
      .build();
    MavenLocator locator = new MavenLocator(config);

    File file = locator.copyToFile(MavenLocation.builder().setKey("org.codehaus.sonar", "sonar-java-api", "2.8").build(), new File(toDir, "foo.jar"));

    assertThat(file).exists();
    assertThat(file.getName()).isEqualTo("foo.jar");
    assertThat(file.length()).isEqualTo(13);
  }

  @Test
  public void open_local_input_stream() throws Exception {
    Configuration config = Configuration.builder()
      .setProperty("maven.localRepository", localRepository)
      .build();
    MavenLocator locator = new MavenLocator(config);

    InputStream input = locator.openInputStream(MavenLocation.builder().setKey("group", "artifact", "1.0").build());

    assertThat(IOUtils.toByteArray(input).length).isGreaterThan(0);
  }

  @Test
  public void open_remote_input_stream() throws Exception {
    httpServer.setMockResponseData("This is a jar");

    Configuration config = Configuration.builder()
      .setProperty("maven.nexusUrl", "http://localhost:" + httpServer.getPort() + "/")
      .setProperty("maven.nexusRepository", "ss-repo")
      .build();
    MavenLocator locator = new MavenLocator(config);

    InputStream input = locator.openInputStream(MavenLocation.builder().setKey("org.codehaus.sonar", "sonar-java-api", "2.8").build());

    assertThat(IOUtils.toByteArray(input).length).isGreaterThan(0);
  }

  @Test
  public void throw_when_error_opening_remote_stream() {
    httpServer.setMockResponseStatus(500);

    thrown.expect(IllegalStateException.class);
    Configuration config = Configuration.builder()
      .setProperty("maven.nexusUrl", "http://localhost:" + httpServer.getPort() + "/")
      .setProperty("maven.nexusRepository", "ss-repo")
      .build();
    MavenLocator locator = new MavenLocator(config);

    // Create an error 500 on Nexus by injecting URL parameter in groupId
    locator.openInputStream(MavenLocation.builder().setKey("unknow&v=", "xxx", "2.8").build());
  }

  @Test
  public void shouldReturnNullIfRemoteFileDoesNotExist() {
    httpServer.setMockResponseStatus(404);

    Configuration config = Configuration.builder()
      .setProperty("maven.nexusUrl", "http://localhost:" + httpServer.getPort() + "/")
      .setProperty("maven.nexusRepository", "ss-repo")
      .build();
    MavenLocator locator = new MavenLocator(config);

    File file = locator.locate(MavenLocation.builder().setKey("unknown", "xxx", "1.2.3").build());

    assertThat(file).isNull();
  }
}
