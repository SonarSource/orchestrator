/*
 * Orchestrator
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package com.sonar.orchestrator.server;

import com.google.common.collect.ImmutableSortedSet;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.container.SonarDistribution;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerZipFinderTest {

  private static final File ZIP_4_5_6 = FileUtils.toFile(ServerInstallerTest.class.getResource("ServerZipFinderTest/sonarqube-4.5.6-lite.zip"));
  private static final Version VERSION_4_5_6 = Version.create("4.5.6");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public MockWebServer updateCenterServer = new MockWebServer();

  private FileSystem fs;
  private File installsDir;
  private File workspaceDir;
  private File mavenLocalDir;
  private UpdateCenter updateCenter = mock(UpdateCenter.class, Mockito.RETURNS_DEEP_STUBS);
  private ServerZipCache cache;
  private ServerZipFinder underTest;

  @Before
  public void setUp() throws IOException {
    installsDir = temp.newFolder();
    workspaceDir = temp.newFolder();
    mavenLocalDir = temp.newFolder();
    Configuration config = Configuration.builder()
      .setProperty("orchestrator.sonarInstallsDir", installsDir.getAbsolutePath())
      .setProperty("orchestrator.workspaceDir", workspaceDir.getAbsolutePath())
      .setProperty("maven.localRepository", mavenLocalDir.getAbsolutePath())
      .build();
    fs = new FileSystem(config);
    cache = new ServerZipCache(fs);
    underTest = new ServerZipFinder(fs, updateCenter, cache);
  }

  @Test
  public void find_zip_version_in_cache() {
    File cached = cache.copyToCache(VERSION_4_5_6, ZIP_4_5_6);
    assertThat(underTest.find(new SonarDistribution().setVersion(VERSION_4_5_6))).isEqualTo(cached);
  }

  @Test
  public void find_zip_version_in_maven_local_repository() throws Exception {
    File mavenArtifact = new File(mavenLocalDir, "org/sonarsource/sonarqube/sonar-application/4.5.6/sonar-application-4.5.6.zip");
    FileUtils.copyFile(ZIP_4_5_6, mavenArtifact);
    assertThat(underTest.find(new SonarDistribution().setVersion(VERSION_4_5_6))).exists().isEqualTo(mavenArtifact);
  }

  @Test
  public void find_old_codehaus_zip_in_maven_local_repository() throws Exception {
    File mavenArtifact = new File(mavenLocalDir, "org/codehaus/sonar/sonar-application/4.5.6/sonar-application-4.5.6.zip");
    FileUtils.copyFile(ZIP_4_5_6, mavenArtifact);
    assertThat(underTest.find(new SonarDistribution().setVersion(VERSION_4_5_6))).exists().isEqualTo(mavenArtifact);
  }

  @Test
  public void find_zip_in_update_center() throws Exception {
    Release updateCenterRelease = new Release(null, org.sonar.updatecenter.common.Version.create(VERSION_4_5_6.toString()))
      .setDownloadUrl(updateCenterServer.url("/sq/sonarqube-4.5.6.zip").toString());
    when(updateCenter.getSonar().getAllReleases()).thenReturn(ImmutableSortedSet.of(updateCenterRelease));

    updateCenterServer.enqueue(new MockResponse()
      .addHeader("Content-Disposition", "attachment; filename=sonarqube-4.5.6.zip")
      .setBody("<content>"));
    File zip = underTest.find(new SonarDistribution().setVersion(VERSION_4_5_6));
    assertThat(zip).exists().isFile();
    assertThat(zip.getName()).isEqualTo("sonarqube-4.5.6.zip");
    assertThat(zip.getParentFile()).isEqualTo(installsDir);
  }

  @Test
  public void throw_ISE_if_fail_to_download_from_update_center() throws Exception {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(format("Fail to download http://localhost:%d/sq/sonarqube-4.5.6.zip. Received 500 [Server Error]", updateCenterServer.getPort()));

    Release updateCenterRelease = new Release(null, org.sonar.updatecenter.common.Version.create(VERSION_4_5_6.toString()))
      .setDownloadUrl(updateCenterServer.url("/sq/sonarqube-4.5.6.zip").toString());
    when(updateCenter.getSonar().getAllReleases()).thenReturn(ImmutableSortedSet.of(updateCenterRelease));

    updateCenterServer.enqueue(new MockResponse().setResponseCode(500));
    underTest.find(new SonarDistribution().setVersion(VERSION_4_5_6));
  }

  @Test
  public void do_not_download_from_update_center_if_version_url_is_not_defined() throws Exception {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("SonarQube 4.5.6 not found");

    Release updateCenterRelease = new Release(null, org.sonar.updatecenter.common.Version.create(VERSION_4_5_6.toString()))
      .setDownloadUrl(null);
    when(updateCenter.getSonar().getAllReleases()).thenReturn(ImmutableSortedSet.of(updateCenterRelease));

    underTest.find(new SonarDistribution().setVersion(VERSION_4_5_6));
  }

  @Test
  public void throw_ISE_if_zip_not_found() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("SonarQube 4.5.6 not found");

    underTest.find(new SonarDistribution().setVersion(VERSION_4_5_6));
  }
}
