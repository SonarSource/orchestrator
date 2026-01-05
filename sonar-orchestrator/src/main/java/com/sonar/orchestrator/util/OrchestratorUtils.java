/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource Sàrl
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
package com.sonar.orchestrator.util;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

public class OrchestratorUtils {

  private OrchestratorUtils() {
    // prevent instantiation
  }

  @CheckForNull
  public static String defaultIfEmpty(@Nullable String object, @Nullable String defaultValue) {
    return StringUtils.isEmpty(object) ? defaultValue : object;
  }

  @CheckForNull
  public static <T> T defaultIfNull(@Nullable T object, @Nullable T defaultValue) {
    return object != null ? object : defaultValue;
  }
}
