/*
 * Orchestrator
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
package com.sonar.orchestrator.build;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.util.Command;
import com.sonar.orchestrator.util.CommandExecutor;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MavenBuildExecutorTest {

  private final MavenBuildExecutor.Os os = mock(MavenBuildExecutor.Os.class);
  private final MavenBuildExecutor underTest = new MavenBuildExecutor(os);
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void configure_command_in_PATH_on_linux() throws Exception {
    emulateLinux();
    Configuration configuration = Configuration.builder().setProperty("maven.binary", "mvnDebug").build();

    assertThat(underTest.buildMvnPath(configuration)).isEqualTo("mvnDebug");
  }

  @Test
  public void configure_command_in_PATH_on_windows() throws Exception {
    emulateWindows();
    Configuration configuration = Configuration.builder().setProperty("maven.binary", "mvnDebug.cmd").build();

    assertThat(underTest.buildMvnPath(configuration)).isEqualTo("mvnDebug.cmd");
  }

  @Test
  public void configure_path_to_command_on_linux() throws Exception {
    emulateLinux();
    File home = temp.newFolder();
    String binPath = new File(home, "bin/maven").getCanonicalPath();
    Configuration configuration = Configuration.builder().setProperty("maven.binary", binPath).build();

    assertThat(underTest.buildMvnPath(configuration)).isEqualTo(binPath);
  }

  @Test
  public void configure_path_to_command_on_windows() throws Exception {
    emulateWindows();
    File home = temp.newFolder();
    String binPath = new File(home, "bin/maven.bat").getCanonicalPath();
    Configuration configuration = Configuration.builder().setProperty("maven.binary", binPath).build();

    assertThat(underTest.buildMvnPath(configuration)).isEqualTo(binPath);
  }

  @Test
  public void default_command_on_linux_is_in_PATH() throws Exception {
    emulateLinux();
    Configuration configuration = Configuration.builder().build();

    assertThat(underTest.buildMvnPath(configuration)).isEqualTo("mvn");
  }

  @Test
  public void default_command_on_windows_is_in_PATH() throws Exception {
    emulateWindows();
    Configuration configuration = Configuration.builder().build();

    assertThat(underTest.buildMvnPath(configuration)).isEqualTo("mvn.cmd");
  }

  @Test
  public void configure_MAVEN_HOME_on_linux() throws Exception {
    emulateLinux();
    File home = temp.newFolder();
    Configuration configuration = Configuration.builder().setProperty("MAVEN_HOME", home.getCanonicalPath()).build();

    String result = underTest.buildMvnPath(configuration);

    assertThat(result).isEqualTo(new File(home, "bin/mvn").getCanonicalPath());
  }

  @Test
  public void configure_maven_home_on_windows() throws Exception {
    emulateWindows();
    File home = temp.newFolder();
    Configuration configuration = Configuration.builder().setProperty("MAVEN_HOME", home.getCanonicalPath()).build();

    String result = underTest.buildMvnPath(configuration);

    assertThat(result).isEqualTo(new File(home, "bin/mvn.cmd").getCanonicalPath());
  }

  @Test
  public void configure_maven_home_and_binary_on_windows() throws Exception {
    emulateWindows();
    File home = temp.newFolder();
    Configuration configuration = Configuration.builder()
      .setProperty("MAVEN_HOME", home.getCanonicalPath())
      .setProperty("MAVEN_BINARY", "mvnDebug.cmd")
      .build();

    String result = underTest.buildMvnPath(configuration);

    assertThat(result).isEqualTo(new File(home, "bin/mvnDebug.cmd").getCanonicalPath());
  }

  @Test
  public void configure_maven_home_on_linux() throws Exception {
    emulateLinux();
    File home = temp.newFolder();
    Configuration configuration = Configuration.builder().setProperty("MAVEN_HOME", home.getCanonicalPath()).build();

    String result = underTest.buildMvnPath(configuration);

    assertThat(result).isEqualTo(new File(home, "bin/mvn").getCanonicalPath());
  }

  @Test
  public void configure_maven_home_and_binary_on_linux() throws Exception {
    emulateLinux();
    File home = temp.newFolder();
    Configuration configuration = Configuration.builder()
      .setProperty("MAVEN_HOME", home.getCanonicalPath())
      .setProperty("MAVEN_BINARY", "mvnDebug")
      .build();

    String result = underTest.buildMvnPath(configuration);

    assertThat(result).isEqualTo(new File(home, "bin/mvnDebug").getCanonicalPath());
  }

  @Test
  public void support_maven_3_2_binary_on_windows() throws Exception {
    emulateWindows();
    File home = temp.newFolder();
    FileUtils.touch(new File(home, "bin/mvn.bat"));
    Configuration configuration = Configuration.builder().setProperty("MAVEN_HOME", home.getCanonicalPath()).build();

    String result = underTest.buildMvnPath(configuration);

    assertThat(result).isEqualTo(new File(home, "bin/mvn.bat").getCanonicalPath());
  }

  @Test
  public void shouldStoreLogs() {
    Location pom = FileLocation.of(getClass().getResource("/com/sonar/orchestrator/build/MavenBuildTest/pom.xml"));
    MavenBuild build = MavenBuild.create(pom).addGoal("clean");

    Configuration config = Configuration.createEnv();
    BuildResult result = new MavenBuildExecutor().execute(build, config, new Locators(config), new HashMap<>());

    assertThat(result.getLogs().length()).isGreaterThan(0);
    assertThat(result.getLogs()).containsSubsequence("[INFO] Scanning for projects...", "[INFO] Total time");
  }

  // ORCH-179

  @Test
  public void shouldPassAdditionalArguments() {
    Location pom = FileLocation.of(getClass().getResource("/com/sonar/orchestrator/build/MavenBuildTest/pom.xml"));
    MavenBuild build = MavenBuild.create(pom).addGoal("clean").addArguments("-PnotExists");

    Configuration config = Configuration.createEnv();
    BuildResult result = new MavenBuildExecutor().execute(build, config, new Locators(config), new HashMap<>());

    assertThat(result.getLogs().length()).isGreaterThan(0);
    assertThat(result.getLogs()).contains("[WARNING] The requested profile \"notExists\" could not be activated because it does not exist.");
  }

  @Test
  public void execute_command() throws Exception {
    File pom = new File(getClass().getResource("/com/sonar/orchestrator/build/MavenBuildTest/pom.xml").toURI());
    MavenBuild build = MavenBuild.create(pom)
      .addGoal("clean")
      .addSonarGoal()
      .setDebugLogs(true)
      .setTimeoutSeconds(30);
    Map<String, String> props = new TreeMap<>();
    props.put("sonar.jdbc.dialect", "h2");

    CommandExecutor executor = mock(CommandExecutor.class);
    when(executor.execute(any(), any(), anyLong())).thenReturn(2);

    Configuration config = Configuration.create();
    new MavenBuildExecutor().execute(build, config, new Locators(config), props, executor);

    verify(executor).execute(argThat(mvnMatcher(pom, "clean")), any(), eq(30000L));
    verify(executor).execute(argThat(mvnMatcher(pom, "org.sonarsource.scanner.maven:sonar-maven-plugin:4.0.0.4121:sonar")), any(), eq(30000L));
  }

  private ArgumentMatcher<Command> mvnMatcher(final File pom, final String goal) {
    return c -> {
      // Windows directory with space use case
      String quote = "";
      if (pom.getAbsolutePath().contains(" ")) {
        quote = "\"";
      }
      return c.toCommandLine().contains("mvn")
        && c.toCommandLine().contains("-f " + quote + pom.getAbsolutePath())
        && c.toCommandLine().contains("-X")
        && c.toCommandLine().contains(goal);
    };
  }

  private void emulateWindows() {
    when(os.isWindows()).thenReturn(true);
  }

  private void emulateLinux() {
    when(os.isWindows()).thenReturn(false);
  }
}
