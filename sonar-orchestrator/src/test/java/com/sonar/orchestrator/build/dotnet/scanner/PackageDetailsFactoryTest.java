/*
 * Orchestrator
 * Copyright (C) 2011-2024 SonarSource SA
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
package com.sonar.orchestrator.build.dotnet.scanner;

import com.sonar.orchestrator.version.Version;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PackageDetailsFactoryTest {
  private final PackageDetailsFactory sut = new PackageDetailsFactory();

  @Test
  public void create_old_scanner_version_using_dotnet()  {
    PackageDetails packageDetails = sut.create(Version.create("5.0.0.0"), true);

    assertThat(packageDetails.getGroupId()).isEqualTo("org.sonarsource.scanner.msbuild");
    assertThat(packageDetails.getArtifactId()).isEqualTo("sonar-scanner-msbuild");
    assertThat(packageDetails.getClassifier()).isEqualTo("netcoreapp2.0");
    assertThat(packageDetails.getPackageName()).isEqualTo("sonar-scanner-msbuild-5.0.0.0-netcoreapp2.0");
    assertThat(packageDetails.getExecutableName()).isEqualTo("SonarScanner.MSBuild.dll");
  }

  @Test
  public void create_old_scanner_version_using_dotnet_framework()  {
    PackageDetails packageDetails = sut.create(Version.create("5.0.0.0"), false);

    assertThat(packageDetails.getGroupId()).isEqualTo("org.sonarsource.scanner.msbuild");
    assertThat(packageDetails.getArtifactId()).isEqualTo("sonar-scanner-msbuild");
    assertThat(packageDetails.getClassifier()).isEqualTo("net46");
    assertThat(packageDetails.getPackageName()).isEqualTo("sonar-scanner-msbuild-5.0.0.0-net46");
    assertThat(packageDetails.getExecutableName()).isEqualTo("SonarScanner.MSBuild.exe");
  }

  @Test
  public void create_new_scanner_version_using_dotnet()  {
    PackageDetails packageDetails = sut.create(Version.create("6.0.0.81631"), true);

    assertThat(packageDetails.getGroupId()).isEqualTo("org.sonarsource.scanner.msbuild");
    assertThat(packageDetails.getArtifactId()).isEqualTo("sonar-scanner");
    assertThat(packageDetails.getClassifier()).isEqualTo("net");
    assertThat(packageDetails.getPackageName()).isEqualTo("sonar-scanner-6.0.0.81631-net");
    assertThat(packageDetails.getExecutableName()).isEqualTo("SonarScanner.MSBuild.dll");
  }

  @Test
  public void create_new_scanner_version_using_dotnet_framework()  {
    PackageDetails packageDetails = sut.create(Version.create("6.0.0.81631"), false);

    assertThat(packageDetails.getGroupId()).isEqualTo("org.sonarsource.scanner.msbuild");
    assertThat(packageDetails.getArtifactId()).isEqualTo("sonar-scanner");
    assertThat(packageDetails.getClassifier()).isEqualTo("net-framework");
    assertThat(packageDetails.getPackageName()).isEqualTo("sonar-scanner-6.0.0.81631-net-framework");
    assertThat(packageDetails.getExecutableName()).isEqualTo("SonarScanner.MSBuild.exe");
  }
}
