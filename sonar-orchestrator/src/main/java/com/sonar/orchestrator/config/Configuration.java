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
package com.sonar.orchestrator.config;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.Properties;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.updatecenter.common.UpdateCenter;

public final class Configuration {
  private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

  public static final String SONAR_VERSION_PROPERTY = "sonar.runtimeVersion";
  private static final String ENV_SHARED_DIR = "SONAR_IT_SOURCES";
  private static final String PROP_SHARED_DIR = "orchestrator.it_sources";

  private final Map<String, String> props;
  private final FileSystem fileSystem;
  private final UpdateCenter updateCenter;

  private Configuration(Map<String, String> map, UpdateCenter updateCenter) {
    this.updateCenter = updateCenter;
    this.props = ImmutableMap.copyOf(map);
    this.fileSystem = new FileSystem(this);
  }

  public FileSystem fileSystem() {
    return fileSystem;
  }

  public UpdateCenter updateCenter() {
    return updateCenter;
  }

  public Version getSonarVersion() {
    return Version.create(props.get(SONAR_VERSION_PROPERTY));
  }

  public Version getPluginVersion(String pluginKey) {
    return Version.create(props.get(pluginKey + "Version"));
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
    Preconditions.checkNotNull(rootPath, String.format("Property '%s' or environment variable '%s' is missing", PROP_SHARED_DIR, ENV_SHARED_DIR));

    File rootDir = new File(rootPath);
    Preconditions.checkState(rootDir.isDirectory() && rootDir.exists(),
      String.format("Please check the definition of it_sources (%s or %s) because the directory does not exist: %s", PROP_SHARED_DIR, ENV_SHARED_DIR, rootDir));

    return FileLocation.of(new File(rootDir, relativePath));
  }

  public String getString(String key) {
    if ("sonar.jdbc.dialect".equals(key) && "embedded".equalsIgnoreCase(props.get(key))) {
      return "h2";
    }

    return props.get(key);
  }

  public String getString(String key, @Nullable String defaultValue) {
    return StringUtils.defaultString(props.get(key), defaultValue);
  }

  public String getStringByKeys(String... keys) {
    for (String key : keys) {
      String result = getString(key);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  public int getInt(String key, int defaultValue) {
    String stringValue = props.get(key);
    if (StringUtils.isNotBlank(stringValue)) {
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
    private Map<String, String> props = Maps.newHashMap();
    private UpdateCenter updateCenter;

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

    public Builder setUpdateCenter(UpdateCenter updateCenter) {
      this.updateCenter = updateCenter;
      return this;
    }

    private Builder loadPropertiesFile() {
      String fileUrl = props.get("ORCHESTRATOR_CONFIG_URL");
      fileUrl = StringUtils.defaultIfBlank(props.get("orchestrator.configUrl"), fileUrl);
      if (!StringUtils.isNotBlank(fileUrl)) {
        // Use default values
        setPropertyIfAbsent("sonar.jdbc.dialect", "embedded");
        setPropertyIfAbsent("orchestrator.updateCenterUrl", "http://update.sonarsource.org/update-center-dev.properties");
        setPropertyIfAbsent("maven.localRepository", System.getProperty("user.home") + "/.m2/repository");

        return this;
      }

      try {
        fileUrl = interpolate(fileUrl, props);
        String fileContent = IOUtils.toString(new URI(fileUrl), "UTF-8");
        Properties fileProps = new Properties();
        fileProps.load(IOUtils.toInputStream(fileContent));
        for (Map.Entry<Object, Object> entry : fileProps.entrySet()) {
          if (!props.containsKey(entry.getKey().toString())) {
            props.put(entry.getKey().toString(), entry.getValue().toString());
          }
        }
      } catch (Exception e) {
        throw new IllegalStateException("Fail to load configuration file: " + fileUrl, e);
      }
      return this;
    }

    private void setPropertyIfAbsent(String key, String value) {
      if (StringUtils.isBlank(props.get(key))) {
        LOG.warn("Using default value for orchestrator.properties: " + key + "=" + value);
        props.put(key, value);
      }
    }

    private Builder interpolateProperties() {
      Map<String, String> copy = Maps.newHashMap();
      for (Map.Entry<String, String> entry : props.entrySet()) {
        copy.put(entry.getKey(), interpolate(entry.getValue(), props));
      }
      props = copy;
      return this;
    }

    private static String interpolate(String prop, Map<String, String> with) {
      return StrSubstitutor.replace(prop, with, "${", "}");
    }

    public Configuration build() {
      loadPropertiesFile();
      interpolateProperties();
      return new Configuration(props, updateCenter);
    }
  }
}
