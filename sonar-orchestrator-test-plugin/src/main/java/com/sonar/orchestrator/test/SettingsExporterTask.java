/*
 * Orchestrator Test Plugin
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
package com.sonar.orchestrator.test;

import org.sonar.api.task.Task;
import org.sonar.api.task.TaskDefinition;

public class SettingsExporterTask implements Task {

  public static final TaskDefinition DEFINITION = TaskDefinition.builder()
    .description("Export settings")
    .key("settings")
    .taskClass(SettingsExporterTask.class)
    .build();

  private final SettingsExporter exporter;

  public SettingsExporterTask(SettingsExporter exporter) {
    this.exporter = exporter;
  }

  @Override
  public void execute() {
    exporter.export();
  }
}
