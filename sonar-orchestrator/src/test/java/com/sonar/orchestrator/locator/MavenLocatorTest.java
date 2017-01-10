/*
 * Orchestrator
 * Copyright (C) 2011-2017 SonarSource SA
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

import com.sonar.orchestrator.config.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class MavenLocatorTest {

  private File localRepository = FileUtils.toFile(getClass().getResource("/com/sonar/orchestrator/locator/MavenLocatorTest/repository"));

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void shouldFindInLocalRepository() {
    Configuration config = Configuration.builder()
      .setProperty("maven.localRepository", localRepository)
      .build();
    MavenLocator locator = new MavenLocator(config);

    File file = locator.locate(MavenLocation.builder().setKey("group", "artifact", "1.0").build());

    assertThat(file).exists().hasName("artifact-1.0.jar");

    Locators locators = new Locators(config);
    locators.locate(MavenLocation.builder().setKey("group", "artifact", "1.0").build());

    assertThat(file).exists().hasName("artifact-1.0.jar");
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

    assertThat(config.fileSystem().mavenLocalRepository().getAbsolutePath()).isNotEmpty();
    assertThat(locator.locate(MavenLocation.builder().setKey("group", "other", "1.1").build())).isNull();
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
  public void open_local_input_stream() throws Exception {
    Configuration config = Configuration.builder()
      .setProperty("maven.localRepository", localRepository)
      .build();
    MavenLocator locator = new MavenLocator(config);

    InputStream input = locator.openInputStream(MavenLocation.builder().setKey("group", "artifact", "1.0").build());

    assertThat(IOUtils.toByteArray(input).length).isGreaterThan(0);
  }
}
