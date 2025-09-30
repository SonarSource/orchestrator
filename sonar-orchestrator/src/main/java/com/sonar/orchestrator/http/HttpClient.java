/*
 * Orchestrator
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
package com.sonar.orchestrator.http;

import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class HttpClient {

  private final OkHttpClient okClient;

  private HttpClient(OkHttpClient okClient) {
    this.okClient = okClient;
  }

  public HttpCall newCall(HttpUrl url) {
    return new HttpCall(okClient, url);
  }

  OkHttpClient getUnderlying() {
    return okClient;
  }

  public static class Builder {
    private SystemProperties system = SystemProperties.INSTANCE;

    Builder setSystemProperties(SystemProperties sp) {
      this.system = sp;
      return this;
    }

    public HttpClient build() {
      OkHttpClient.Builder okClient = new OkHttpClient.Builder()
        // make the default values of OkHttp explicit
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)

        // super high timeouts because Orchestrator targets build environments
        // (that are known to be slow or often under pressure...)
        .connectTimeout(30L, TimeUnit.SECONDS)
        .readTimeout(5L, TimeUnit.MINUTES)
        .writeTimeout(5L, TimeUnit.MINUTES);

      // OkHttp detects 'http.proxyHost' java property, but credentials should be filled
      String proxyLogin = system.getProperty("http.proxyUser");
      if (!isEmpty(system.getProperty("http.proxyHost")) && !isEmpty(proxyLogin)) {
        okClient.proxyAuthenticator(new ProxyAuthenticator(proxyLogin, system.getProperty("http.proxyPassword")));
      }

      return new HttpClient(okClient.build());
    }
  }

  static class ProxyAuthenticator implements Authenticator {
    private final String login;
    private final String password;

    private ProxyAuthenticator(String login, String password) {
      this.login = login;
      this.password = password;
    }

    @Override
    public Request authenticate(Route route, Response response) {
      if (HttpURLConnection.HTTP_PROXY_AUTH == response.code()) {
        String credential = Credentials.basic(login, password);
        return response.request().newBuilder().header("Proxy-Authorization", credential).build();
      }
      return null;
    }

    public String getLogin() {
      return login;
    }

    public String getPassword() {
      return password;
    }
  }

  static class SystemProperties {
    private static final SystemProperties INSTANCE = new SystemProperties();

    public String getProperty(String key) {
      return System.getProperty(key);
    }
  }
}
