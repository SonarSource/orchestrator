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

import com.google.common.base.Charsets;
import com.sonar.sslr.api.AstNode;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;

import java.util.List;

public class SonarItDslInterpreter {

  private final LexerlessGrammar grammar = SonarItDslGrammar.createGrammar();

  public List<Command> interpret(String dsl) {
    ParserAdapter<LexerlessGrammar> parser = new ParserAdapter<LexerlessGrammar>(Charsets.UTF_8, grammar);
    AstNode ast = parser.parse(dsl);
    DslTransformation transformation = new DslTransformation();
    return transformation.transform(ast);
  }

}
