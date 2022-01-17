/*
 * Orchestrator
 * Copyright (C) 2011-2022 SonarSource SA
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
package com.sonar.orchestrator.container;

import com.sonar.orchestrator.locator.MavenLocation;
import java.util.Properties;
import org.junit.Test;

import static com.sonar.orchestrator.container.Edition.COMMUNITY;
import static com.sonar.orchestrator.container.Edition.DATACENTER;
import static com.sonar.orchestrator.container.Edition.ENTERPRISE;
import static org.assertj.core.api.Assertions.assertThat;

public class SonarDistributionTest {
  @Test
  public void shouldCreate() {
    SonarDistribution distribution = new SonarDistribution().setVersion("2.7");
    assertThat(distribution.getVersion().get()).isEqualTo("2.7");
    assertThat(distribution.getPluginLocations().size()).isEqualTo(0);
    assertThat(distribution.getProfileBackups().size()).isEqualTo(0);
    assertThat(distribution.isKeepBundledPlugins()).isFalse();
  }

  @Test
  public void shouldAddPlugin() {
    SonarDistribution distribution = new SonarDistribution();
    distribution.addPluginLocation(MavenLocation.create("org.codehaus.sonar-plugins", "sonar-clirr-plugin", "1.1"));
    assertThat(distribution.getPluginLocations().size()).isEqualTo(1);
    MavenLocation mavenPlugin = (MavenLocation) distribution.getPluginLocations().get(0);
    assertThat(mavenPlugin.getGroupId()).isEqualTo("org.codehaus.sonar-plugins");
    assertThat(mavenPlugin.getArtifactId()).isEqualTo("sonar-clirr-plugin");
    assertThat(mavenPlugin.getVersion()).isEqualTo("1.1");
    assertThat(mavenPlugin.getPackaging()).isEqualTo("jar");
  }

  @Test
  public void test_keep_bundled_plugins() {
    SonarDistribution distribution = new SonarDistribution();
    distribution.setKeepBundledPlugins(false);
    assertThat(distribution.isKeepBundledPlugins()).isFalse();
    distribution.setKeepBundledPlugins(true);
    assertThat(distribution.isKeepBundledPlugins()).isTrue();
  }

  @Test
  public void shouldDefineServerProperties() {
    SonarDistribution distribution = new SonarDistribution();
    assertThat(distribution.getServerProperties().size()).isEqualTo(0);

    distribution.setServerProperty("foo", "bar");
    assertThat((String) distribution.getServerProperties().get("foo")).isEqualTo("bar");

    Properties props = new Properties();
    props.setProperty("new", "value");
    distribution.addServerProperties(props);
    assertThat(distribution.getServerProperties().size()).isEqualTo(2);
    assertThat((String) distribution.getServerProperties().get("foo")).isEqualTo("bar");
    assertThat((String) distribution.getServerProperties().get("new")).isEqualTo("value");

    distribution.removeServerProperty("foo");
    assertThat(distribution.getServerProperties().get("foo")).isNull();
  }

  @Test
  public void shouldSetEdition() {
    SonarDistribution distribution = new SonarDistribution();
    assertThat(distribution.getEdition()).isEqualTo(COMMUNITY); // Default Edition

    distribution.setEdition(ENTERPRISE);
    assertThat(distribution.getEdition()).isEqualTo(ENTERPRISE);

    distribution.setEdition(DATACENTER);
    assertThat(distribution.getEdition()).isEqualTo(DATACENTER);

    distribution.setEdition(ENTERPRISE);
    assertThat(distribution.getEdition()).isEqualTo(ENTERPRISE);

    distribution.setEdition(COMMUNITY);
    assertThat(distribution.getEdition()).isEqualTo(COMMUNITY);
  }

  @Test
  public void correctly_sets_force_default_admin_credentials_redirect_property() {
    SonarDistribution distribution = new SonarDistribution();
    assertThat(distribution.isForceDefaultAdminCredentialsRedirect()).isFalse();
    distribution.setForceDefaultAdminCredentialsRedirect(true);
    assertThat(distribution.isForceDefaultAdminCredentialsRedirect()).isTrue();
    distribution.setForceDefaultAdminCredentialsRedirect(false);
    assertThat(distribution.isForceDefaultAdminCredentialsRedirect()).isFalse();
  }
}
