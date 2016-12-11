/*
 * Orchestrator
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
package com.sonar.orchestrator.locator;

import com.sonar.orchestrator.util.NetworkUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import okhttp3.Credentials;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.api.Fail;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.servlet.ProxyServlet;

import static org.assertj.core.api.Assertions.assertThat;

public class URLLocatorTest {

  URLLocator urlLocator = new URLLocator();
  URLLocation location;
  URLLocation locationWithFilename;

  private static final String PROXY_PROP_HOST = "http.proxyHost";
  private static final String PROXY_PROP_PORT = "http.proxyPort";
  private static final String PROXY_PROP_USER = "http.proxyUser";
  private static final String PROXY_PROP_PASSWORD = "http.proxyPassword";
  private static final String PROXY_PROP_NON_PROXY_HOST = "http.nonProxyHosts";

  private static final String PROXY_AUTH_TEST_USER = "scott";
  private static final String PROXY_AUTH_TEST_PASSWORD = "tiger";

  private static Map<String, String> proxyPropertiesBackup;
  private static Server server;
  private static int httpProxyPort;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public MockWebServer webServer = new MockWebServer();

  @Before
  public void prepare() {
    URL url = this.getClass().getResource("/com/sonar/orchestrator/locator/URLLocatorTest/foo.txt");
    location = URLLocation.create(url);
    locationWithFilename = URLLocation.create(url, "bar.txt");
  }

  @Test
  public void testToString() {
    assertThat(location.toString()).contains("com/sonar/orchestrator/locator/URLLocatorTest/foo.txt");
  }

  @Test
  public void testEquals() {
    URL anotherURL;
    try {
      anotherURL = new URL("http://docs.oracle.com/javase/7/docs/api/java/net/URL.html");
      URLLocation anotherURLLocation = URLLocation.create(anotherURL);
      URLLocation anotherURLLocationSameURL = URLLocation.create(anotherURL);

      assertThat(location.equals(location)).isTrue();
      assertThat(!location.equals("wrong")).isTrue();
      assertThat(anotherURLLocation.equals(anotherURLLocationSameURL)).isTrue();
      assertThat(!location.equals(anotherURLLocation)).isTrue();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testNotAnUri() throws MalformedURLException {
    URL url = new URL("http://docs.oracle.com/javase/7/docs/api/java/net/URL.html");
    URLLocation urlLocation = URLLocation.create(url);
    URLLocation notAnURIUrlLocation = URLLocation.create(new URL("http:// "));

    assertThat(notAnURIUrlLocation.equals(urlLocation)).isFalse();
    assertThat(notAnURIUrlLocation.equals(url)).isFalse();
    assertThat(notAnURIUrlLocation.hashCode()).isNotEqualTo(0);
  }

  @Test
  public void testHashCode() {
    assertThat(location.hashCode()).isNotEqualTo(0);
  }

  @Test
  public void locate_not_supported() {
    thrown.expect(UnsupportedOperationException.class);

    urlLocator.locate(location);
  }

  @Test
  public void copyToDirectory() throws Exception {
    File toDir = temp.newFolder();

    File copy = urlLocator.copyToDirectory(location, toDir);
    File copy2 = urlLocator.copyToDirectory(locationWithFilename, toDir);

    assertThat(copy).exists().isFile();
    assertThat(copy2).exists().isFile();
    assertThat(copy.getName()).isEqualTo("foo.txt");
    assertThat(copy2.getName()).isEqualTo("bar.txt");
    assertThat(FileUtils.readFileToString(copy)).isEqualTo("foo");
    assertThat(FileUtils.readFileToString(copy2)).isEqualTo("foo");
  }

  @Test
  public void copyToFile() throws Exception {
    File toFile = temp.newFile();

    File copy = urlLocator.copyToFile(location, toFile);

    assertThat(copy).exists().isFile();
    assertThat(copy).isSameAs(toFile);
    assertThat(FileUtils.readFileToString(copy)).isEqualTo("foo");
  }

  @Test
  public void openInputStream() throws Exception {
    InputStream input = urlLocator.openInputStream(location);

    assertThat(IOUtils.toString(input)).isEqualTo("foo");
  }

  @Test
  public void test_getFilenameFromContentDispositionHeader() {
    assertThat(URLLocator.getFilenameFromContentDispositionHeader(null)).isNull();
    assertThat(URLLocator.getFilenameFromContentDispositionHeader("")).isNull();
    assertThat(URLLocator.getFilenameFromContentDispositionHeader("Content-Disposition: attachment; filename=foo.jar")).isEqualTo("foo.jar");
    assertThat(URLLocator.getFilenameFromContentDispositionHeader("Content-Disposition: attachment; filename=foo.jar;")).isEqualTo("foo.jar");
  }

  @Test
  public void test_copyToFile() throws Exception {
    File toFile = temp.newFile();
    webServer.enqueue(new MockResponse().setBody("hello world"));

    urlLocator.copyToFile(URLLocation.create(webServer.url("/").url()), toFile);
    assertThat(toFile).exists().isFile().hasContent("hello world");
  }

  @Test
  public void copyToDir_gets_filename_from_http_header() throws Exception {
    File toDir = temp.newFolder();
    webServer.enqueue(new MockResponse().setBody("hello world").setHeader("Content-Disposition", "attachment; filename=\"foo.txt\""));

    // URL is about bar.txt but HTTP header is about foo.txt -> the latter wins
    urlLocator.copyToDirectory(URLLocation.create(webServer.url("/bar.txt").url()), toDir);
    File toFile = new File(toDir, "foo.txt");
    assertThat(toFile).exists().isFile().hasContent("hello world");
  }

  @Test
  public void copyToDir_gets_filename_from_url_if_http_header_is_missing() throws Exception {
    File toDir = temp.newFolder();
    webServer.enqueue(new MockResponse().setBody("hello world"));

    urlLocator.copyToDirectory(URLLocation.create(webServer.url("/foo.txt").url()), toDir);
    File toFile = new File(toDir, "foo.txt");
    assertThat(toFile).exists().isFile().hasContent("hello world");
  }

  @Test
  public void copyToFile_with_proxy_not_started() throws IOException {
    saveProxyProperties();
    File toFile = temp.newFile();
    webServer.enqueue(new MockResponse().setBody("hello world"));
    System.setProperty(PROXY_PROP_NON_PROXY_HOST, "");
    System.setProperty(PROXY_PROP_HOST, "localhost");
    System.setProperty(PROXY_PROP_PORT, String.valueOf(NetworkUtils.getNextAvailablePort()));
    try {
      urlLocator.copyToFile(URLLocation.create(webServer.url("/").url()), toFile);
      Fail.fail("Proxy is not started, connection can't be establised");
    } catch (IllegalStateException e) {
      assertThat(e.getCause()).isNotNull();
      assertThat(e.getCause().getClass()).isEqualTo(ConnectException.class);
      assertThat(e.getCause().getMessage()).contains("connect");
    } finally {
      System.clearProperty(PROXY_PROP_NON_PROXY_HOST);
      System.clearProperty(PROXY_PROP_HOST);
      System.clearProperty(PROXY_PROP_PORT);
      restoreProxyProperties();
    }
  }

  @Test
  public void copyToFile_adds_authentication_header_if_system_properties_proxy_host_and_user_are_set() throws Exception {
    saveProxyProperties();
    startProxy();
    File toFile = temp.newFile();
    webServer.enqueue(new MockResponse().setBody("hello world"));
    System.setProperty(PROXY_PROP_NON_PROXY_HOST, "");
    System.setProperty(PROXY_PROP_HOST, "localhost");
    System.setProperty(PROXY_PROP_PORT, String.valueOf(httpProxyPort));
    System.setProperty(PROXY_PROP_USER, PROXY_AUTH_TEST_USER);
    System.setProperty(PROXY_PROP_PASSWORD, PROXY_AUTH_TEST_PASSWORD);
    try {
      urlLocator.copyToFile(URLLocation.create(webServer.url("/").url()), toFile);
      assertThat(toFile).exists().isFile().hasContent("hello world");
    } finally {
      System.clearProperty(PROXY_PROP_NON_PROXY_HOST);
      System.clearProperty(PROXY_PROP_HOST);
      System.clearProperty(PROXY_PROP_PORT);
      System.clearProperty(PROXY_PROP_USER);
      System.clearProperty(PROXY_PROP_PASSWORD);
      restoreProxyProperties();
    }
  }

  @Test
  public void copyToFile_adds_authentication_header_if_system_properties_proxy_host_and_user_are_set_bad_auth() throws Exception {
    saveProxyProperties();
    startProxy();
    File toFile = temp.newFile();
    webServer.enqueue(new MockResponse().setBody("hello world"));
    System.setProperty(PROXY_PROP_NON_PROXY_HOST, "");
    System.setProperty(PROXY_PROP_HOST, "localhost");
    System.setProperty(PROXY_PROP_PORT, String.valueOf(httpProxyPort));
    System.setProperty(PROXY_PROP_USER, PROXY_AUTH_TEST_USER);
    System.setProperty(PROXY_PROP_PASSWORD, PROXY_AUTH_TEST_PASSWORD + "bad");
    try {
      urlLocator.copyToFile(URLLocation.create(webServer.url("/").url()), toFile);
      Fail.fail("Proxy auth is bad");
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).contains(String.valueOf(HttpServletResponse.SC_UNAUTHORIZED));
    } finally {
      System.clearProperty(PROXY_PROP_NON_PROXY_HOST);
      System.clearProperty(PROXY_PROP_HOST);
      System.clearProperty(PROXY_PROP_PORT);
      System.clearProperty(PROXY_PROP_USER);
      System.clearProperty(PROXY_PROP_PASSWORD);
      restoreProxyProperties();
    }
  }

  /**
   * Save Proxy Properties (required if unit test executed behind a real proxy)
   */
  private void saveProxyProperties() {
    proxyPropertiesBackup = new HashMap<>();
    for (Entry<Object, Object> e : System.getProperties().entrySet()) {
      String key = e.getKey().toString();
      if (key.contains("proxy")) {
        continue;
      }
      proxyPropertiesBackup.put(key, (String) e.getValue());
    }
  }

  private void restoreProxyProperties() {
    if (proxyPropertiesBackup == null || proxyPropertiesBackup.isEmpty()) {
      return;
    }
    for (Entry<String, String> e : proxyPropertiesBackup.entrySet()) {
      System.setProperty(e.getKey(), e.getValue());
    }
  }

  private static void startProxy() throws Exception {
    httpProxyPort = NetworkUtils.getNextAvailablePort();
    server = new Server(httpProxyPort);

    ServletHandler handlerProxy = new ServletHandler();
    handlerProxy.addServletWithMapping(AuthProxyServlet.class, "/*");
    HandlerCollection handlers = new HandlerCollection();
    handlers.setHandlers(new Handler[] {handlerProxy, new DefaultHandler()});
    server.addHandler(handlers);
    server.start();
  }

  public static class AuthProxyServlet extends ProxyServlet {

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
      HttpServletRequest request = (HttpServletRequest) req;
      HttpServletResponse response = (HttpServletResponse) res;
      String proxyAuth = request.getHeader("proxy-authorization");
      if (StringUtils.isBlank(proxyAuth)) {
        response.setStatus(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED);
      } else if (!Credentials.basic(PROXY_AUTH_TEST_USER, PROXY_AUTH_TEST_PASSWORD).equals(proxyAuth)) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      } else {
        // Proxy properties should be deleted, otherwise the proxified connection is proxified too => infinite loop
        System.clearProperty(PROXY_PROP_NON_PROXY_HOST);
        System.clearProperty(PROXY_PROP_HOST);
        System.clearProperty(PROXY_PROP_PORT);
        System.clearProperty(PROXY_PROP_USER);
        System.clearProperty(PROXY_PROP_PORT);

        super.service(req, res);
      }
    }

  }

  @After
  public void stopProxy() throws Exception {
    if (server != null && server.isStarted()) {
      server.stop();
    }
  }
}
