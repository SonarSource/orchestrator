/*
 * Orchestrator
 * Copyright (C) 2011-2024 SonarSource SA
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
package com.sonar.orchestrator.http;

import okhttp3.Headers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class HttpResponseTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void isSuccessful_returns_true_if_code_is_2xx() {
    verifyCodeSuccessful(200, true);
    verifyCodeSuccessful(201, true);
    verifyCodeSuccessful(220, true);
    verifyCodeSuccessful(300, false);
    verifyCodeSuccessful(404, false);
  }

  private void verifyCodeSuccessful(int code, boolean expectedSuccessfulFlag) {
    HttpResponse response = new HttpResponse(code, UTF_8, new byte[0], new Headers.Builder().build());
    assertThat(response.getCode()).isEqualTo(code);
    assertThat(response.isSuccessful()).isEqualTo(expectedSuccessfulFlag);
  }

}
