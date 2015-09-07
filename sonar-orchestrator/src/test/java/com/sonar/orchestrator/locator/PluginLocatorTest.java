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
package com.sonar.orchestrator.locator;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.junit.PropertyFilterRunner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginReferential;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Sonar;
import org.sonar.updatecenter.common.UpdateCenter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PropertyFilterRunner.class)
public class PluginLocatorTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private PluginLocator pluginLocator;
  private MavenLocator mavenLocator;
  private PluginReferential pluginReferential;
  private File fakeArtifact;

  @Before
  public void prepare() throws IOException {
    mavenLocator = mock(MavenLocator.class);
    pluginReferential = mock(PluginReferential.class);
    Configuration config = Configuration.builder()
      .setUpdateCenter(UpdateCenter.create(pluginReferential, mock(Sonar.class)))
      .build();
    pluginLocator = new PluginLocator(config, mavenLocator, new URLLocator());
    fakeArtifact = temp.newFile();
    FileUtils.write(fakeArtifact, "fakeContent");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testLocateUnsuported() {
    PluginLocation location = PluginLocation.create("clirr", "1.1", "foo.bar", "clirr-plugin");
    pluginLocator.locate(location);
  }

  @Test
  public void testCopyToFileFromMavenRepository() throws IOException {
    PluginLocation location = PluginLocation.create("clirr", "1.1", "foo.bar", "clirr-plugin");
    File dest = temp.newFile();
    when(mavenLocator.copyToFile(location, dest)).thenReturn(dest);
    assertThat(pluginLocator.copyToFile(location, dest)).isEqualTo(dest);
    verify(mavenLocator).copyToFile(location, dest);
  }

  @Test
  public void testCopyToFileFallbackToUpdateCenter() throws IOException {
    PluginLocation location = PluginLocation.create("clirr", "1.1", "foo.bar", "clirr-plugin");
    File dest = temp.newFile();
    when(mavenLocator.copyToFile(eq(location), any(File.class))).thenReturn(null);
    Plugin plugin = new Plugin("clirr");
    plugin.addRelease(new Release(plugin, "1.1").setDownloadUrl(fakeArtifact.toURI().toURL().toString()));
    when(pluginReferential.findPlugin("clirr")).thenReturn(plugin);
    assertThat(FileUtils.readFileToString(pluginLocator.copyToFile(location, dest))).isEqualTo("fakeContent");
  }

  @Test
  public void testFailIfNotFoundInMavenNorInUpdateCenter() throws IOException {
    PluginLocation location = PluginLocation.create("clirr", "1.1", "foo.bar", "clirr-plugin");
    File dest = temp.newFile();
    when(mavenLocator.copyToFile(eq(location), any(File.class))).thenReturn(null);
    when(pluginReferential.findPlugin("clirr")).thenThrow(new NoSuchElementException());

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Plugin with key clirr not found in update center");
    pluginLocator.copyToFile(location, dest);
  }

  @Test
  public void testFailIfNotFoundInMavenAndNoReleaseInUpdateCenter() throws IOException {
    PluginLocation location = PluginLocation.create("clirr", "1.1", "foo.bar", "clirr-plugin");
    File dest = temp.newFile();
    when(mavenLocator.copyToFile(eq(location), any(File.class))).thenReturn(null);
    Plugin plugin = new Plugin("clirr");
    when(pluginReferential.findPlugin("clirr")).thenReturn(plugin);

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Unable to find version 1.1 in update center for plugin with key clirr");
    pluginLocator.copyToFile(location, dest);
  }

  @Test
  public void testFailIfNotFoundInMavenAndNoURLInUpdateCenter() throws IOException {
    PluginLocation location = PluginLocation.create("clirr", "1.1", "foo.bar", "clirr-plugin");
    File dest = temp.newFile();
    when(mavenLocator.copyToFile(eq(location), any(File.class))).thenReturn(null);
    Plugin plugin = new Plugin("clirr");
    plugin.addRelease(new Release(plugin, "1.1"));
    when(pluginReferential.findPlugin("clirr")).thenReturn(plugin);

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Invalid URL in update center for plugin clirr: ");
    pluginLocator.copyToFile(location, dest);
  }

  @Test
  public void testCopyToDirFromMavenRepository() throws IOException {
    PluginLocation location = PluginLocation.create("clirr", "1.1", "foo.bar", "clirr-plugin");
    File dest = temp.newFolder();
    when(mavenLocator.copyToDirectory(location, dest)).thenReturn(dest);
    assertThat(pluginLocator.copyToDirectory(location, dest)).isEqualTo(dest);
    verify(mavenLocator).copyToDirectory(location, dest);
  }

  @Test
  public void testCopyToDirFallbackToUpdateCenter() throws IOException {
    PluginLocation location = PluginLocation.create("clirr", "1.1", "foo.bar", "clirr-plugin");
    File dest = temp.newFolder();
    when(mavenLocator.copyToDirectory(eq(location), any(File.class))).thenReturn(null);
    Plugin plugin = new Plugin("clirr");
    plugin.addRelease(new Release(plugin, "1.1").setDownloadUrl(fakeArtifact.toURI().toURL().toString()));
    when(pluginReferential.findPlugin("clirr")).thenReturn(plugin);
    assertThat(FileUtils.readFileToString(pluginLocator.copyToDirectory(location, dest))).isEqualTo("fakeContent");
  }

  @Test
  public void testOpenStreamFromMavenRepository() throws IOException {
    PluginLocation location = PluginLocation.create("clirr", "1.1", "foo.bar", "clirr-plugin");
    when(mavenLocator.openInputStream(location)).thenReturn(new FileInputStream(fakeArtifact));
    assertThat(IOUtils.readLines(pluginLocator.openInputStream(location), "UTF-8")).contains("fakeContent");
  }

  @Test
  public void testOpenStreamFallbackToUpdateCenter() throws IOException {
    PluginLocation location = PluginLocation.create("clirr", "1.1", "foo.bar", "clirr-plugin");
    when(mavenLocator.openInputStream(eq(location))).thenReturn(null);
    Plugin plugin = new Plugin("clirr");
    plugin.addRelease(new Release(plugin, "1.1").setDownloadUrl(fakeArtifact.toURI().toURL().toString()));
    when(pluginReferential.findPlugin("clirr")).thenReturn(plugin);
    assertThat(IOUtils.readLines(pluginLocator.openInputStream(location), "UTF-8")).contains("fakeContent");
  }
}
