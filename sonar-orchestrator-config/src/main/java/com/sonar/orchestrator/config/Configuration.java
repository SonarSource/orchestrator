/*
 * Orchestrator Configuration
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
package com.sonar.orchestrator.config;

import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.getUserDirectory;
import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class Configuration {

  private final Map<String, String> props;
  private final FileSystem fileSystem;

  private Configuration(Path homeDir, Map<String, String> props) {
    this.props = Collections.unmodifiableMap(new HashMap<>(props));
    this.fileSystem = new FileSystem(homeDir, this);
  }

  public static Configuration createEnv() {
    return builder().addEnvVariables().addSystemProperties().build();
  }

  public static Configuration create(Properties properties) {
    return builder().addProperties(properties).build();
  }

  public static Configuration create(Map<String, String> properties) {
    return builder().addProperties(properties).build();
  }

  public static Configuration create() {
    return builder().build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public FileSystem fileSystem() {
    return fileSystem;
  }

  public String getString(String key) {
    if ("sonar.jdbc.dialect".equals(key) && "embedded".equalsIgnoreCase(props.get(key))) {
      return "h2";
    }

    return props.get(key);
  }

  public String getString(String key, @Nullable String defaultValue) {
    return getIfNull(props.get(key), defaultValue);
  }

  @CheckForNull
  public String getStringByKeys(String... keys) {
    return getStringByKeys(Objects::nonNull, keys);
  }

  @CheckForNull
  public String getStringByKeys(Predicate<String> validator, String... keys) {
    for (String key : keys) {
      String result = getString(key);
      if (validator.test(result)) {
        return result;
      }
    }
    return null;
  }

  public int getInt(String key, int defaultValue) {
    String stringValue = props.get(key);
    if (!isEmpty(stringValue)) {
      return Integer.parseInt(stringValue);
    }
    return defaultValue;
  }

  public Map<String, String> asMap() {
    return props;
  }

  public static final class Builder {
    private final Map<String, String> props = new HashMap<>();

    private Builder() {
    }

    private static Map<String, String> interpolateProperties(Map<String, String> map) {
      Map<String, String> copy = new HashMap<>();
      for (Map.Entry<String, String> entry : map.entrySet()) {
        copy.put(entry.getKey(), interpolate(entry.getValue(), map));
      }
      return copy;
    }

    private static String interpolate(String prop, Map<String, String> with) {
      return StringSubstitutor.replace(prop, with, "${", "}");
    }

    public Builder addConfiguration(Configuration c) {
      return addMap(c.asMap());
    }

    public Builder addSystemProperties() {
      return addProperties(System.getProperties());
    }

    public Builder addEnvVariables() {
      props.putAll(System.getenv());
      return this;
    }

    public Builder addProperties(Properties p) {
      for (Map.Entry<Object, Object> entry : p.entrySet()) {
        props.put(entry.getKey().toString(), entry.getValue().toString());
      }
      return this;
    }

    public Builder addProperties(Map<String, String> p) {
      props.putAll(p);
      return this;
    }

    public Builder addMap(Map<?, ?> m) {
      for (Map.Entry<?, ?> entry : m.entrySet()) {
        props.put(entry.getKey().toString(), entry.getValue().toString());
      }
      return this;
    }

    public Builder setProperty(String key, @Nullable String value) {
      props.put(key, value);
      return this;
    }

    public Builder setProperty(String key, Path file) {
      props.put(key, file.toAbsolutePath().toString());
      return this;
    }

    private Path loadProperties() {
      Path homeDir = Stream.of(
        props.get("orchestrator.home"),
        props.get("ORCHESTRATOR_HOME"),
        props.get("SONAR_USER_HOME"))
        .filter(s -> !isEmpty(s))
        .findFirst()
        .map(Path::of)
        .orElse(getUserDirectory().toPath().resolve(".sonar/orchestrator"));

      String configUrl = Stream.of(
        props.get("orchestrator.configUrl"),
        props.get("ORCHESTRATOR_CONFIG_URL"))
        .filter(s -> !isEmpty(s))
        .findFirst()
        .orElseGet(() -> {
          Path file = homeDir.resolve("orchestrator.properties");
          try {
            return Files.exists(file) ? file.toAbsolutePath().toUri().toURL().toString() : null;
          } catch (MalformedURLException e) {
            throw new IllegalStateException("Unable to read configuration file", e);
          }
        });

      if (!isEmpty(configUrl)) {
        try {
          configUrl = interpolate(configUrl, props);
          String fileContent = IOUtils.toString(new URI(configUrl), UTF_8);
          Properties fileProps = new Properties();
          fileProps.load(IOUtils.toInputStream(fileContent, UTF_8));
          for (Map.Entry<Object, Object> entry : fileProps.entrySet()) {
            if (!props.containsKey(entry.getKey().toString())) {
              props.put(entry.getKey().toString(), entry.getValue().toString());
            }
          }
        } catch (Exception e) {
          throw new IllegalStateException("Fail to load configuration file: " + configUrl, e);
        }
      }
      return homeDir;
    }

    public Configuration build() {
      Path homeDir = loadProperties();
      Map<String, String> interpolatedProperties = interpolateProperties(props);
      return new Configuration(homeDir, interpolatedProperties);
    }
  }
}
