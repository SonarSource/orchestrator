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
