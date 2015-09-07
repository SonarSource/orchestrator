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
package com.sonar.orchestrator.coverage;

import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.ISessionInfoVisitor;
import org.jacoco.core.data.SessionInfo;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;

public final class ExecDump {

  public static String dumpContent(final String file) throws IOException {
    final StringBuffer sb = new StringBuffer();
    sb.append("CLASS ID         HITS/PROBES   CLASS NAME").append("\n");

    final FileInputStream in = new FileInputStream(file);
    final ExecutionDataReader reader = new ExecutionDataReader(in);
    reader.setSessionInfoVisitor(new ISessionInfoVisitor() {
      @Override
      public void visitSessionInfo(final SessionInfo info) {
        sb.append(String.format("Session \"%s\": %s - %s%n", info.getId(),
            new Date(info.getStartTimeStamp()),
            new Date(info.getDumpTimeStamp()))).append("\n");
      }
    });
    reader.setExecutionDataVisitor(new IExecutionDataVisitor() {
      @Override
      public void visitClassExecution(final ExecutionData data) {
        sb.append((String.format("%016x  %3d of %3d   %s%n",
            Long.valueOf(data.getId()),
            Integer.valueOf(getHitCount(data.getProbes())),
            Integer.valueOf(data.getProbes().length), data.getName()))).append("\n");
      }
    });
    reader.read();
    in.close();
    return sb.toString();
  }

  private static int getHitCount(final boolean[] data) {
    int count = 0;
    for (final boolean hit : data) {
      if (hit) {
        count++;
      }
    }
    return count;
  }

  private ExecDump() {
  }
}
