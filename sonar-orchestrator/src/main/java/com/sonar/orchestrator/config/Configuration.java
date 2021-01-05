/*
 * Orchestrator
 * Copyright (C) 2011-2021 SonarSource SA
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

import com.sonar.orchestrator.locator.ArtifactoryImpl;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Locators;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
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
import org.apache.commons.lang.text.StrSubstitutor;

import static com.sonar.orchestrator.util.OrchestratorUtils.checkState;
import static com.sonar.orchestrator.util.OrchestratorUtils.defaultIfNull;
import static com.sonar.orchestrator.util.OrchestratorUtils.isEmpty;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.io.FileUtils.getUserDirectory;

public class Configuration {
  private static final String ENV_SHARED_DIR = "SONAR_IT_SOURCES";
  private static final String PROP_SHARED_DIR = "orchestrator.it_sources";

  private final Map<String, String> props;
  private final FileSystem fileSystem;
  private final Locators locators;

  private Configuration(File homeDir, Map<String, String> props) {
    this.props = Collections.unmodifiableMap(new HashMap<>(props));
    this.fileSystem = new FileSystem(homeDir, this);
    this.locators = new Locators(this.fileSystem, ArtifactoryImpl.create(this));
  }

  public FileSystem fileSystem() {
    return fileSystem;
  }

  public Locators locators() {
    return locators;
  }

  /**
   * File located in the shared directory defined by the system property orchestrator.it_sources or environment variable SONAR_IT_SOURCES.
   * Example : getFileLocationOfShared("javascript/performancing/pom.xml")
   */
  public FileLocation getFileLocationOfShared(String relativePath) {
    // try to read it_sources
    // in the System.getProperties
    // in the prop file (from orchestrator.properties file)
    // in the environment variable
    String rootPath;
    rootPath = System.getProperty(PROP_SHARED_DIR);
    if (rootPath == null) {
      rootPath = props.get(PROP_SHARED_DIR);
    }
    if (rootPath == null) {
      rootPath = System.getenv(ENV_SHARED_DIR);
    }
    requireNonNull(rootPath, format("Property '%s' or environment variable '%s' is missing", PROP_SHARED_DIR, ENV_SHARED_DIR));

    File rootDir = new File(rootPath);
    checkState(rootDir.isDirectory() && rootDir.exists(),
      "Please check the definition of it_sources (%s or %s) because the directory does not exist: %s", PROP_SHARED_DIR, ENV_SHARED_DIR, rootDir);

    return FileLocation.of(new File(rootDir, relativePath));
  }

  public String getString(String key) {
    if ("sonar.jdbc.dialect".equals(key) && "embedded".equalsIgnoreCase(props.get(key))) {
      return "h2";
    }

    return props.get(key);
  }

  public String getString(String key, @Nullable String defaultValue) {
    return defaultIfNull(props.get(key), defaultValue);
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

  public static final class Builder {
    private Map<String, String> props = new HashMap<>();

    private Builder() {
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
      for (Map.Entry<String, String> entry : p.entrySet()) {
        props.put(entry.getKey(), entry.getValue());
      }
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

    public Builder setProperty(String key, File file) {
      props.put(key, file.getAbsolutePath());
      return this;
    }

    private File loadProperties() {
      File homeDir = Stream.of(
        props.get("orchestrator.home"),
        props.get("ORCHESTRATOR_HOME"),
        props.get("SONAR_USER_HOME"))
        .filter(s -> !isEmpty(s))
        .findFirst()
        .map(File::new)
        .orElse(new File(getUserDirectory(), ".sonar/orchestrator"));

      String configUrl = Stream.of(
        props.get("orchestrator.configUrl"),
        props.get("ORCHESTRATOR_CONFIG_URL"))
        .filter(s -> !isEmpty(s))
        .findFirst()
        .orElseGet(() -> {
          File file = new File(homeDir, "orchestrator.properties");
          try {
            return file.exists() ? file.getAbsoluteFile().toURI().toURL().toString() : null;
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

    private static Map<String, String> interpolateProperties(Map<String, String> map) {
      Map<String, String> copy = new HashMap<>();
      for (Map.Entry<String, String> entry : map.entrySet()) {
        copy.put(entry.getKey(), interpolate(entry.getValue(), map));
      }
      return copy;
    }

    private static String interpolate(String prop, Map<String, String> with) {
      return StrSubstitutor.replace(prop, with, "${", "}");
    }

    public Configuration build() {
      File homeDir = loadProperties();
      Map<String, String> interpolatedProperties = interpolateProperties(props);
      return new Configuration(homeDir, interpolatedProperties);
    }
  }
}
