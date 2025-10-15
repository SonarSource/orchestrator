/*
 * Orchestrator Http Client
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

import okhttp3.Authenticator;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HttpClientTest {

  @Test
  void should_have_default_settings() {
    HttpClient underTest = newClient();

    OkHttpClient okClient = underTest.getUnderlying();
    assertThat(okClient.connectTimeoutMillis()).isEqualTo(30_000);
    assertThat(okClient.readTimeoutMillis()).isEqualTo(300_000);
    assertThat(okClient.writeTimeoutMillis()).isEqualTo(300_000);
    assertThat(okClient.followRedirects()).isTrue();
    assertThat(okClient.followSslRedirects()).isTrue();
    assertThat(okClient.retryOnConnectionFailure()).isTrue();
  }

  @Test
  void should_enable_proxy_authentication_if_system_properties_for_proxy_host_and_credentials_are_defined() {
    HttpClient.SystemProperties sysProps = mock(HttpClient.SystemProperties.class);
    when(sysProps.getProperty("http.proxyHost")).thenReturn("proxy.mydomain");
    when(sysProps.getProperty("http.proxyUser")).thenReturn("foo");
    when(sysProps.getProperty("http.proxyPassword")).thenReturn("bar");

    HttpClient underTest = new HttpClient.Builder().setSystemProperties(sysProps).build();
    HttpClient.ProxyAuthenticator proxyAuthenticator = (HttpClient.ProxyAuthenticator) underTest.getUnderlying().proxyAuthenticator();
    assertThat(proxyAuthenticator.getLogin()).isEqualTo("foo");
    assertThat(proxyAuthenticator.getPassword()).isEqualTo("bar");
  }

  @Test
  void should_disable_proxy_authentication_if_system_properties_for_proxy_credentials_are_not_defined() {
    HttpClient.SystemProperties sysProps = mock(HttpClient.SystemProperties.class);
    when(sysProps.getProperty("http.proxyHost")).thenReturn("proxy.mydomain");

    HttpClient underTest = new HttpClient.Builder().setSystemProperties(sysProps).build();
    Authenticator proxyAuthenticator = underTest.getUnderlying().proxyAuthenticator();
    assertThat(proxyAuthenticator).isNotInstanceOf(HttpClient.ProxyAuthenticator.class);
  }

  @Test
  void should_disable_proxy_authentication_if_proxy_credentials_are_defined_but_not_host() {
    HttpClient.SystemProperties sysProps = mock(HttpClient.SystemProperties.class);
    when(sysProps.getProperty("http.proxyUser")).thenReturn("foo");
    when(sysProps.getProperty("http.proxyPassword")).thenReturn("bar");

    HttpClient underTest = new HttpClient.Builder().setSystemProperties(sysProps).build();
    Authenticator proxyAuthenticator = underTest.getUnderlying().proxyAuthenticator();
    assertThat(proxyAuthenticator).isNotInstanceOf(HttpClient.ProxyAuthenticator.class);
  }

  private HttpClient newClient() {
    return new HttpClient.Builder().build();
  }

}
