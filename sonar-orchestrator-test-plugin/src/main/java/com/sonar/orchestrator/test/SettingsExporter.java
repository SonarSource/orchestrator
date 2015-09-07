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

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.config.Settings;
import org.sonar.api.task.TaskExtension;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class SettingsExporter implements BatchExtension, TaskExtension {

  private static final Logger LOGGER = LoggerFactory.getLogger(SettingsExporter.class);

  private final Settings settings;

  public SettingsExporter(Settings settings) {
    this.settings = settings;
  }

  public void export() {
    String path = settings.getString("settings.output");
    if (path != null) {
      Properties props = new Properties();
      LOGGER.info("Settings:");
      for (Map.Entry<String, String> entry : settings.getProperties().entrySet()) {
        LOGGER.info("    " + entry.getKey() + "=" + entry.getValue());
        props.setProperty(entry.getKey(), entry.getValue());
      }
      props.putAll(settings.getProperties());
      File file = new File(path);
      FileWriter fileWriter = null;
      try {
        fileWriter = new FileWriter(file);
        props.store(fileWriter, "");
        LOGGER.info("Settings written to " + file);
      } catch (IOException e) {
        throw new IllegalStateException("Fail to write settings to " + file, e);
      } finally {
        IOUtils.closeQuietly(fileWriter);
      }
    }
  }
}
