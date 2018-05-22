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
import com.sonar.orchestrator.container.SonarDistribution.EDITION;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ServerZipFinderTest {

  private static final String A_VERSION = "6.7";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private Locators locators = mock(Locators.class);
  private ServerZipFinder underTest = new ServerZipFinder(locators);

  @Test
  public void use_local_zip() throws Exception {
    File zip = temp.newFile();
    SonarDistribution distribution = new SonarDistribution().setZipFile(zip);

    assertThat(underTest.find(distribution).getCanonicalPath()).isEqualTo(zip.getCanonicalPath());
    verifyZeroInteractions(locators);
  }

  @Test
  public void use_maven_zip() throws Exception {
    File zip = temp.newFile();

    for (EDITION edition : EDITION.values()) {
      reset(locators);
      when(locators.locate(any())).thenReturn(zip);
      SonarDistribution distribution = new SonarDistribution().setVersion(A_VERSION).setEdition(edition);
      File result = underTest.find(distribution);

      assertThat(result.getCanonicalPath()).isEqualTo(zip.getCanonicalPath());
      ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
      verify(locators).locate(captor.capture());
      MavenLocation calledLocation = (MavenLocation) captor.getValue();
      assertThat(calledLocation.getVersion()).isEqualTo(A_VERSION);
      assertThat(calledLocation.getPackaging()).isEqualTo("zip");
      switch (edition) {
        case COMMUNITY:
          assertThat(calledLocation.getGroupId()).isEqualTo("org.sonarsource.sonarqube");
          assertThat(calledLocation.getArtifactId()).isEqualTo("sonar-application");
          break;
        case DEVELOPER:
          assertThat(calledLocation.getGroupId()).isEqualTo("com.sonarsource.sonarqube");
          assertThat(calledLocation.getArtifactId()).isEqualTo("sonarqube-developer");
          break;
        case ENTERPRISE:
          assertThat(calledLocation.getGroupId()).isEqualTo("com.sonarsource.sonarqube");
          assertThat(calledLocation.getArtifactId()).isEqualTo("sonarqube-enterprise");
          break;
        case DATACENTER:
          assertThat(calledLocation.getGroupId()).isEqualTo("com.sonarsource.sonarqube");
          assertThat(calledLocation.getArtifactId()).isEqualTo("sonarqube-datacenter");
          break;

      }
    }
  }

  @Test
  public void throw_ISE_if_zip_not_found() {
    when(locators.locate(any())).thenReturn(null);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("SonarQube " + A_VERSION.toString() + " not found");

    underTest.find(new SonarDistribution().setVersion(A_VERSION));
  }
}
