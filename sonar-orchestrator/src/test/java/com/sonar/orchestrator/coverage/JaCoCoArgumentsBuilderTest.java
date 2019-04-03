/*
 * Orchestrator
 * Copyright (C) 2011-2019 SonarSource SA
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
package com.sonar.orchestrator.coverage;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.test.MockHttpServerInterceptor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import org.apache.commons.io.FilenameUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class JaCoCoArgumentsBuilderTest {

  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void all_constructors_are_private() {
   assertThat(hasOnlyPrivateConstructors(JaCoCoArgumentsBuilder.class)).isTrue();
  }

  /**
   * Asserts that all constructors are private, usually for helper classes with
   * only static methods. If a constructor does not have any parameters, then
   * it's instantiated.
   */
  private static boolean hasOnlyPrivateConstructors(Class clazz) {
    boolean ok = true;
    for (Constructor constructor : clazz.getDeclaredConstructors()) {
      ok &= Modifier.isPrivate(constructor.getModifiers());
      if (constructor.getParameterTypes().length == 0) {
        constructor.setAccessible(true);
        try {
          constructor.newInstance();
        } catch (Exception e) {
          throw new IllegalStateException(String.format("Fail to instantiate %s", clazz), e);
        }
      }
    }
    return ok;
  }

  @Test
  public void shouldGetJaCoCoVersionFromPom() {
    assertThat(JaCoCoArgumentsBuilder.jaCoCoVersion).isNotEmpty();
  }

  @Test
  public void shouldReturnNullByDefault() {
    Configuration config = Configuration.create(new HashMap<>());

    assertThat(JaCoCoArgumentsBuilder.getJaCoCoArgument(config)).isNull();
  }

  @Test(expected = IllegalStateException.class)
  public void shouldFailIfNoJacocoPropertyFile() {
    JaCoCoArgumentsBuilder.readProperties("unexisting.properties");
  }

  @Test
  public void shouldReturnJaCoCoArgument() throws IOException {
    File output = temp.newFile();
    Map<String, String> props = new HashMap<>();
    props.put("orchestrator.computeCoverage", "true");
    props.put("orchestrator.coverageReportPath", output.getAbsolutePath());
    Configuration config = Configuration.create(props);

    String argument = JaCoCoArgumentsBuilder.getJaCoCoArgument(config);

    assertThat(argument).matches("-javaagent:.*=destfile=" +
      Matcher.quoteReplacement(FilenameUtils.separatorsToUnix(output.getAbsolutePath())) +
      ",append=true,excludes=\\*_javassist_\\*,includes=\\*sonar\\*");
  }
}
