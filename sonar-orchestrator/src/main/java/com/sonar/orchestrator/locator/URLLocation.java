/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package com.sonar.orchestrator.locator;

import java.net.URISyntaxException;
import java.net.URL;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

import static java.util.Objects.requireNonNull;

public class URLLocation implements Location {

  private final URL url;
  private final String filename;

  private URLLocation(URL url, @Nullable String filename) {
    requireNonNull(url);
    this.url = url;
    this.filename = filename;
  }

  public static URLLocation create(URL url) {
    return new URLLocation(url, null);
  }

  public static URLLocation create(URL url, @Nullable String filename) {
    return new URLLocation(url, filename);
  }

  public URL getURL() {
    return url;
  }

  public String getFileName() {
    return filename != null ? filename : StringUtils.substringAfterLast(getURL().toString(), "/");
  }

  @Override
  public String toString() {
    return getURL().toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    URLLocation that = (URLLocation) o;
    try {
      return url.toURI().equals(that.url.toURI());
    } catch (URISyntaxException e) {
      return url.toString().equals(that.url.toString());
    }
  }

  @Override
  public int hashCode() {
    try {
      return url.toURI().hashCode();
    } catch (URISyntaxException e) {
      return url.toString().hashCode();
    }
  }
}
