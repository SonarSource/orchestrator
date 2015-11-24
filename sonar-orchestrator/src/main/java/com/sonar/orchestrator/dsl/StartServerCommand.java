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
package com.sonar.orchestrator.dsl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.config.Configuration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.apache.commons.lang.StringUtils;

public class StartServerCommand extends Command {

  private List<Plugin> plugins = Lists.newArrayList();
  private Map<String, String> properties = Maps.newHashMap();

  public void addPlugin(Plugin plugin) {
    plugins.add(plugin);
  }

  public void setProperty(String key, String value) {
    properties.put(key, value);
  }

  @Override
  public void execute(Dsl.Context context) {
    initOrchestrator(context).start();
  }

  @VisibleForTesting
  Orchestrator initOrchestrator(Dsl.Context context) {
    Orchestrator orchestrator = context.getOrchestrator();
    if (orchestrator == null) {
      OrchestratorBuilder builder = Orchestrator.builder(Configuration.create(context.getSettings()));
      initPlugins(builder);
      initProperties(builder);
      orchestrator = builder.build();
      context.setOrchestrator(orchestrator);
    }
    return orchestrator;
  }

  private void initPlugins(OrchestratorBuilder builder) {
    for (Plugin plugin : plugins) {
      try {
        builder.setOrchestratorProperty(plugin.key + "Version", plugin.version);
        builder.addPlugin(plugin.key);
        builder.activateLicense(plugin.key);

      } catch (NoSuchElementException e) {
        throw new IllegalStateException(String.format("Unable to find plugin %s version %s in update center", plugin.key, plugin.version), e);
      }
    }
  }

  private void initProperties(OrchestratorBuilder builder) {
    for (Map.Entry<String, String> property : properties.entrySet()) {
      builder.setServerProperty(property.getKey(), property.getValue());
    }
  }

  public static class Plugin {
    public Plugin(String key, String version) {
      Preconditions.checkArgument(StringUtils.isNotBlank(key), "Plugin key can't be blank");
      Preconditions.checkArgument(StringUtils.isNotBlank(version), "Plugin version can't be blank");
      this.key = StringUtils.lowerCase(key);
      this.version = version;
    }

    public String key;
    public String version;
  }

  public List<Plugin> getPlugins() {
    return plugins;
  }

  public Map<String, String> getProperties() {
    return properties;
  }
}
