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
package com.sonar.orchestrator.build;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.locator.Locators;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import static com.sonar.orchestrator.build.Utils.checkArgument;

public abstract class Build<T extends Build<T>> {

  public static final long DEFAULT_TIMEOUT_SECONDS = 7L * 24 * 60 * 60;

  protected long timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
  protected Map<String, String> properties = new HashMap<>();
  private Map<String, String> env = new HashMap<>();
  private List<String> additionalArguments = new ArrayList<>();

  /**
   * see setTimeoutSeconds()
   */
  public long getTimeoutSeconds() {
    return timeoutSeconds;
  }

  /**
   * Timeout in seconds. Default value is infinite (one week to be more precise)
   *
   * @see #DEFAULT_TIMEOUT_SECONDS
   */
  public T setTimeoutSeconds(long l) {
    checkArgument(l > 0, "Timeout must be greater than zero");
    this.timeoutSeconds = l;
    return (T) this;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public String getProperty(String key) {
    return properties.get(key);
  }

  public T clearProperties() {
    this.properties.clear();
    return (T) this;
  }

  public T setProperties(Map<String, String> p) {
    this.properties.putAll(p);
    return (T) this;
  }

  public T setProperties(String... keyValues) {
    return setProperties(PropertyUtils.toMap(keyValues));
  }

  public T setProperty(String key, @Nullable String value) {
    if (value == null) {
      this.properties.remove(key);
    } else {
      this.properties.put(key, value);
    }
    return (T) this;
  }

  /**
   * @deprecated since 2.17 use {@link Server#associateProjectToQualityProfile(String, String, String)}
   */
  @Deprecated(since = "2.17")
  public T setProfile(@Nullable String profileKey) {
    return setProperty("sonar.profile", profileKey);
  }

  public Map<String, String> getEnvironmentVariables() {
    return env;
  }

  public Map<String, String> getEffectiveEnvironmentVariables() {
    Map<String, String> result = new HashMap<>();
    result.putAll(doGetEnvironmentVariablePrefixes());
    for (Map.Entry<String, String> entry : env.entrySet()) {
      String value = result.get(entry.getKey());
      if (value == null) {
        result.put(entry.getKey(), entry.getValue());
      } else {
        result.put(entry.getKey(), value + " " + entry.getValue());
      }
    }
    return result;
  }

  /**
   * Can be considered as default values of env variables, except
   * that values are prefixed to variables set by caller.
   */
  protected Map<String, String> doGetEnvironmentVariablePrefixes() {
    return new HashMap<>();
  }

  /**
   * Environment variables that are propagated during command execution.
   * The initial value is a copy of the environment of the current process.
   *
   * @since 2.5
   */
  public T setEnvironmentVariable(String name, String value) {
    this.env.put(name, value);
    return (T) this;
  }

  /**
   * Command-line arguments, for example "-Dfoo=true" or "-Xbar"
   *
   * @since 2.10
   */
  public List<String> arguments() {
    return additionalArguments;
  }

  /**
   * Add command-line argument, for example "-Dfoo=true" or "-Xbar"
   *
   * @since 2.10
   */
  public T addArgument(String additionalArgument) {
    return addArguments(Arrays.asList(additionalArgument));
  }

  /**
   * Add command-line arguments, for example "-Dfoo=true" or "-Xbar"
   *
   * @since 2.10
   */
  public T addArguments(String... additionalArguments) {
    return addArguments(Arrays.asList(additionalArguments));
  }

  /**
   * Add command-line arguments, for example "-Dfoo=true" or "-Xbar"
   *
   * @since 2.10
   */
  public T addArguments(List<String> additionalArguments) {
    this.additionalArguments.addAll(additionalArguments);
    return (T) this;
  }

  /**
   * Set command-line arguments, for example "-Dfoo=true" or "-Xbar". Old values are dropped.
   *
   * @since 2.10
   */
  public T setArguments(List<String> additionalArguments) {
    this.additionalArguments = additionalArguments;
    return (T) this;
  }

  /**
   * Shortcut to set the parameter "sonar.dynamicAnalysis" to false
   *
   * @since 2.10
   * @deprecated since 3.13
   */
  @Deprecated(since = "3.13")
  public T withoutDynamicAnalysis() {
    return setProperty("sonar.dynamicAnalysis", "false");
  }

  abstract BuildResult execute(Configuration config, Locators locators, Map<String, String> adjustedProperties);

}
