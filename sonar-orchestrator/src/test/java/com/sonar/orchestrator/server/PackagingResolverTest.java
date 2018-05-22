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
package com.sonar.orchestrator.server;

import com.sonar.orchestrator.container.SonarDistribution;
import com.sonar.orchestrator.container.SonarDistribution.Edition;
import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class PackagingResolverTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private Locators locators = mock(Locators.class, Mockito.RETURNS_DEEP_STUBS);
  private PackagingResolver underTest = new PackagingResolver(locators);

  @Test
  public void use_local_zip() throws Exception {
    testLocalZip(temp.newFile("sonarqube-6.7.2.zip"), "6.7.2");
    testLocalZip(temp.newFile("sonar-application-6.7.2.zip"), "6.7.2");
  }

  @Test
  public void fail_if_version_cant_be_guessed_from_local_zip() throws Exception {
    SonarDistribution distribution = new SonarDistribution().setZipFile(temp.newFile("sonar.zip"));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to guess version from filename: sonar.zip");

    underTest.resolve(distribution);
  }

  @Test
  public void fail_if_version_alias_cant_be_resolved() {
    SonarDistribution distribution = new SonarDistribution().setVersion("DOGFOOD");
    prepareResolutionOfVersion("org.sonarsource.sonarqube", "sonar-application", "DOGFOOD", Optional.empty());

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Version can not be resolved: [org.sonarsource.sonarqube:sonar-application:DOGFOOD:zip]");

    underTest.resolve(distribution);
  }

  @Test
  public void resolve_editions() throws Exception {
    testResolutionOfEdition(Edition.COMMUNITY, "org.sonarsource.sonarqube", "sonar-application");
    testResolutionOfEdition(Edition.DEVELOPER, "com.sonarsource.sonarqube", "sonarqube-developer");
    testResolutionOfEdition(Edition.ENTERPRISE, "com.sonarsource.sonarqube", "sonarqube-enterprise");
    testResolutionOfEdition(Edition.DATACENTER, "com.sonarsource.sonarqube", "sonarqube-datacenter");
  }

  @Test
  public void fail_if_zip_not_found() {
    when(locators.locate(any())).thenReturn(null);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("SonarQube 6.8 not found");

    underTest.resolve(new SonarDistribution().setVersion("6.8"));
  }

  private void testLocalZip(File zip, String expectedVersion) throws IOException {
    SonarDistribution distribution = new SonarDistribution().setZipFile(zip);

    Packaging packaging = underTest.resolve(distribution);

    assertThat(packaging.getEdition()).isEqualTo(Edition.COMMUNITY);
    assertThat(packaging.getVersion().toString()).isEqualTo(expectedVersion);
    assertThat(packaging.getZip().getCanonicalPath()).isEqualTo(zip.getCanonicalPath());
    verifyZeroInteractions(locators);
  }

  private void testResolutionOfEdition(Edition edition, String editionGroupId, String editionArtifactId) throws IOException {
    String versionOrAlias = "DEV";
    SonarDistribution distribution = new SonarDistribution().setVersion(versionOrAlias).setEdition(edition);
    // developer edition does not exist before 7.2, so the public group id must be used to resolve version alias
    prepareResolutionOfVersion("org.sonarsource.sonarqube", "sonar-application", versionOrAlias, Optional.of("7.3.0.10000"));
    File zip = prepareResolutionOfZip(editionGroupId, editionArtifactId, "7.3.0.10000");

    Packaging packaging = underTest.resolve(distribution);

    assertThat(packaging.getVersion().toString()).isEqualTo("7.3.0.10000");
    assertThat(packaging.getEdition()).isEqualTo(edition);
    assertThat(packaging.getZip().getCanonicalPath()).isEqualTo(zip.getCanonicalPath());
  }

  private File prepareResolutionOfZip(String groupId, String artifactId, String version) throws IOException {
    File zip = temp.newFile();
    when(locators.locate(MavenLocation.builder()
      .setGroupId(groupId)
      .setArtifactId(artifactId)
      .setVersion(version)
      .withPackaging("zip")
      .build())).thenReturn(zip);
    return zip;
  }

  private void prepareResolutionOfVersion(String groupId, String artifactId, String versionOrAlias, Optional<String> resolvedVersion) {
    when(locators.maven().resolveVersion(MavenLocation.builder()
      .setGroupId(groupId)
      .setArtifactId(artifactId)
      .setVersion(versionOrAlias)
      .withPackaging("zip")
      .build())).thenReturn(resolvedVersion);
  }
}
