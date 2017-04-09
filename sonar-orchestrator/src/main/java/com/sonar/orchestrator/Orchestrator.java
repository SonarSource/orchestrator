/*
 * Orchestrator
 * Copyright (C) 2011-2017 SonarSource SA
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

import com.google.common.base.Preconditions;
import com.sonar.orchestrator.build.Build;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.BuildRunner;
import com.sonar.orchestrator.build.SynchronousAnalyzer;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.config.Licenses;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.container.SonarDistribution;
import com.sonar.orchestrator.db.Database;
import com.sonar.orchestrator.db.DefaultDatabase;
import com.sonar.orchestrator.http.HttpMethod;
import com.sonar.orchestrator.junit.SingleStartExternalResource;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.server.ServerCommandLineFactory;
import com.sonar.orchestrator.server.ServerInstaller;
import com.sonar.orchestrator.server.ServerProcess;
import com.sonar.orchestrator.server.ServerProcessImpl;
import com.sonar.orchestrator.server.ServerZipFinder;
import com.sonar.orchestrator.server.StartupLogWatcher;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.services.PropertyUpdateQuery;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.sonar.orchestrator.container.Server.ADMIN_LOGIN;
import static com.sonar.orchestrator.container.Server.ADMIN_PASSWORD;
import static java.util.Objects.requireNonNull;

public class Orchestrator extends SingleStartExternalResource {

  private static final Logger LOG = LoggerFactory.getLogger(Orchestrator.class);

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
    this.licenses = new Licenses();
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
   * Make ready SonarQube infrastructure according to the settings and options.
   * A call to this method is usually made just after the instanciation of Orchestrator
   *
   * Steps are:
   * 1/ connects the db
   * 2/ downloads an install SonarQube server on next available port
   * 3/ starts SonarQube server on next available port
   * 4/ sets profile
   * 5/ download and install plugins
    */
  public void start() {
    if (started.getAndSet(true)) {
      throw new IllegalStateException("Orchestrator is already started");
    }

    database = new DefaultDatabase(config);
    database.start();

    FileSystem fs = config.fileSystem();
    ServerZipFinder zipFinder = new ServerZipFinder(fs, config.updateCenter());
    ServerInstaller serverInstaller = new ServerInstaller(zipFinder, config, database.getClient());
    server = serverInstaller.install(distribution);

    process = new ServerProcessImpl(new ServerCommandLineFactory(fs), server, startupLogWatcher);
    process.start();

    for (Location backup : distribution.getProfileBackups()) {
      server.restoreProfile(backup);
    }

    for (String pluginKey : distribution.getLicensedPluginKeys()) {
      String license = licenses.get(pluginKey);
      if (!isNullOrEmpty(license)) {
        updateSetting(licenses.licensePropertyKey(pluginKey), license);
      }
    }

    buildRunner = new BuildRunner(config);
  }

  private void updateSetting(String key, String value) {
    if (getServer().version().isGreaterThanOrEquals("6.1")) {
      server.newHttpCall("/api/settings/set")
        .setMethod(HttpMethod.POST)
        .setAdminCredentials()
        .setParam("key", key)
        .setParam("value", value)
        .execute();
    } else {
      server.getAdminWsClient().update(new PropertyUpdateQuery(key, value));
    }
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
    return executeBuildInternal(build, false, waitForComputeEngine);
  }

  public BuildResult executeBuildQuietly(Build<?> build) {
    return executeBuildQuietly(build, true);
  }

  public BuildResult executeBuildQuietly(Build<?> build, boolean waitForComputeEngine) {
    return executeBuildInternal(build, true, waitForComputeEngine);
  }

  private BuildResult executeBuildInternal(Build<?> build, boolean quietly, boolean waitForComputeEngine) {
    Preconditions.checkNotNull(buildRunner, ORCHESTRATOR_IS_NOT_STARTED);

    BuildResult buildResult;
    if (quietly) {
      buildResult = buildRunner.runQuietly(server, build);
    } else {
      buildResult = buildRunner.run(server, build);
    }
    if (waitForComputeEngine) {
      new SynchronousAnalyzer(server).waitForDone();
    }
    return buildResult;
  }

  public BuildResult[] executeBuilds(Build<?>... builds) {
    Preconditions.checkNotNull(buildRunner, ORCHESTRATOR_IS_NOT_STARTED);

    BuildResult[] results = new BuildResult[builds.length];
    for (int index = 0; index < builds.length; index++) {
      results[index] = buildRunner.run(server, builds[index]);
    }
    new SynchronousAnalyzer(server).waitForDone();
    return results;
  }

  /**
   * Reset inspection measures and some other data (manual rules, etc.)
   */
  public void resetData() {
    LOG.info("Reset data");
    // temporary increase timeout - experimental test for SonarSource environment
    SonarClient client = SonarClient.builder()
      .url(server.getUrl())
      .login(ADMIN_LOGIN)
      .password(ADMIN_PASSWORD)
      .connectTimeoutMilliseconds(300_000)
      .readTimeoutMilliseconds(600_000).build();
    client.post("/api/orchestrator/reset");
  }

  /**
   * Use environment variables and system properties
   */
  public static Orchestrator createEnv() {
    return new OrchestratorBuilder(Configuration.createEnv()).build();
  }

  public static OrchestratorBuilder builderEnv() {
    return new OrchestratorBuilder(Configuration.createEnv());
  }

  public static OrchestratorBuilder builder(Configuration config) {
    return new OrchestratorBuilder(config);
  }

  public Licenses getLicenses() {
    return licenses;
  }
}
