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
package com.sonar.orchestrator.build;

import com.google.common.collect.ImmutableMap;
import com.sonar.orchestrator.config.Configuration;
import java.io.File;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Executes the sonar-runner script. In-process mode is not supported yet.
 *
 * @since 2.1
 */
public class SonarScanner extends SonarRunner {

  SonarScanner() {
  }

  @Override
  protected Map<String, String> doGetEnvironmentVariablePrefixes() {
    // http://jira.sonarsource.com/browse/ORCH-256
    // Temporarily hardcoded in Orchestrator meanwhile sonar-runner 2.5
    return ImmutableMap.of("SONAR_RUNNER_OPTS", "-Djava.awt.headless=true");
  }

  @Override
  public SonarScanner setRunnerVersion(String s) {
    return (SonarScanner) super.setRunnerVersion(s);
  }

  @Override
  public SonarScanner setProjectDir(File dir) {
    return (SonarScanner) super.setProjectDir(dir);
  }

  @Override
  public SonarScanner setProjectKey(@Nullable String s) {
    return (SonarScanner) super.setProjectKey(s);
  }

  @Override
  public SonarScanner setProjectVersion(@Nullable String s) {
    return (SonarScanner) super.setProjectVersion(s);
  }

  @Override
  public SonarScanner setProjectName(@Nullable String s) {
    return (SonarScanner) super.setProjectName(s);
  }

  @Override
  public SonarScanner setSourceDirs(@Nullable String s) {
    return (SonarScanner) super.setSourceDirs(s);
  }

  @Override
  public SonarScanner setTestDirs(@Nullable String testDirs) {
    return (SonarScanner) super.setTestDirs(testDirs);
  }

  /**
   * @deprecated since 3.8 sonar.binaries is deprecated
   */
  @Deprecated
  @Override
  public SonarScanner setBinaries(@Nullable String s) {
    return (SonarScanner) super.setBinaries(s);
  }

  /**
   * @deprecated since 3.8 sonar.libraries is deprecated
   */
  @Deprecated
  @Override
  public SonarScanner setLibraries(@Nullable String s) {
    return (SonarScanner) super.setLibraries(s);
  }

  @Override
  public SonarScanner setLanguage(@Nullable String s) {
    return (SonarScanner) super.setLanguage(s);
  }

  @Override
  public SonarScanner setSourceEncoding(@Nullable String s) {
    return (SonarScanner) super.setSourceEncoding(s);
  }

  @Override
  public SonarScanner setDebugLogs(boolean b) {
    return (SonarScanner) super.setDebugLogs(b);
  }

  @Override
  public SonarScanner setShowErrors(boolean b) {
    return (SonarScanner) super.setShowErrors(b);
  }

  @Override
  public SonarScanner setTask(String task) {
    return (SonarScanner) super.setTask(task);
  }

  @Override
  public SonarScanner setProfile(String profileKey) {
    return (SonarScanner) super.setProfile(profileKey);
  }

  @Override
  public SonarScanner addArgument(String additionalArgument) {
    return (SonarScanner) super.addArgument(additionalArgument);
  }

  @Override
  public SonarScanner addArguments(List<String> additionalArguments) {
    return (SonarScanner) super.addArguments(additionalArguments);
  }

  @Override
  public SonarScanner addArguments(String... additionalArguments) {
    return (SonarScanner) super.addArguments(additionalArguments);
  }

  @Override
  public SonarScanner setProperty(String key, String value) {
    return (SonarScanner) super.setProperty(key, value);
  }

  @Override
  public SonarScanner setProperties(Map<String, String> p) {
    return (SonarScanner) super.setProperties(p);
  }

  @Override
  public SonarScanner setProperties(String... keyValues) {
    return (SonarScanner) super.setProperties(keyValues);
  }

  public static SonarScanner create() {
    return new SonarScanner()
      // default value
      .setProperty(PROP_KEY_SOURCE_ENCODING, DEFAULT_SOURCE_ENCODING)
      .setProperty("sonar.scm.disabled", String.valueOf(DEFAULT_SCM_DISABLED));
  }

  public static SonarScanner create(File projectDir, String... keyValueProperties) {
    return
    // default value
      create()
        // incoming values
        .setProjectDir(projectDir)
        .setProperties(keyValueProperties);
  }

  @Override
  BuildResult execute(Configuration config, Map<String, String> adjustedProperties) {
    check();
    return new SonarScannerExecutor().execute(this, config, adjustedProperties);
  }

}
