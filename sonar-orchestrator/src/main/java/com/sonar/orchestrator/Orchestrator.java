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
package com.sonar.orchestrator;

import com.sonar.orchestrator.build.Build;
import com.sonar.orchestrator.build.BuildCache;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.BuildRunner;
import com.sonar.orchestrator.build.ScannerReportModifier;
import com.sonar.orchestrator.build.ScannerReportSubmitter;
import com.sonar.orchestrator.build.SynchronousAnalyzer;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.config.Licenses;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.container.SonarDistribution;
import com.sonar.orchestrator.db.Database;
import com.sonar.orchestrator.db.DefaultDatabase;
import com.sonar.orchestrator.http.HttpCall;
import com.sonar.orchestrator.http.HttpMethod;
import com.sonar.orchestrator.http.HttpResponse;
import com.sonar.orchestrator.junit.SingleStartExternalResource;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.server.PackagingResolver;
import com.sonar.orchestrator.server.ServerCommandLineFactory;
import com.sonar.orchestrator.server.ServerInstaller;
import com.sonar.orchestrator.server.ServerProcess;
import com.sonar.orchestrator.server.ServerProcessImpl;
import com.sonar.orchestrator.server.StartupLogWatcher;
import com.sonar.orchestrator.util.ZipUtils;
import org.sonar.scanner.protocol.output.ScannerReportWriter;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public class Orchestrator extends SingleStartExternalResource {

  private static final String ORCHESTRATOR_IS_NOT_STARTED = "Orchestrator is not started";

  private final Configuration config;
  private final SonarDistribution distribution;
  private final Licenses licenses;
  private final AtomicBoolean started = new AtomicBoolean(false);

  private DefaultDatabase database;
  private Server server;
  private BuildRunner buildRunner;
  private ServerProcess process;
  private StartupLogWatcher startupLogWatcher;

  /**
   * Constructor, but use rather OrchestratorBuilder
   */
  Orchestrator(Configuration config, SonarDistribution distribution, @Nullable StartupLogWatcher startupLogWatcher) {
    this.config = requireNonNull(config);
    this.distribution = requireNonNull(distribution);
    this.licenses = new Licenses(config);
    this.startupLogWatcher = startupLogWatcher;
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
    if (server == null) {
      database = new DefaultDatabase(config);
      database.start();

      PackagingResolver packagingResolver = new PackagingResolver(config.locators());
      ServerInstaller serverInstaller = new ServerInstaller(packagingResolver, config, config.locators(), database.getClient());
      server = serverInstaller.install(distribution);
    }
    return server;
  }

  /**
   * Install ans start SonarQube.
   * <p>
   * Steps are:
   * 1/ connect to db
   * 2/ download an install SonarQube server
   * 3/ download and install plugins
   * 4/ start SonarQube server
   * 5/ restore Quality profiles, if any
   */
  public void start() {
    if (started.getAndSet(true)) {
      throw new IllegalStateException("Orchestrator is already started");
    }

    install();

    FileSystem fs = config.fileSystem();
    process = new ServerProcessImpl(new ServerCommandLineFactory(fs), server, startupLogWatcher);
    process.start();

    for (Location backup : distribution.getProfileBackups()) {
      server.restoreProfile(backup);
    }

    if (distribution.isActivateLicense()) {
      activateLicense();
    }

    buildRunner = new BuildRunner(server, config);
  }

  /**
   * Set a test license that work for all commercial products
   *
   * @since 3.15
   */
  public void activateLicense() {
    String license = licenses.getLicense(server.getEdition(), server.version());
    configureLicense(license);
  }

  /**
   * Removes the license that have been installed with
   * {@link OrchestratorBuilder#activateLicense()}
   *
   * @since 3.15
   */
  public void clearLicense() {
    configureLicense(null);
  }

  private void configureLicense(@Nullable String license) {
    HttpCall httpCall = server.newHttpCall(license == null ? "api/editions/unset_license" : "api/editions/set_license")
      .setMethod(HttpMethod.POST)
      .setAdminCredentials();
    if (license != null) {
      httpCall.setParam("license", license);
    }
    httpCall.execute();
  }

  /**
   * Put down testing infrastructure
   * 1/ sonarserver
   * 2/ database
   */
  public void stop() {
    if (!started.getAndSet(false)) {
      // ignore double-stop
      return;
    }

    if (process != null) {
      process.stop();
    }
    if (database != null) {
      database.stop();
    }

    buildRunner.clearCache();
  }

  /**
   * restart of the sonarQube server
   */
  public void restartServer() {
    if (process != null) {
      process.stop();
      process.start();
    }
  }

  public Database getDatabase() {
    return database;
  }

  public Configuration getConfiguration() {
    return config;
  }

  public Server getServer() {
    return server;
  }

  /**
   * File located in the shared directory defined by the system property orchestrator.it_sources or environment variable SONAR_IT_SOURCES.
   * Example : getFileLocationOfShared("javascript/performancing/pom.xml")
   */
  public FileLocation getFileLocationOfShared(String relativePath) {
    return config.getFileLocationOfShared(relativePath);
  }

  public SonarDistribution getDistribution() {
    return distribution;
  }

  public BuildResult executeBuild(Build<?> build) {
    return executeBuild(build, true);
  }

  public BuildResult executeBuild(Build<?> build, boolean waitForComputeEngine) {
    return executeBuildInternal(build, false, waitForComputeEngine, null);
  }

  public BuildResult executeBuildQuietly(Build<?> build) {
    return executeBuildQuietly(build, true);
  }

  public BuildResult executeBuildQuietly(Build<?> build, boolean waitForComputeEngine) {
    return executeBuildInternal(build, true, waitForComputeEngine, null);
  }

  public BuildResult executeBuildWithCache(Build<?> build, String cacheId) {
    return executeBuildWithCache(build, false, true, cacheId);
  }

  public BuildResult executeBuildWithCache(Build<?> build, boolean quietly, boolean waitForComputeEngine, String cacheId) {
    return executeBuildInternal(build, quietly, waitForComputeEngine, cacheId);
  }

  private BuildResult executeBuildInternal(Build<?> build, boolean quietly, boolean waitForComputeEngine, @Nullable String cacheId) {
    requireNonNull(buildRunner, ORCHESTRATOR_IS_NOT_STARTED);

    BuildResult buildResult;
    if (quietly) {
      buildResult = buildRunner.runQuietly(build, cacheId);
    } else {
      buildResult = buildRunner.run(build, cacheId);
    }
    if (waitForComputeEngine) {
      new SynchronousAnalyzer(server).waitForDone();
    }
    return buildResult;
  }

  public BuildResult[] executeBuilds(Build<?>... builds) {
    requireNonNull(buildRunner, ORCHESTRATOR_IS_NOT_STARTED);

    BuildResult[] results = new BuildResult[builds.length];
    for (int index = 0; index < builds.length; index++) {
      results[index] = buildRunner.run(builds[index]);
    }
    new SynchronousAnalyzer(server).waitForDone();
    return results;
  }

  public static OrchestratorBuilder builderEnv() {
    return new OrchestratorBuilder(Configuration.createEnv());
  }

  public static OrchestratorBuilder builder(Configuration config) {
    return new OrchestratorBuilder(config);
  }
}
