/*
 * Orchestrator
 * Copyright (C) 2011-2017 SonarSource SA
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.annotation.CheckForNull;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Response;

public class HttpResponse {

  private final int code;
  private final Charset charset;
  private final byte[] body;
  private final Headers headers;

  HttpResponse(int code, Charset charset, byte[] body, Headers headers) {
    this.code = code;
    this.charset = charset;
    this.body = body;
    this.headers = headers;
  }

  public int getCode() {
    return code;
  }

  public Charset getCharset() {
    return charset;
  }

  public byte[] getBody() {
    return body;
  }

  public String getBodyAsString() {
    return new String(body, charset);
  }

  @CheckForNull
  public String getHeader(String key) {
    return headers.get(key);
  }

  /**
   * Returns true if the code is in [200..300), which means the request was successfully received,
   * understood, and accepted.
   */
  public boolean isSuccessful() {
    return code >= 200 && code < 300;
  }

  static HttpResponse from(Response okResponse) throws IOException {
    MediaType contentType = okResponse.body().contentType();
    Charset charset = contentType != null ? contentType.charset(StandardCharsets.UTF_8) : StandardCharsets.UTF_8;
    return new HttpResponse(okResponse.code(), charset, okResponse.body().bytes(), okResponse.headers());
  }
}
