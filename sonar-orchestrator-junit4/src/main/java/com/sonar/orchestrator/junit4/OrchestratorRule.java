/*
 * Orchestrator - JUnit 4
 * Copyright (C) 2011-2025 SonarSource SA
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
package com.sonar.orchestrator.junit4;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.Build;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.container.SonarDistribution;
import com.sonar.orchestrator.db.Database;
import com.sonar.orchestrator.locator.FileLocation;

/**
 * To be used as a JUnit 4 {@link org.junit.Rule} or {@link org.junit.ClassRule}. For example:
 * <pre>{@code
 * public class MyTest {
 *
 *   @ClassRule
 *   public static final OrchestratorRule ORCHESTRATOR = OrchestratorRule.builderEnv()
 *     .setSonarVersion("LATEST_RELEASE")
 *     .addPlugin(MavenLocation.of("org.sonarsource.html", "sonar-html-plugin", "DEV"))
 *     .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-jsp.xml"))
 *     .build();
 * }
 * }</pre>
 */
public class OrchestratorRule extends SingleStartExternalResource {

  private final Orchestrator orchestrator;

  /**
   * Constructor, but use rather OrchestratorBuilder
   */
  OrchestratorRule(Orchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  @Override
  protected void beforeAll() {
    start();
  }

  @Override
  protected void afterAll() {
    stop();
  }

  /**
   * Install SonarQube without starting it.
   * For advanced usage only! It allows to tweak file system before startup.
   *
   * @since 3.19
   */
  public Server install() {
    return orchestrator.install();
  }

  /**
   * Install and start SonarQube.
   * <p>
   * Steps are:
   * 1/ connect to db
   * 2/ download and install SonarQube server
   * 3/ download and install plugins
   * 4/ start SonarQube server
   * 5/ restore Quality profiles, if any
   */
  public void start() {
    orchestrator.start();
  }

  /**
   * Set a test license that work for all commercial products
   *
   * @since 3.15
   */
  public void activateLicense() {
    orchestrator.activateLicense();
  }

  /**
   * Removes the license that have been installed with
   * {@link OrchestratorRuleBuilder#activateLicense()}
   *
   * @since 3.15
   */
  public void clearLicense() {
    orchestrator.clearLicense();
  }

  /**
   * Put down testing infrastructure
   * 1/ sonarserver
   * 2/ database
   */
  public void stop() {
    orchestrator.stop();
  }

  /**
   * restart of the sonarQube server
   */
  public void restartServer() {
    orchestrator.restartServer();
  }

  public Database getDatabase() {
    return orchestrator.getDatabase();
  }

  public Configuration getConfiguration() {
    return orchestrator.getConfiguration();
  }

  public Server getServer() {
    return orchestrator.getServer();
  }

  /**
   * File located in the shared directory defined by the system property orchestrator.it_sources or environment variable SONAR_IT_SOURCES.
   * Example : getFileLocationOfShared("javascript/performancing/pom.xml")
   */
  public FileLocation getFileLocationOfShared(String relativePath) {
    return orchestrator.getFileLocationOfShared(relativePath);
  }

  public SonarDistribution getDistribution() {
    return orchestrator.getDistribution();
  }

  public BuildResult executeBuild(Build<?> build) {
    return executeBuild(build, true);
  }

  public BuildResult executeBuild(Build<?> build, boolean waitForComputeEngine) {
    return orchestrator.executeBuild(build, waitForComputeEngine);
  }

  public BuildResult executeBuildQuietly(Build<?> build) {
    return executeBuildQuietly(build, true);
  }

  public BuildResult executeBuildQuietly(Build<?> build, boolean waitForComputeEngine) {
    return orchestrator.executeBuildQuietly(build, waitForComputeEngine);
  }

  public String getDefaultAdminToken() {
    return orchestrator.getDefaultAdminToken();
  }

  public BuildResult[] executeBuilds(Build<?>... builds) {
    return orchestrator.executeBuilds(builds);
  }

  public static OrchestratorRuleBuilder builderEnv() {
    return builder(Configuration.createEnv());
  }

  public static OrchestratorRuleBuilder builder(Configuration config) {
    return new OrchestratorRuleBuilder(config);
  }

  public Orchestrator getOrchestrator() {
    return orchestrator;
  }
}
