/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource Sàrl
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
package com.sonar.orchestrator.util;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.locator.FileLocation;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import org.apache.commons.io.FilenameUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class SharedDirTest {

  @Rule
  public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void getFileLocationOfShared() throws URISyntaxException {
    URL url = getClass().getResource("/com/sonar/orchestrator/util/SharedDirTest/sample.properties");
    Properties props = new Properties();
    props.setProperty("orchestrator.configUrl", url.toString());
    props.setProperty("orchestrator.it_sources", FilenameUtils.getFullPath(url.toURI().getPath()));
    Configuration config = Configuration.create(props);
    SharedDir underTest = new SharedDir(config);

    FileLocation location = underTest.getFileLocationOfShared("sample.properties");
    assertThat(location.getFile()).isFile();
    assertThat(location.getFile()).exists();
    assertThat(location.getFile()).hasName("sample.properties");
  }

  @Test
  public void getFileLocationOfSharedApplyPriority() throws URISyntaxException {
    URL url = getClass().getResource("/com/sonar/orchestrator/util/SharedDirTest/sample.properties");
    Properties props = new Properties();
    props.setProperty("orchestrator.configUrl", url.toString());
    environmentVariables.set("SONAR_IT_SOURCES", FilenameUtils.getFullPath(url.toURI().getPath()));
    props.setProperty("orchestrator.it_sources", FilenameUtils.getFullPath(url.toURI().getPath()));
    Configuration config = Configuration.create(props);
    SharedDir underTest = new SharedDir(config);

    FileLocation location = underTest.getFileLocationOfShared("sample.properties");
    assertThat(location.getFile()).exists();
  }

  @Test
  public void getFileLocationOfShared_bad_place() {
    thrown.expect(IllegalStateException.class);

    URL url = getClass().getResource("/com/sonar/orchestrator/util/SharedDirTest/sample.properties");
    Properties props = new Properties();
    props.setProperty("orchestrator.configUrl", url.toString());
    props.setProperty("orchestrator.it_sources", "/com/sonar/orchestrator/util/SharedDirTest");
    Configuration config = Configuration.create(props);
    SharedDir underTest = new SharedDir(config);

    underTest.getFileLocationOfShared("/bad/path");
  }

  @Test
  public void getFileLocationOfShared_not_a_directory() {
    thrown.expect(IllegalStateException.class);

    URL url = getClass().getResource("/com/sonar/orchestrator/util/SharedDirTest/sample.properties");
    Properties props = new Properties();
    props.setProperty("orchestrator.configUrl", url.toString());
    props.setProperty("orchestrator.it_sources", "/com/sonar/orchestrator/util/SharedDirTest/sample.properties");
    Configuration config = Configuration.create(props);
    SharedDir underTest = new SharedDir(config);

    underTest.getFileLocationOfShared(".");
  }

}
