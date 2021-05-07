/*
 * Orchestrator
 * Copyright (C) 2011-2021 SonarSource SA
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
package com.sonar.orchestrator.build;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildCacheTest {
  private final BuildCache buildCache = new BuildCache();

  @After
  public void cleanUp() {
    buildCache.clear();
  }

  @Test
  public void cache_hit() throws URISyntaxException {
    buildCache.cache("id1", createBuild());
    Optional<BuildCache.CachedReport> cached = buildCache.getCached("id1");
    assertThat(cached).isNotEmpty();
    assertThat(cached.get().getProjectKey()).isEqualTo("sample");
    assertThat(cached.get().getProjectName()).isEqualTo("Sample");
    assertThat(cached.get().getBranchName()).isNull();
    assertThat(cached.get().getPrKey()).isNull();
    assertThat(cached.get().getReportDirectory()).isNotNull();
  }

  @Test
  public void clear_should_delete_files() throws URISyntaxException {
    buildCache.cache("id1", createBuild());
    Optional<BuildCache.CachedReport> cached = buildCache.getCached("id1");
    assertThat(cached.get().getReportDirectory()).isDirectory();

    buildCache.clear();

    assertThat(cached.get().getReportDirectory()).doesNotExist();
    assertThat(buildCache.getCached("id1")).isEmpty();
  }

  @Test
  public void return_empty_if_not_cached() throws URISyntaxException {
    buildCache.cache("id1", createBuild());
    assertThat(buildCache.getCached("id2")).isEmpty();
  }

  private Build<?> createBuild() throws URISyntaxException {
    Path scannerReportDir = Paths.get(this.getClass().getResource("BuildCacheTest/scanner-report").toURI());

    Build<?> build = mock(Build.class);
    when(build.getScannerReportDirectory()).thenReturn(Optional.of(scannerReportDir));
    return build;
  }
}
