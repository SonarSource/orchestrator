/*
 * Orchestrator
 * Copyright (C) 2011-2023 SonarSource SA
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
package com.sonar.orchestrator.http;

public class HttpClientFactory {

  private static transient volatile boolean initialized;
  private static HttpClient singleton;

  private HttpClientFactory() {
    // prevent instantiation, only static methods for the time being
  }

  public static HttpClient create() {
    // A 2-field variant of Double Checked Locking.
    if (!initialized) {
      synchronized (HttpClient.class) {
        if (!initialized) {
          singleton = new HttpClient.Builder().build();
          initialized = true;
          return singleton;
        }
      }
    }
    return singleton;
  }
}
