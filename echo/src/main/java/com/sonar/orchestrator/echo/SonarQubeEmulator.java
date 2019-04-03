/*
 * Echo Program
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
package com.sonar.orchestrator.echo;

import java.io.File;

public class SonarQubeEmulator {
  public static void main(String[] args) throws InterruptedException {
    System.out.println("starting");
    System.out.println("....");
    System.out.println("started");

    // try 30 seconds max
    for (int i = 0; i < 600; i++) {
      File file = new File("temp/sharedmemory");
      if (file.exists()) {
        System.out.println("stopped");
        System.exit(0);
      }
      Thread.sleep(50L);
    }
    System.out.println("stop not requested");
    System.exit(1);
  }
}
