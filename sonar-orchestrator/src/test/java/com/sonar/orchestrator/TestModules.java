/*
 * Orchestrator
 * Copyright (C) 2011-2019 SonarSource SA
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
package com.sonar.orchestrator;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.io.filefilter.WildcardFileFilter;

public class TestModules {

  public static File getFile(String dir, String filenameRegexp) {
    FileFilter fileFilter = new WildcardFileFilter(filenameRegexp);
    File[] files = new File(dir).listFiles(fileFilter);
    if (files == null || files.length != 1) {
      throw new IllegalStateException("File not found: " + filenameRegexp + " in " + dir);
    }
    return files[0];
  }

  /**
   * Used to override System.getEnv
   * http://stackoverflow.com/questions/318239/how-do-i-set-environment-variables-from-java
   */
  public static void setEnv(Map<String, String> newenv) {
    try {
      Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
      Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
      theEnvironmentField.setAccessible(true);
      Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
      env.putAll(newenv);
      Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
      theCaseInsensitiveEnvironmentField.setAccessible(true);
      Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
      cienv.putAll(newenv);
    } catch (NoSuchFieldException e) {
      try {
        Class[] classes = Collections.class.getDeclaredClasses();
        Map<String, String> env = System.getenv();
        for (Class cl : classes) {
          if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
            Field field = cl.getDeclaredField("m");
            field.setAccessible(true);
            Object obj = field.get(env);
            Map<String, String> map = (Map<String, String>) obj;
            map.clear();
            map.putAll(newenv);
          }
        }
      } catch (Exception e2) {
        e2.printStackTrace();
      }
    } catch (Exception e1) {
      e1.printStackTrace();
    }
  }
}
