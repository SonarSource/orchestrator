/*
 * Orchestrator
 * Copyright (C) 2011-2020 SonarSource SA
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

import java.io.File;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class HttpCallTest {

  private static final String PONG = "pong";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public MockWebServer server = new MockWebServer();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60L));

  @Test
  public void setHeader_adds_header_to_http_request() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG));

    HttpResponse response = newCall("")
      .setHeader("foo", "bar")
      .execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getHeader("foo")).isEqualTo("bar");
  }

  @Test
  public void setHeader_throws_NPE_if_key_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Header key cannot be null");

    newCall("").setHeader(null, "foo");
  }

  @Test
  public void setHeader_throws_NPE_if_value_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Header [foo] cannot have null value");

    newCall("").setHeader("foo", null);
  }

  @Test
  public void execute_GET_request_should_return_response() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG));

    HttpResponse response = newCall("").execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = server.takeRequest();
    verifyRecorded(recordedRequest, "GET", "");
  }

  @Test
  public void GET_parameters_should_be_sent_in_url_query() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG));

    HttpResponse response = newCall("api/system/ping")
      .setParam("foo", "foz")
      .setParam("bar", "baz")
      .execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = server.takeRequest();
    verifyRecorded(recordedRequest, "GET", "api/system/ping?foo=foz&bar=baz");
  }

  @Test
  public void GET_parameter_key_with_null_value_is_set_in_url_query() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG));

    HttpResponse response = newCall("api/system/ping")
      .setParam("foo", null)
      .execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = server.takeRequest();
    verifyRecorded(recordedRequest, "GET", "api/system/ping?foo");
  }

  @Test
  public void POST_parameter_with_null_value() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG));

    HttpResponse response = newCall("api/system/ping")
      .setMethod(HttpMethod.POST)
      .setParam("foo", null)
      .execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = server.takeRequest();
    verifyRecorded(recordedRequest, "POST", "api/system/ping");
    assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("");
  }

  @Test
  public void GET_parameters_defined_with_setParams_should_be_sent_in_url_query() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG));

    HttpResponse response = newCall("api/system/ping")
      .setParams("foo", "foz", "bar", "baz")
      .execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = server.takeRequest();
    verifyRecorded(recordedRequest, "GET", "api/system/ping?foo=foz&bar=baz");
  }

  @Test
  public void setParams_throws_IAE_if_odd_number_of_varargs() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Expecting even number of arguments: [one, two, three]");

    newCall("api/system/ping")
      .setParams("foo", "foz", "one", "two", "three");
  }

  @Test
  public void parameters_of_POST_request_should_be_sent_in_body() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG));

    HttpResponse response = newCall("api/system/ping")
      .setMethod(HttpMethod.POST)
      .setParam("foo", "foz")
      .setParam("bar", "baz")
      .execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = server.takeRequest();
    verifyRecorded(recordedRequest, "POST", "api/system/ping");
    assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("foo=foz&bar=baz");
  }

  @Test
  public void parameters_of_MULTIPART_POST_request_should_be_sent_as_multipart_in_body() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG));

    HttpResponse response = newCall("api/system/ping")
      .setMethod(HttpMethod.MULTIPART_POST)
      .setParam("foo", "foz")
      .setParam("bar", "baz")
      .execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = server.takeRequest();
    verifyRecorded(recordedRequest, "POST", "api/system/ping");
    assertThat(recordedRequest.getBody().readUtf8())
      .containsSubsequence("Content-Disposition: form-data; name=\"foo\"", "foz", "Content-Disposition: form-data; name=\"bar\"", "baz");
  }

  @Test
  public void HttpResponse_contains_headers() {
    server.enqueue(new MockResponse().setBody(PONG)
      .setHeader("foo", "foo_val")
      .setHeader("bar", "bar_val"));

    HttpResponse response = newCall("api/system/ping").execute();

    assertThat(response.getHeader("foo")).isEqualTo("foo_val");
    assertThat(response.getHeader("bar")).isEqualTo("bar_val");
    assertThat(response.getHeader("missing")).isNull();
  }

  @Test
  public void user_agent_should_be_set_with_default_value_if_not_manually_defined() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG));

    newCall("").execute();

    RecordedRequest recordedRequest = server.takeRequest();
    verifyHeader(recordedRequest, "User-Agent", "Orchestrator");
  }

  @Test
  public void user_agent_should_be_overridden_if_defined() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG));

    newCall("")
      .setHeader("User-Agent", "Firefox")
      .execute();

    RecordedRequest recordedRequest = server.takeRequest();
    verifyHeader(recordedRequest, "User-Agent", "Firefox");
  }

  @Test
  public void setCredentials_adds_Authorization_header() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG));

    newCall("")
      .setCredentials("foo", "bar")
      .execute();

    RecordedRequest recordedRequest = server.takeRequest();
    verifyHeader(recordedRequest, "Authorization", "Basic " + base64("foo:bar"));
  }

  @Test
  public void setAdminCredentials_adds_Authorization_header_with_default_admin_account() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG));

    newCall("").setAdminCredentials().execute();

    RecordedRequest recordedRequest = server.takeRequest();
    verifyHeader(recordedRequest, "Authorization", "Basic " + base64("admin:admin"));
  }

  @Test
  public void setAuthenticationToken_adds_Authorization_header_with_token_as_login_and_empty_password() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG));

    newCall("").setAuthenticationToken("abcde").execute();

    RecordedRequest recordedRequest = server.takeRequest();
    verifyHeader(recordedRequest, "Authorization", "Basic " + base64("abcde:"));
  }

  @Test
  public void Authorization_header_is_not_defined_by_default() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG));

    newCall("").execute();

    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getHeader("Authorization")).isNull();
  }

  @Test
  public void execute_throws_HttpException_if_response_code_is_not_2xx() {
    server.enqueue(new MockResponse().setResponseCode(404).setBody("<error>"));

    try {
      newCall("api/system/ping").execute();
      fail();
    } catch (HttpException e) {
      assertThat(e.getMessage()).isEqualTo(format("URL [http://%s:%d/api/system/ping] returned code [404]", server.getHostName(), server.getPort()));
      assertThat(e.getCode()).isEqualTo(404);
      assertThat(e.getUrl()).isEqualTo(server.url("api/system/ping").toString());
      assertThat(e.getBody()).isEqualTo("<error>");
    }
  }

  @Test
  public void executeUnsafely_does_not_throw_HttpException_if_response_code_is_not_2xx() {
    server.enqueue(new MockResponse().setResponseCode(404).setBody("<error>"));

    HttpResponse response = newCall("api/system/ping").executeUnsafely();

    assertThat(response.getCode()).isEqualTo(404);
    assertThat(response.getBodyAsString()).isEqualTo("<error>");
  }

  @Test
  public void executeUnsafely_returns_response_if_code_is_2xx() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG));

    HttpResponse response = newCall("api/system/ping").executeUnsafely();

    verifySuccess(response, PONG);
  }

  @Test
  public void downloadToFile_overrides_content_of_existing_file() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG));
    File target = temp.newFile();
    FileUtils.write(target, "<before>");

    newCall("api/system/ping").downloadToFile(target);

    assertThat(target).hasContent(PONG);
  }

  @Test
  public void downloadToFile_creates_the_file_if_it_does_not_exist() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG));
    File dir = temp.newFolder();
    File file = new File(dir, "ping.txt");
    assertThat(file).doesNotExist();

    newCall("api/system/ping").downloadToFile(file);

    assertThat(file).exists().isFile().hasContent(PONG);
  }

  @Test
  public void downloadToFile_creates_the_file_and_its_parent_dirs_if_they_do_not_exist() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG));
    File dir = temp.newFolder();
    File file = new File(dir, "foo/bar/ping.txt");
    assertThat(file).doesNotExist();

    newCall("api/system/ping").downloadToFile(file);

    assertThat(file).exists().isFile().hasContent(PONG);
  }

  @Test
  public void downloadToFile_throws_ISE_if_target_is_a_directory() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG));
    File dir = temp.newFolder();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can not call " + server.url("api/system/ping"));

    newCall("api/system/ping").downloadToFile(dir);
  }

  @Test
  public void downloadToFile_throws_HttpException_if_response_code_is_not_2xx() throws Exception {
    server.enqueue(new MockResponse().setBody("<error>").setResponseCode(500));
    File file = temp.newFile();

    try {
      newCall("api/system/ping").downloadToFile(file);
      fail();
    } catch (HttpException e) {
      assertThat(e.getCode()).isEqualTo(500);
      assertThat(e.getUrl()).isEqualTo(server.url("api/system/ping").toString());
      assertThat(e.getBody()).isEqualTo("<error>");
    }
  }

  @Test
  public void downloadToDir_downloads_content_in_file_named_specified_by_ContentDisposition_header() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG)
      .setHeader("Content-Disposition", "attachment; filename=foo.jar"));
    File dir = temp.newFolder();

    newCall("api/system/ping").downloadToDirectory(dir);

    assertThat(new File(dir, "foo.jar")).isFile().exists().hasContent(PONG);
  }

  @Test
  public void downloadToDir_downloads_content_in_file_named_by_URL_if_ContentDisposition_header_is_not_present() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG));
    File dir = temp.newFolder();

    newCall("api/system/ping.txt").downloadToDirectory(dir);

    assertThat(new File(dir, "ping.txt")).isFile().exists().hasContent(PONG);
  }

  @Test
  public void downloadToDir_throws_ISE_if_ContentDisposition_header_is_not_present_and_URL_does_not_contain_filename() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG));
    File dir = temp.newFolder();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can not guess the target filename for download of " + server.url("") + ". Header Content-Disposition is missing or empty.");

    newCall("").downloadToDirectory(dir);
  }

  @Test
  public void downloadToDir_throws_ISE_if_ContentDisposition_header_has_vulnerability() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG).setHeader("Content-Disposition", "attachment; filename=/etc/password"));
    File dir = temp.newFolder();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Header Content-Disposition has invalid value: /etc/password");

    newCall("").downloadToDirectory(dir);
  }

  @Test
  public void downloadToDir_creates_dir_if_it_does_not_exist() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG).setHeader("Content-Disposition", "attachment; filename=foo.jar"));
    File dir = temp.newFolder();
    dir.delete();

    newCall("api/system/ping").downloadToDirectory(dir);

    assertThat(new File(dir, "foo.jar")).isFile().exists().hasContent(PONG);
  }

  @Test
  public void downloadToDir_throws_HttpException_if_response_code_is_not_2xx() throws Exception {
    server.enqueue(new MockResponse().setBody("<error>").setResponseCode(500));
    File dir = temp.newFolder();

    try {
      newCall("api/system/ping").downloadToDirectory(dir);
      fail();
    } catch (HttpException e) {
      assertThat(e.getCode()).isEqualTo(500);
      assertThat(e.getUrl()).isEqualTo(server.url("api/system/ping").toString());
      assertThat(e.getBody()).isEqualTo("<error>");
    }
  }

  @Test
  public void setTimeout_overrides_default_timeouts() throws Exception {
    // default timeouts would support this slow response
    server.enqueue(new MockResponse().setBody(PONG).setBodyDelay(10, TimeUnit.SECONDS));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can not call " + server.url("api/system/ping") + " due to network failure");

    newCall("api/system/ping")
      .setTimeoutMs(1L)
      .execute();
  }

  @Test
  public void charset_of_response_body_is_defined_by_header_ContentType() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG).setHeader("Content-Type", "text/plain; charset=iso-8859-1"));

    HttpResponse response = newCall("api/system/ping").execute();

    assertThat(response.getCharset()).isEqualTo(ISO_8859_1);
  }

  @Test
  public void charset_of_response_body_is_utf8_if_not_specified_by_header_ContentType() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG).setHeader("Content-Type", "text/plain"));

    HttpResponse response = newCall("api/system/ping").execute();

    assertThat(response.getCharset()).isEqualTo(UTF_8);
  }

  @Test
  public void charset_of_response_body_is_utf8_if_header_ContentType_is_missing() throws Exception {
    server.enqueue(new MockResponse().setBody(PONG));

    HttpResponse response = newCall("api/system/ping").execute();

    assertThat(response.getCharset()).isEqualTo(UTF_8);
  }

  private void verifySuccess(HttpResponse response, String expectedBody) throws Exception {
    assertThat(response.getCode()).isEqualTo(200);
    assertThat(response.getBodyAsString()).isEqualTo(expectedBody);
    assertThat(response.getBody()).isEqualTo(expectedBody.getBytes());
  }

  private void verifyRecorded(RecordedRequest recordedRequest, String expectedMethod, String expectedPath) {
    assertThat(recordedRequest.getMethod()).isEqualTo(expectedMethod);
    assertThat(recordedRequest.getPath()).isEqualTo("/" + expectedPath);
  }

  private void verifyHeader(RecordedRequest recordedRequest, String key, String expectedValue) {
    assertThat(recordedRequest.getHeader(key)).isEqualTo(expectedValue);
  }

  private HttpCall newCall(String path) {
    HttpClient underTest = HttpClientFactory.create();
    return underTest.newCall(server.url(path));
  }

  private static String base64(String s) {
    return Base64.getEncoder().encodeToString(s.getBytes());
  }
}
