/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
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
package com.sonar.orchestrator.locator;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class MavenRepositoryVersionTest {

  @Test
  public void deserialization_whenXmlCorrect() throws Exception {
    String xmlInput = createValidXml();
    MavenRepositoryVersion deserialized = new XmlMapper().readValue(xmlInput, MavenRepositoryVersion.class);

    assertEquals("1.1.0", deserialized.getModelVersion());
    assertEquals("org.sonarsource.sonarqube", deserialized.getGroupId());
    assertEquals("sonar-plugin-api", deserialized.getArtifactId());
    MavenRepositoryVersion.Versioning versioning = deserialized.getVersioning();
    assertEquals("3.0.1.54424", versioning.getLatest());
    assertEquals("3.0.1.54424", versioning.getRelease());
    List<String> versions = versioning.getVersions();
    assertTrue(versions.contains("1.0"));
    assertTrue(versions.contains("1.0.1"));
    assertTrue(versions.contains("1.1"));
    assertTrue(versions.contains("2.0"));
    assertTrue(versions.contains("2.0.1"));
    assertTrue(versions.contains("3.0"));
    assertTrue(versions.contains("3.0.1"));
    assertTrue(versions.contains("3.0.1.54424"));
  }

  @Test
  public void deserialization_whenXmlCorrectAndNoVersions() {
    String xmlInput = createInvalidXml();
    assertThrows(JsonParseException.class, () -> new XmlMapper().readValue(xmlInput, MavenRepositoryVersion.class));
  }
  
  private String createValidXml() {
    return "<metadata modelVersion=\"1.1.0\">" +
        "<groupId>org.sonarsource.sonarqube</groupId>" +
        "<artifactId>sonar-plugin-api</artifactId>" +
        "<versioning>" +
        "<latest>3.0.1.54424</latest>" +
        "<release>3.0.1.54424</release>" +
        "<versions>" +
          "<version>1.0</version>" +
          "<version>1.0.1</version>" +
          "<version>1.1</version>" +
          "<version>2.0</version>" +
          "<version>2.0.1</version>" +
          "<version>3.0</version>" +
          "<version>3.0.1</version>" +
          "<version>3.0.1.54424</version>" +
        "</versions>" +
        "<lastUpdated>20221018072817</lastUpdated>" +
        "</versioning>" +
      "</metadata>";
  }

  private String createInvalidXml() {
    return "Some random text";
  }

}