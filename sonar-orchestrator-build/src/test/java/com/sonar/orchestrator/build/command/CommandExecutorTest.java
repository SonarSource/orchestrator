/*
 * Orchestrator Build
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
package com.sonar.orchestrator.build.command;

import com.sonar.orchestrator.build.util.StreamConsumer;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class CommandExecutorTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Rule
  public TestName testName = new TestName();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private File workDir;

  @Before
  public void setUp() throws IOException {
    workDir = tempFolder.newFolder(testName.getMethodName());
  }

  @Test
  public void should_consume_stream() throws Exception {
    final StringBuilder stdOutBuilder = new StringBuilder();
    StreamConsumer streamConsumer = new StreamConsumer() {
      @Override
      public void consumeLine(String line) {
        stdOutBuilder.append(line).append(System.lineSeparator());
      }
    };
    Command command = Command.create(getScript("output")).setDirectory(workDir);
    int exitCode = CommandExecutor.create().execute(command, streamConsumer, 1000L);
    assertThat(exitCode).isZero();

    String stdOut = stdOutBuilder.toString();
    assertThat(stdOut)
      .contains("stdOut: first line")
      .contains("stdOut: second line");
  }

  @Test
  public void stream_consumer_can_throw_exception() throws Exception {
    Command command = Command.create(getScript("output")).setDirectory(workDir);
    thrown.expect(CommandException.class);
    CommandExecutor.create().execute(command, BAD_CONSUMER, 1000L);
  }

  private static final StreamConsumer BAD_CONSUMER = new StreamConsumer() {
    @Override
    public void consumeLine(String line) {
      throw new RuntimeException();
    }
  };

  @Test
  public void should_stop_after_timeout() throws IOException {
    String executable = getScript("forever");
    long start = System.currentTimeMillis();
    try {
      CommandExecutor.create().execute(Command.create(executable).setDirectory(workDir), 300L);
      fail();
    } catch (CommandException e) {
      long duration = System.currentTimeMillis() - start;
      // Future.get(), which is used by CommandExecutor, has not a precise timeout.
      // See http://stackoverflow.com/questions/23199820/future-get-timeout-precision-and-possible-alternatives
      // The deviation is in both directions, so it implies to test something like >270ms instead of >300ms
      assertThat(duration).as(e.getMessage()).isGreaterThan(240L);
    }
  }

  @Test
  public void should_fail_null_command_not_allowed() {
    thrown.expect(NullPointerException.class);
    CommandExecutor.create().execute(null, 1000L);
  }

  @Test
  public void should_fail_on_linux_if_script_not_found() {
    if (!SystemUtils.IS_OS_WINDOWS) {
      thrown.expect(CommandException.class);
    }
    CommandExecutor.create().execute(Command.create("notfound").setDirectory(workDir), 1000L);
  }

  @Test
  public void escape_arguments() throws Exception {
    File outputDir = tempFolder.newFolder();
    String separator = System.getProperty("file.separator");
    File javaExec = new File(System.getProperty("java.home"), "bin" + separator + "java");

    Command command = Command.create(javaExec.getCanonicalPath());
    // system properties
    command.addArgument("-Dprop.standard=val1");
    command.addArgument("-Dprop.whitespace=white space");
    command.addArgument("-Dprop.special=special;:&<characters>");
    command.addArgument("-Dprop.backslash=c:\\path");
    command.addArgument("-Dprop.quotes=single'quote");

    command.addArgument("-jar");
    command.addArgument(getFile("../echo/target", "echo-*.jar").getAbsolutePath());
    command.setDirectory(outputDir);

    // arguments
    command.addArgument("foo");
    command.addArgument("amper&sand");
    command.addArgument("white space");
    command.addArgument("comma;colon:");
    command.addArgument("<foo>");
    command.addArgument("c:\\path");
    command.addArgument("'single quotes'");

    CommandExecutor.create().execute(command, 2000L);

    List<String> args = FileUtils.readLines(new File(outputDir, "arguments.txt"), StandardCharsets.UTF_8);
    assertThat(args).contains("foo");
    assertThat(args).contains("amper&sand");
    assertThat(args).contains("white space").doesNotContain("white", "space");
    assertThat(args).contains("comma;colon:");
    assertThat(args).contains("<foo>");
    assertThat(args).contains("c:\\path");
    assertThat(args).contains("'single quotes'");

    Properties props = new Properties();
    FileReader fileReader = new FileReader(new File(outputDir, "sysprops.txt"));
    props.load(fileReader);
    fileReader.close();
    assertThat(props.getProperty("prop.standard")).isEqualTo("val1");
    assertThat(props.getProperty("prop.whitespace")).isEqualTo("white space");
    assertThat(props.getProperty("prop.special")).isEqualTo("special;:&<characters>");
    assertThat(props.getProperty("prop.backslash")).isEqualTo("c:\\path");
    assertThat(props.getProperty("prop.quotes")).isEqualTo("single'quote");
  }

  @Test
  public void should_not_contain_removed_environment_variable() throws IOException {
    Set<String> outputEnv = new HashSet<>();
    StreamConsumer streamConsumer = line -> {
      outputEnv.add(line.split("=")[0]);
    };
    Command command = Command.create(getScript("env"));
    Map<String, String> env = command.getEnvironmentVariables();
    String varKey = env.keySet().iterator().next();
    command.removeEnvironmentVariable(varKey);

    CommandExecutor.create().execute(command, streamConsumer, 1000L);

    assertThat(outputEnv).doesNotContain(varKey);
  }

  @Test
  public void should_have_only_required_environment_variables() throws IOException {
    Set<String> outputEnv = new HashSet<>();
    StreamConsumer streamConsumer = line -> {
      outputEnv.add(line.split("=")[0]);
    };
    Command command = Command.create(getScript("env"));
    command.replaceEnvironment(new HashMap<>());

    CommandExecutor.create().execute(command, streamConsumer, 1000L);
    if (SystemUtils.IS_OS_WINDOWS) {
      // On Windows, Java ensures a minimum set of environment variables
      assertThat(outputEnv).containsExactlyInAnyOrder("PATHEXT", "SystemRoot", "COMSPEC", "PROMPT");
    } else {
      // On Linux, only PWD is set as a minimum requirement
      assertThat(outputEnv).containsExactly("PWD");
    }
  }

  private static String getScript(String name) throws IOException {
    String filename;
    if (SystemUtils.IS_OS_WINDOWS) {
      filename = name + ".bat";
    } else {
      filename = name + ".sh";
    }
    return new File("src/test/scripts/" + filename).getCanonicalPath();
  }

  private static File getFile(String dir, String filenameRegexp) {
    FileFilter fileFilter = new WildcardFileFilter(filenameRegexp);
    File[] files = new File(dir).listFiles(fileFilter);
    if (files == null || files.length != 1) {
      throw new IllegalStateException("File not found: " + filenameRegexp + " in " + dir);
    }
    return files[0];
  }
}
