/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package com.sonar.orchestrator.coverage;

import com.sonar.orchestrator.config.Configuration;
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

  private JaCoCoArgumentsBuilder() {
    // prevents instantiation
  }

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
  public static String getJaCoCoArgument(Configuration config) {
    String computeCoverage = config.getString("orchestrator.computeCoverage", "false");
    if (!"true".equals(computeCoverage)) {
      return null;
    }

    String destFile = config.getString("orchestrator.coverageReportPath", "target/jacoco.exec");
    destFile = FilenameUtils.separatorsToUnix(destFile);

    File jacocoLocation = config.locators().locate(agentLocation);
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
