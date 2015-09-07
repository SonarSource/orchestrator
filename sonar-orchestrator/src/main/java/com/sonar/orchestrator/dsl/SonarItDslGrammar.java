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
package com.sonar.orchestrator.dsl;

import com.sonar.sslr.api.GenericTokenType;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;
import org.sonar.sslr.parser.LexerlessGrammar;

public enum SonarItDslGrammar implements GrammarRuleKey {

  DSL_UNIT,
  START_SERVER_COMMAND,
  PLUGIN,
  PLUGIN_KEY,
  PLUGIN_VERSION,
  CD_COMMAND,
  PATH,
  VERIFY_COMMAND,
  RESOURCE_KEY,
  MEASURE_ASSERTION,
  METRIC,
  EXPECTED_MEASURE,
  STOP_SERVER_COMMAND,
  SONAR_RUNNER_COMMAND,
  PROPERTY,
  PROPERTY_KEY,
  PROPERTY_VALUE,
  PAUSE_COMMAND,

  COMMENT,
  WS,
  LETTER_OR_DIGIT;

  private static final String SERVER = "server";

  public static LexerlessGrammar createGrammar() {
    return createGrammarBuilder().build();
  }

  public static LexerlessGrammarBuilder createGrammarBuilder() {
    LexerlessGrammarBuilder b = LexerlessGrammarBuilder.create();

    b.rule(DSL_UNIT).is(
      WS,
      b.oneOrMore(b.firstOf(
        START_SERVER_COMMAND,
        CD_COMMAND,
        SONAR_RUNNER_COMMAND,
        VERIFY_COMMAND,
        STOP_SERVER_COMMAND,
        PAUSE_COMMAND)),
      b.endOfInput());

    b.rule(START_SERVER_COMMAND).is(keyword(b, "start"), keyword(b, SERVER), b.zeroOrMore(b.firstOf(PLUGIN, PROPERTY)));

    b.rule(PLUGIN).is(b.optional(b.firstOf(keyword(b, "with"), keyword(b, "and"))), keyword(b, "plugin"), PLUGIN_KEY, PLUGIN_VERSION);
    b.rule(PLUGIN_KEY).is(b.token(GenericTokenType.IDENTIFIER, b.regexp("\\w++")), WS);
    b.rule(PLUGIN_VERSION).is(b.token(GenericTokenType.IDENTIFIER, b.regexp("[\\w\\.\\-]++")), WS);

    b.rule(CD_COMMAND).is(keyword(b, "cd"), PATH);
    b.rule(PATH).is(b.token(GenericTokenType.IDENTIFIER, b.regexp("[/a-zA-Z\\d]++")), WS);

    b.rule(SONAR_RUNNER_COMMAND).is(keyword(b, "sonar-runner"), b.zeroOrMore(PROPERTY));
    b.rule(PROPERTY).is(b.optional(b.firstOf(keyword(b, "with"), keyword(b, "and"))),
      b.firstOf(
        b.sequence(keyword(b, "property"), PROPERTY_KEY, PROPERTY_VALUE),
        b.sequence(PROPERTY_KEY, "=", WS, PROPERTY_VALUE)));
    b.rule(PROPERTY_KEY).is(b.token(GenericTokenType.IDENTIFIER, b.regexp("[a-zA-Z0-9\\-_]++(\\.[a-zA-Z0-9\\-_]++)*+")), WS);
    b.rule(PROPERTY_VALUE).is(b.token(GenericTokenType.IDENTIFIER, b.regexp("[/a-zA-Z0-9]++")), WS);

    b.rule(VERIFY_COMMAND).is(keyword(b, "verify"), RESOURCE_KEY, b.oneOrMore(MEASURE_ASSERTION));
    b.rule(MEASURE_ASSERTION).is(keyword(b, "measure"), METRIC, keyword(b, "is"), EXPECTED_MEASURE);
    b.rule(METRIC).is(b.token(GenericTokenType.IDENTIFIER, b.regexp("[a-zA-Z\\-_]++")), WS);
    b.rule(EXPECTED_MEASURE).is(b.token(GenericTokenType.IDENTIFIER, b.regexp("\\d+(\\.\\d+)?")), WS);
    b.rule(RESOURCE_KEY).is(b.token(GenericTokenType.IDENTIFIER, b.regexp("[^ \\n\\r\\t]++")), WS);

    b.rule(STOP_SERVER_COMMAND).is(keyword(b, "stop"), keyword(b, SERVER));

    b.rule(PAUSE_COMMAND).is(keyword(b, "pause"));

    b.rule(COMMENT).is(b.commentTrivia(b.regexp("#[^\\r\\n]*+"))).skip();
    b.rule(WS).is(b.skippedTrivia(b.regexp("\\s*+")), b.zeroOrMore(COMMENT, b.skippedTrivia(b.regexp("\\s*+")))).skip();
    b.rule(LETTER_OR_DIGIT).is(b.regexp("[a-zA-Z0-9]")).skip();

    b.setRootRule(DSL_UNIT);

    return b;

  }

  private static Object keyword(LexerlessGrammarBuilder b, String s) {
    return b.sequence(s, b.nextNot(LETTER_OR_DIGIT), WS);
  }

}
