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

import com.sonar.orchestrator.config.Configuration;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OrchestratorTest {

  @Rule
  public Orchestrator underTest = new OrchestratorBuilder(Configuration.create())
    .setSonarVersion("LATEST_RELEASE[7.9]")
    .build();

  @Test
  public void test_reset_data() throws SQLException {
    underTest.getServer().provisionProject("toto", "toto");
    assertThat(countProject()).isEqualTo(1);

    underTest.resetData();

    assertThat(countProject()).isEqualTo(0);
  }

  private int countProject() throws SQLException {
    Connection connection = underTest.getDatabase().openConnection();
    PreparedStatement preparedStatement = connection.prepareStatement("select count(*) from projects");
    ResultSet resultSet = preparedStatement.executeQuery();
    resultSet.next();
    return resultSet.getInt(1);
  }
}
