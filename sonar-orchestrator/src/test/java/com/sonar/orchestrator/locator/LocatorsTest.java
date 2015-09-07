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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by erichirlemann on 06.02.15.
 */
@RunWith(PropertyFilterRunner.class)
public class LocatorsTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldFailLocateUrlLocation() throws MalformedURLException {
    thrown.expect(UnsupportedOperationException.class);

    Configuration config = Configuration.createEnv();
    Locators locators = new Locators(config);

    URLLocation urlLocation = URLLocation.create(new URL("http://this.is.a.valid.url.com/"));
    locators.locate(urlLocation);
  }

  @Test
  public void shouldFailLocateResourceLocation() {
    thrown.expect(UnsupportedOperationException.class);

    Configuration config = Configuration.createEnv();
    Locators locators = new Locators(config);

    ResourceLocation location = ResourceLocation.create("/");
    locators.locate(location);
  }

  @Test
  public void shouldFailLocatePluginLocation() {
    thrown.expect(UnsupportedOperationException.class);

    Configuration config = Configuration.createEnv();
    Locators locators = new Locators(config);

    PluginLocation location = PluginLocation.create("clirr", "1.1", "groupId", "artifactId");
    locators.locate(location);
  }

  private class UnsupportedLocation implements  Location {

  }

  @Test
  public void shouldFailLocateUnsupportedLocation() {
    thrown.expect(IllegalArgumentException.class);

    Configuration config = Configuration.createEnv();
    Locators locators = new Locators(config);

    UnsupportedLocation location = new UnsupportedLocation();
    locators.locate(location);
  }
}
