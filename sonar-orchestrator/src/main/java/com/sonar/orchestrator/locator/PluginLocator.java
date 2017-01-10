/*
 * Orchestrator
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package com.sonar.orchestrator.locator;

import com.sonar.orchestrator.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.NoSuchElementException;

import static java.lang.String.format;

public class PluginLocator implements Locator<PluginLocation> {
  private static final Logger LOG = LoggerFactory.getLogger(PluginLocator.class);

  private final Configuration config;
  private final MavenLocator mavenLocator;
  private final URLLocator urlLocator;

  public PluginLocator(Configuration config, MavenLocator mavenLocator, URLLocator urlLocator) {
    this.config = config;
    this.mavenLocator = mavenLocator;
    this.urlLocator = urlLocator;
  }

  @Override
  public File locate(PluginLocation location) {
    throw new UnsupportedOperationException();
  }

  @Override
  public File copyToDirectory(PluginLocation location, File toDir) {
    File result = mavenLocator.copyToDirectory(location, toDir);
    if (result == null) {
      logFallbackToUpdateCenter(location);
      return urlLocator.copyToDirectory(findInUpdateCenter(location), toDir);
    }
    return result;
  }

  @Override
  public File copyToFile(PluginLocation location, File toFile) {
    File result = mavenLocator.copyToFile(location, toFile);
    if (result == null) {
      logFallbackToUpdateCenter(location);
      return urlLocator.copyToFile(findInUpdateCenter(location), toFile);
    }
    return result;
  }

  @Override
  public InputStream openInputStream(PluginLocation location) {
    InputStream result = mavenLocator.openInputStream(location);
    if (result == null) {
      logFallbackToUpdateCenter(location);
      return urlLocator.openInputStream(findInUpdateCenter(location));
    }
    return result;
  }

  private static void logFallbackToUpdateCenter(PluginLocation location) {
    LOG.info("Unable to find plugin {} in Maven repository. Fallback to use update center URL", location.key());
  }

  private URLLocation findInUpdateCenter(PluginLocation location) {
    Plugin findPlugin;
    try {
      findPlugin = config.updateCenter().getUpdateCenterPluginReferential().findPlugin(location.key());
    } catch (NoSuchElementException e) {
      throw new IllegalStateException(format("Plugin with key %s not found in update center", location.key()), e);
    }

    Release release;
    try {
      release = findPlugin.getRelease(location.version().toString());
    } catch (NoSuchElementException e) {
      throw new IllegalStateException(format("Unable to find version %s in update center for plugin with key %s", location.version().toString(), location.key()), e);
    }

    try {
      return URLLocation.create(new URL(release.getDownloadUrl()),
        format("sonar-%s-plugin-%s.jar", release.getKey(), release.getVersion()));
    } catch (MalformedURLException e) {
      throw new IllegalStateException(format("Invalid URL in update center for plugin %s: %s", release.getKey(), release.getDownloadUrl()), e);
    }
  }

}
