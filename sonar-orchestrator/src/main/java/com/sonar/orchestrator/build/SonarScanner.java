/*
 * Orchestrator
 * Copyright (C) 2011-2021 SonarSource SA
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
package com.sonar.orchestrator.build;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang.SystemUtils;

/**
 * Executes the sonar-runner script. In-process mode is not supported yet.
 *
 * @since 3.8
 */
public class SonarScanner extends SonarRunner {

  private static final Map<String, String> ENV_VARIABLES;
  static {
    Map<String, String> map = new HashMap<>();
    map.put("SONAR_RUNNER_OPTS", "-Djava.awt.headless=true");
    ENV_VARIABLES = Collections.unmodifiableMap(map);
  }

  private boolean useOldSonarRunnerScript = false;
  private String classifier = null;

  SonarScanner() {
  }

  @Override
  protected Map<String, String> doGetEnvironmentVariablePrefixes() {
    // http://jira.sonarsource.com/browse/ORCH-256
    // Temporarily hardcoded in Orchestrator meanwhile sonar-runner 2.5
    return ENV_VARIABLES;
  }

  @Override
  public SonarScanner setEnvironmentVariable(String name, String value) {
    return (SonarScanner) super.setEnvironmentVariable(name, value);
  }

  /**
   * @deprecated since 3.11 use {@link #setScannerVersion(String)}
   */
  @Deprecated
  @Override
  public SonarScanner setRunnerVersion(String s) {
    return (SonarScanner) super.setRunnerVersion(s);
  }

  /**
   * @since 3.11
   */
  public Version scannerVersion() {
    return runnerVersion();
  }

  /**
   * @since 3.11
   */
  public SonarScanner setScannerVersion(String s) {
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

  /**
   * @since 3.11 used by SQ Scanner CLI ITs
   */
  public SonarScanner setUseOldSonarRunnerScript(boolean useOldSonarRunnerScript) {
    this.useOldSonarRunnerScript = useOldSonarRunnerScript;
    return this;
  }

  @Override
  public boolean isUseOldSonarRunnerScript() {
    return !scannerVersion().isGreaterThanOrEquals(2, 6) || useOldSonarRunnerScript;
  }

  /**
   * @since 3.15 used by SQ Scanner CLI ITs
   */
  public SonarScanner useNative() {
    this.classifier = determineClassifier();
    return this;
  }

  private static String determineClassifier() {
    if (SystemUtils.IS_OS_LINUX) {
      return "linux";
    }
    if (SystemUtils.IS_OS_WINDOWS) {
      return "windows";
    }
    if (SystemUtils.IS_OS_MAC_OSX) {
      return "macosx";
    }
    throw new IllegalStateException("Unsupported OS: only Linux, Windows and Mac OS X are supported");
  }

  @Override
  public String classifier() {
    return classifier;
  }

  public static SonarScanner create() {
    return new SonarScanner()
      // default value
      .setProperty(PROP_KEY_SOURCE_ENCODING, DEFAULT_SOURCE_ENCODING);
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
