/*
 * Echo Program
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package com.sonar.orchestrator.echo;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;
import java.util.Properties;

public class Echo {
  public static void main(String[] args) throws Exception {
    File argumentsFile = new File("arguments.txt");
    FileWriter argsWriter = new FileWriter(argumentsFile);
    System.out.println("Arguments");
    for (String arg : args) {
      System.out.println("\t" + arg);
      argsWriter.write(arg);
      argsWriter.write("\n");
    }
    argsWriter.close();

    Properties props = new Properties();
    System.out.println("\nSystem Properties");
    for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
      System.out.println("\t" + entry.getKey() + "=" + entry.getValue());
      props.put(entry.getKey(), entry.getValue());
    }
    File sysFile = new File("sysprops.txt");
    FileWriter sysWriter = new FileWriter(sysFile);
    props.store(sysWriter, "");
    sysWriter.close();
  }
}
