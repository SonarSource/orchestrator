/*
 * Orchestrator Build
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
package com.sonar.orchestrator.build.coverage;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.util.Properties;
import org.apache.commons.io.FilenameUtils;

public class JaCoCoArgumentsBuilder {

  private static final String EXCLUDES = "*_javassist_*";
  private static final String INCLUDES = "*sonar*";

  /**
   * Loaded from pom.xml
   */
  static String jaCoCoVersion;
  static MavenLocation agentLocation;

  static {
    Properties props = readProperties("jacoco.properties");
    jaCoCoVersion = props.getProperty("jacoco.version");
    agentLocation = MavenLocation.builder()
      .setGroupId("org.jacoco")
      .setArtifactId("org.jacoco.agent")
      .setVersion(jaCoCoVersion)
      .setClassifier("runtime")
      .build();
  }

  private JaCoCoArgumentsBuilder() {
    // prevents instantiation
  }

  static Properties readProperties(String propertyFileName) {
    Properties props = new Properties();
    try {
      props.load(JaCoCoArgumentsBuilder.class.getResourceAsStream(propertyFileName));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to load JaCoCo version to be used", e);
    }
    return props;
  }

  /**
   * Return the JVM argument to be added for JaCoCo code coverage analysis.
   * @param config
   * @return null if code coverage is not enabled
   */
  public static String getJaCoCoArgument(Configuration config, Locators locators) {
    String computeCoverage = config.getString("orchestrator.computeCoverage", "false");
    if (!"true".equals(computeCoverage)) {
      return null;
    }

    String destFile = config.getString("orchestrator.coverageReportPath", "target/jacoco.exec");
    destFile = FilenameUtils.separatorsToUnix(destFile);

    File jacocoLocation = locators.locate(agentLocation);
    if (jacocoLocation == null) {
      throw new IllegalStateException("Unable to locate jacoco: " + agentLocation + " in " + config.fileSystem().mavenLocalRepository());
    }

    String agentPath = FilenameUtils.separatorsToUnix(jacocoLocation.getAbsolutePath());

    return new StringBuilder("-javaagent:")
      .append(agentPath)
      .append("=destfile=").append(destFile).append(",")
      .append("append=true,")
      .append("excludes=").append(EXCLUDES).append(",")
      .append("includes=").append(INCLUDES)
      .toString();
  }
}
