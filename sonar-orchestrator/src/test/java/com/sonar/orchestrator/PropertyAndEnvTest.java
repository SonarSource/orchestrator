/*
 * Orchestrator
 * Copyright (C) 2011-2024 SonarSource SA
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
package com.sonar.orchestrator;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;

import static com.sonar.orchestrator.TestModules.setEnv;

/**
 * Created by eric on 06/02/15.
 */
public class PropertyAndEnvTest {

  private Properties storedProperties;
  private Map<String, String> storedEnv;

  @Before
  public void storePropertiesAndEnv() {
    storedProperties = (Properties) System.getProperties().clone();
    storedEnv = new HashMap<>();

    for (String key : System.getenv().keySet()) {
      storedEnv.put(key, System.getenv(key));
    }
  }

  @After
  public void restorePropertiesAndEnv() {
    System.setProperties((Properties) storedProperties.clone());
    setEnv(storedEnv);
  }
}
