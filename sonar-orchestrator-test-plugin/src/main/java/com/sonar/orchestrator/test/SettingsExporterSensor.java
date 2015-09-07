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

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;

public class SettingsExporterSensor implements Sensor {
  private final SettingsExporter exporter;

  public SettingsExporterSensor(SettingsExporter exporter) {
    this.exporter = exporter;
  }


  @Override
  public void analyse(Project project, SensorContext sensorContext) {
    exporter.export();
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }
}
