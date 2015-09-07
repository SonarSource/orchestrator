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
package com.sonar.orchestrator.container;

import com.sonar.orchestrator.junit.PropertyFilterRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(PropertyFilterRunner.class)
public class InstallationKeysTest {
  @Test
  public void generate_unique_key() {
    Set<String> keys = new HashSet<>();
    for (int i = 0; i < 1000; i++) {
      String key = InstallationKeys.instance().next();
      assertThat(key).isNotEmpty();
      assertThat(key.length()).isLessThan(5);
      assertThat(keys.contains(key)).isFalse();
      keys.add(key);
    }
  }
}
