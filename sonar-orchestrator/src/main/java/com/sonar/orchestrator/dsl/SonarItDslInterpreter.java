/*
 * Orchestrator
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.sonar.sslr.api.AstNode;
import java.util.List;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SonarItDslInterpreter {

  private final LexerlessGrammar grammar = SonarItDslGrammar.createGrammar();

  public List<Command> interpret(String dsl) {
    ParserAdapter<LexerlessGrammar> parser = new ParserAdapter<>(UTF_8, grammar);
    AstNode ast = parser.parse(dsl);
    DslTransformation transformation = new DslTransformation();
    return transformation.transform(ast);
  }

}
