/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package com.sonar.orchestrator.build;

import java.util.HashMap;
import java.util.Map;

import static com.sonar.orchestrator.util.OrchestratorUtils.checkArgument;

final class PropertyUtils {

  private PropertyUtils() {
  }

  static Map<String, String> toMap(String[] keyValues) {
    checkArgument(keyValues.length % 2 == 0, "Must be an even number of key/values");
    Map<String, String> map = new HashMap<>();
    int index = 0;
    while (index < keyValues.length) {
      String key = keyValues[index++];
      String value = keyValues[index++];
      map.put(key, value);
    }
    return map;
  }
}
