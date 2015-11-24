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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Executes the sonar-runner script. In-process mode is not supported yet.
 *
 * @since 2.1
 * @deprecated since 3.8 use {@link SonarScanner}
 */
@Deprecated
public class SonarRunner extends Build<SonarRunner> {

  public static final String DEFAULT_RUNNER_VERSION = "2.4";
  public static final String PROP_KEY_SOURCE_ENCODING = "sonar.sourceEncoding";
  public static final String DEFAULT_SOURCE_ENCODING = "UTF-8";
  public static final boolean DEFAULT_SCM_DISABLED = true;

  private Version runnerVersion = Version.create(DEFAULT_RUNNER_VERSION);
  private File projectDir;
  private boolean debugLogs = false;
  private boolean showErrors = true;
  private String task;

  SonarRunner() {
  }

  @Override
  protected Map<String, String> doGetEnvironmentVariablePrefixes() {
    // http://jira.sonarsource.com/browse/ORCH-256
    // Temporarily hardcoded in Orchestrator meanwhile sonar-runner 2.5
    return ImmutableMap.of("SONAR_RUNNER_OPTS", "-Djava.awt.headless=true");
  }

  public Version runnerVersion() {
    return runnerVersion;
  }

  public File getProjectDir() {
    return projectDir;
  }

  public boolean isDebugLogs() {
    return debugLogs;
  }

  public boolean isShowErrors() {
    return showErrors;
  }

  public String getTask() {
    return task;
  }

  public SonarRunner setRunnerVersion(String s) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(s), "sonar-runner version must be set");
    this.runnerVersion = Version.create(s);
    return this;
  }

  public SonarRunner setProjectDir(File dir) {
    checkProjectDir(dir);
    this.projectDir = dir;
    return this;
  }

  public SonarRunner setProjectKey(@Nullable String s) {
    return setProperty("sonar.projectKey", s);
  }

  public SonarRunner setProjectVersion(@Nullable String s) {
    return setProperty("sonar.projectVersion", s);
  }

  public SonarRunner setProjectName(@Nullable String s) {
    return setProperty("sonar.projectName", s);
  }

  public SonarRunner setSourceDirs(@Nullable String s) {
    return setProperty("sonar.sources", s);
  }

  public SonarRunner setTestDirs(@Nullable String testDirs) {
    return setProperty("sonar.tests", testDirs);
  }

  public SonarRunner setBinaries(@Nullable String s) {
    return setProperty("sonar.binaries", s);
  }

  public SonarRunner setLibraries(@Nullable String s) {
    return setProperty("sonar.libraries", s);
  }

  public SonarRunner setLanguage(@Nullable String s) {
    return setProperty("sonar.language", s);
  }

  public SonarRunner setSourceEncoding(@Nullable String s) {
    if (s == null) {
      return setProperty(PROP_KEY_SOURCE_ENCODING, DEFAULT_SOURCE_ENCODING);
    } else {
      return setProperty(PROP_KEY_SOURCE_ENCODING, s);
    }

  }

  public SonarRunner setDebugLogs(boolean b) {
    this.debugLogs = b;
    return this;
  }

  public SonarRunner setShowErrors(boolean b) {
    this.showErrors = b;
    return this;
  }

  public SonarRunner setTask(String task) {
    this.task = task;
    return this;
  }

  public static SonarRunner create() {
    return new SonarRunner()
      // default value
      .setProperty(PROP_KEY_SOURCE_ENCODING, DEFAULT_SOURCE_ENCODING)
      .setProperty("sonar.scm.disabled", String.valueOf(DEFAULT_SCM_DISABLED));
  }

  public static SonarRunner create(File projectDir, String... keyValueProperties) {
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

  void check() {
    checkProjectDir(projectDir);
  }

  private static void checkProjectDir(File dir) {
    Preconditions.checkNotNull(dir, "Project directory must be set");
    Preconditions.checkArgument(dir.exists(), "Project directory must exist");
    Preconditions.checkArgument(dir.isDirectory(), "Project directory must be... a directory");
  }
}
