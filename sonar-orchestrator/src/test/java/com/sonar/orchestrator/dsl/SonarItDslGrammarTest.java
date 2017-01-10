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
package com.sonar.orchestrator.dsl;

import com.google.common.base.Charsets;
import com.sonar.sslr.api.AstNode;
import org.junit.Test;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class SonarItDslGrammarTest {

  private LexerlessGrammar g = SonarItDslGrammar.createGrammar();

  @Test
  public void shouldGenerateAST() {
    ParserAdapter<LexerlessGrammar> parser = new ParserAdapter<>(Charsets.UTF_8, g);
    AstNode ast = parser.parse("start server with plugin Cobol 1.12 stop server");
    org.assertj.core.api.Assertions.assertThat(ast.getTokens()).hasSize(8);
  }

  @Test
  public void dslUnit() {
    assertThat(g.rule(SonarItDslGrammar.DSL_UNIT)).matches(
      "start server "
        + "  with plugin cobol 1.12 "
        + "  and plugin php 2.14 "
        + "  and property key value "
        + "cd /path/to/project "
        + "sonar-runner "
        + "  with property sonar.jacoco.path path "
        + "  and property sonar.sources src/main/java "
        + "verify org.apache.struts:struts "
        + "  measure ncloc is 123 "
        + "  measure coverage is 34 "
        + "verify org.apache.struts:struts:Action.java "
        + "  measure ncloc is 123 "
        + "  measure coverage is 34 "
        + "stop server");
  }

  @Test
  public void startServer() {
    assertThat(g.rule(SonarItDslGrammar.START_SERVER_COMMAND))
      .notMatches("startserver")
      .matches("start server")
      .matches("start server with plugin cobol 1.12")
      .matches(
        "start server " +
          "with plugin cobol 1.12 " +
          "and another.property = value"
      );
  }

  @Test
  public void comment() {
    assertThat(g.rule(SonarItDslGrammar.COMMENT))
      .matches("# comment")
      .notMatches("# comment \n");
  }

  @Test
  public void whitespace() {
    assertThat(g.rule(SonarItDslGrammar.WS))
      .matches("")
      .matches("\n")
      .matches(" # comment\n# comment on another line");
  }

  @Test
  public void cd() {
    assertThat(g.rule(SonarItDslGrammar.CD_COMMAND))
      .matches("cd /somewhere/dir2");
  }

  @Test
  public void path() {
    assertThat(g.rule(SonarItDslGrammar.PATH))
      .matches("/dir1/dir2")
      .matches("dir1/dir2")
      .matches("dir1");
  }

  @Test
  public void sonarRunner() {
    assertThat(g.rule(SonarItDslGrammar.SONAR_RUNNER_COMMAND))
      .matches("sonar-runner")
      .matches("sonar-runner with someProperty=value")
      .matches("sonar-runner with property something somethingElse")
      .matches("sonar-runner with someProperty = value and property something somethingElse");
  }

  @Test
  public void verify() {
    assertThat(g.rule(SonarItDslGrammar.VERIFY_COMMAND))
      .matches("verify something measure ncloc is 34");
  }

  @Test
  public void metric() {
    assertThat(g.rule(SonarItDslGrammar.METRIC))
      .matches("ncloc")
      .matches("metric_key_with_underscore")
      .matches("metric-key-with-dash");
  }

  @Test
  public void pluginVersion() {
    assertThat(g.rule(SonarItDslGrammar.PLUGIN_VERSION))
      .matches("1.0")
      .matches("1.0.1");
  }

  @Test
  public void plugin() {
    assertThat(g.rule(SonarItDslGrammar.PLUGIN))
      .matches("with plugin cobol 1.12");
  }

  @Test
  public void expectedMeasure() {
    assertThat(g.rule(SonarItDslGrammar.EXPECTED_MEASURE))
      .matches("34");
  }

  @Test
  public void resourceKey() {
    assertThat(g.rule(SonarItDslGrammar.RESOURCE_KEY))
      .matches("org.sonar:sonar")
      .matches("example:[default].file")
      .matches("example:path/to/file")
      .matches("resource_key_with_underscore")
      .matches("resource-key-with-dash")
      .notMatches("\n")
      .notMatches("\r")
      .notMatches("\t");
  }

  @Test
  public void stopServer() {
    assertThat(g.rule(SonarItDslGrammar.STOP_SERVER_COMMAND))
      .notMatches("stopServer")
      .matches("stop server");
  }

  @Test
  public void pause() {
    assertThat(g.rule(SonarItDslGrammar.PAUSE_COMMAND))
      .matches("pause")
      .notMatches("pauseSonar");
  }

  @Test
  public void propertyKey() {
    assertThat(g.rule(SonarItDslGrammar.PROPERTY_KEY))
      .matches("sonar.propertyKey")
      .matches("sonar.jacoco.path")
      .matches("sonar.key1")
      .matches("property_key_with_underscore")
      .matches("property-key-with-dash");
  }

  @Test
  public void propertyValue() {
    assertThat(g.rule(SonarItDslGrammar.PROPERTY_VALUE))
      .matches("value")
      .matches("/value")
      .matches("value2");
  }

}
