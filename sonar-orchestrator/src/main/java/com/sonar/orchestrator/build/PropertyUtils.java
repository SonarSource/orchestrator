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
package com.sonar.orchestrator.build;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import java.util.Map;

final class PropertyUtils {

  private PropertyUtils() {
  }

  static Map<String, String> toMap(String[] keyValues) {
    Preconditions.checkArgument(keyValues.length % 2 == 0, "Must be an even number of key/values");
    Map<String, String> map = Maps.newHashMap();
    int index = 0;
    while (index < keyValues.length) {
      String key = keyValues[index++];
      String value = keyValues[index++];
      map.put(key, value);
    }
    return map;
  }
}
