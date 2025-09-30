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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;
import mockwebserver3.SocketEffect;
import mockwebserver3.junit4.MockWebServerRule;
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
  public MockWebServerRule mockWebServerRule = new MockWebServerRule();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60L));

  private static String base64(String s) {
    return Base64.getEncoder().encodeToString(s.getBytes());
  }

  @Test
  public void setHeader_adds_header_to_http_request() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG).build());

    HttpResponse response = newCall("")
      .setHeader("foo", "bar")
      .execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = mockWebServerRule.getServer().takeRequest();
    assertThat(recordedRequest.getHeaders().get("foo")).isEqualTo("bar");
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
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG).build());

    HttpResponse response = newCall("").execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = mockWebServerRule.getServer().takeRequest();
    verifyRecorded(recordedRequest, "GET", "");
  }

  @Test
  public void GET_parameters_should_be_sent_in_url_query() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG).build());

    HttpResponse response = newCall("api/system/ping")
      .setParam("foo", "foz")
      .setParam("bar", "baz")
      .execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = mockWebServerRule.getServer().takeRequest();
    verifyRecorded(recordedRequest, "GET", "api/system/ping?foo=foz&bar=baz");
  }

  @Test
  public void GET_parameter_key_with_null_value_is_set_in_url_query() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG).build());

    HttpResponse response = newCall("api/system/ping")
      .setParam("foo", null)
      .execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = mockWebServerRule.getServer().takeRequest();
    verifyRecorded(recordedRequest, "GET", "api/system/ping?foo");
  }

  @Test
  public void POST_parameter_with_null_value() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG).build());

    HttpResponse response = newCall("api/system/ping")
      .setMethod(HttpMethod.POST)
      .setParam("foo", null)
      .execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = mockWebServerRule.getServer().takeRequest();
    verifyRecorded(recordedRequest, "POST", "api/system/ping");
    assertThat(recordedRequest.getBody().utf8()).isEmpty();
  }

  @Test
  public void GET_parameters_defined_with_setParams_should_be_sent_in_url_query() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG).build());

    HttpResponse response = newCall("api/system/ping")
      .setParams("foo", "foz", "bar", "baz")
      .execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = mockWebServerRule.getServer().takeRequest();
    verifyRecorded(recordedRequest, "GET", "api/system/ping?foo=foz&bar=baz");
  }

  @Test
  public void setParams_throws_IAE_if_odd_number_of_varargs() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Expecting even number of arguments: [one, two, three]");

    newCall("api/system/ping")
      .setParams("foo", "foz", "one", "two", "three");
  }

  @Test
  public void parameters_of_POST_request_should_be_sent_in_body() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG).build());

    HttpResponse response = newCall("api/system/ping")
      .setMethod(HttpMethod.POST)
      .setParam("foo", "foz")
      .setParam("bar", "baz")
      .execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = mockWebServerRule.getServer().takeRequest();
    verifyRecorded(recordedRequest, "POST", "api/system/ping");
    assertThat(recordedRequest.getBody().utf8()).isEqualTo("foo=foz&bar=baz");
  }

  @Test
  public void parameters_of_MULTIPART_POST_request_should_be_sent_as_multipart_in_body() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG).build());

    HttpResponse response = newCall("api/system/ping")
      .setMethod(HttpMethod.MULTIPART_POST)
      .setParam("foo", "foz")
      .setParam("bar", "baz")
      .execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = mockWebServerRule.getServer().takeRequest();
    verifyRecorded(recordedRequest, "POST", "api/system/ping");
    assertThat(recordedRequest.getBody().utf8())
      .containsSubsequence("Content-Disposition: form-data; name=\"foo\"", "foz", "Content-Disposition: form-data; name=\"bar\"", "baz");
  }

  @Test
  public void HttpResponse_contains_headers() {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG)
      .setHeader("foo", "foo_val")
      .setHeader("bar", "bar_val")
      .build());

    HttpResponse response = newCall("api/system/ping").execute();

    assertThat(response.getHeader("foo")).isEqualTo("foo_val");
    assertThat(response.getHeader("bar")).isEqualTo("bar_val");
    assertThat(response.getHeader("missing")).isNull();
  }

  @Test
  public void user_agent_should_be_set_with_default_value_if_not_manually_defined() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG).build());

    newCall("").execute();

    RecordedRequest recordedRequest = mockWebServerRule.getServer().takeRequest();
    verifyHeader(recordedRequest, "User-Agent", "Orchestrator");
  }

  @Test
  public void user_agent_should_be_overridden_if_defined() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG).build());

    newCall("")
      .setHeader("User-Agent", "Firefox")
      .execute();

    RecordedRequest recordedRequest = mockWebServerRule.getServer().takeRequest();
    verifyHeader(recordedRequest, "User-Agent", "Firefox");
  }

  @Test
  public void setCredentials_adds_Authorization_header() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG).build());

    newCall("")
      .setCredentials("foo", "bar")
      .execute();

    RecordedRequest recordedRequest = mockWebServerRule.getServer().takeRequest();
    verifyHeader(recordedRequest, "Authorization", "Basic " + base64("foo:bar"));
  }

  @Test
  public void setAdminCredentials_adds_Authorization_header_with_default_admin_account() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG).build());

    newCall("").setAdminCredentials().execute();

    RecordedRequest recordedRequest = mockWebServerRule.getServer().takeRequest();
    verifyHeader(recordedRequest, "Authorization", "Basic " + base64("admin:admin"));
  }

  @Test
  public void setAuthenticationToken_adds_Authorization_header_with_token_as_login_and_empty_password() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG).build());

    newCall("").setAuthenticationToken("abcde").execute();

    RecordedRequest recordedRequest = mockWebServerRule.getServer().takeRequest();
    verifyHeader(recordedRequest, "Authorization", "Basic " + base64("abcde:"));
  }

  @Test
  public void Authorization_header_is_not_defined_by_default() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG).build());

    newCall("").execute();

    RecordedRequest recordedRequest = mockWebServerRule.getServer().takeRequest();
    assertThat(recordedRequest.getHeaders().get("Authorization")).isNull();
  }

  @Test
  public void execute_throws_HttpException_if_response_code_is_not_2xx() {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().code(404).body("<error>").build());

    try {
      newCall("api/system/ping").execute();
      fail();
    } catch (HttpException e) {
      assertThat(e.getMessage())
        .isEqualTo(format("URL [http://%s:%d/api/system/ping] returned code [404]", mockWebServerRule.getServer().getHostName(), mockWebServerRule.getServer().getPort()));
      assertThat(e.getCode()).isEqualTo(404);
      assertThat(e.getUrl()).isEqualTo(mockWebServerRule.getServer().url("api/system/ping").toString());
      assertThat(e.getBody()).isEqualTo("<error>");
    }
  }

  @Test
  public void executeUnsafely_does_not_throw_HttpException_if_response_code_is_not_2xx() {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().code(404).body("<error>").build());

    HttpResponse response = newCall("api/system/ping").executeUnsafely();

    assertThat(response.getCode()).isEqualTo(404);
    assertThat(response.getBodyAsString()).isEqualTo("<error>");
  }

  @Test
  public void executeUnsafely_returns_response_if_code_is_2xx() {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG).build());

    HttpResponse response = newCall("api/system/ping").executeUnsafely();

    verifySuccess(response, PONG);
  }

  @Test
  public void downloadToFile_overrides_content_of_existing_file() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG).build());
    File target = temp.newFile();
    FileUtils.write(target, "<before>", StandardCharsets.UTF_8);

    newCall("api/system/ping").downloadToFile(target);

    assertThat(target).hasContent(PONG);
  }

  @Test
  public void downloadToFile_creates_the_file_if_it_does_not_exist() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG).build());
    File dir = temp.newFolder();
    File file = new File(dir, "ping.txt");
    assertThat(file).doesNotExist();

    newCall("api/system/ping").downloadToFile(file);

    assertThat(file).exists().isFile().hasContent(PONG);
  }

  @Test
  public void downloadToFile_creates_the_file_and_its_parent_dirs_if_they_do_not_exist() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG).build());
    File dir = temp.newFolder();
    File file = new File(dir, "foo/bar/ping.txt");
    assertThat(file).doesNotExist();

    newCall("api/system/ping").downloadToFile(file);

    assertThat(file).exists().isFile().hasContent(PONG);
  }

  @Test
  public void downloadToFile_throws_ISE_if_target_is_a_directory() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG).build());
    File dir = temp.newFolder();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can not call " + mockWebServerRule.getServer().url("api/system/ping"));

    newCall("api/system/ping").downloadToFile(dir);
  }

  @Test
  public void downloadToFile_throws_HttpException_if_response_code_is_not_2xx() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().code(500).body("<error>").build());
    File file = temp.newFile();

    try {
      newCall("api/system/ping").downloadToFile(file);
      fail();
    } catch (HttpException e) {
      assertThat(e.getCode()).isEqualTo(500);
      assertThat(e.getUrl()).isEqualTo(mockWebServerRule.getServer().url("api/system/ping").toString());
      assertThat(e.getBody()).isEqualTo("<error>");
    }
  }

  @Test
  public void downloadToDir_downloads_content_in_file_named_specified_by_ContentDisposition_header() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG)
      .setHeader("Content-Disposition", "attachment; filename=foo.jar")
      .build());
    File dir = temp.newFolder();

    newCall("api/system/ping").downloadToDirectory(dir);

    assertThat(new File(dir, "foo.jar")).isFile().exists().hasContent(PONG);
  }

  @Test
  public void downloadToDir_downloads_content_in_file_named_by_URL_if_ContentDisposition_header_is_not_present() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG).build());
    File dir = temp.newFolder();

    newCall("api/system/ping.txt").downloadToDirectory(dir);

    assertThat(new File(dir, "ping.txt")).isFile().exists().hasContent(PONG);
  }

  @Test
  public void downloadToDir_throws_ISE_if_ContentDisposition_header_is_not_present_and_URL_does_not_contain_filename() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG).build());
    File dir = temp.newFolder();

    expectedException.expect(IllegalStateException.class);
    expectedException
      .expectMessage("Can not guess the target filename for download of " + mockWebServerRule.getServer().url("") + ". Header Content-Disposition is missing or empty.");

    newCall("").downloadToDirectory(dir);
  }

  @Test
  public void downloadToDir_throws_ISE_if_ContentDisposition_header_has_vulnerability() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG)
      .setHeader("Content-Disposition", "attachment; filename=/etc/password")
      .build());
    File dir = temp.newFolder();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Header Content-Disposition has invalid value: /etc/password");

    newCall("").downloadToDirectory(dir);
  }

  @Test
  public void downloadToDir_creates_dir_if_it_does_not_exist() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG)
      .setHeader("Content-Disposition", "attachment; filename=foo.jar")
      .build());
    File dir = temp.newFolder();
    dir.delete();

    newCall("api/system/ping").downloadToDirectory(dir);

    assertThat(new File(dir, "foo.jar")).isFile().exists().hasContent(PONG);
  }

  @Test
  public void downloadToDir_throws_HttpException_if_response_code_is_not_2xx() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body("<error>").code(500).build());
    File dir = temp.newFolder();

    try {
      newCall("api/system/ping").downloadToDirectory(dir);
      fail();
    } catch (HttpException e) {
      assertThat(e.getCode()).isEqualTo(500);
      assertThat(e.getUrl()).isEqualTo(mockWebServerRule.getServer().url("api/system/ping").toString());
      assertThat(e.getBody()).isEqualTo("<error>");
    }
  }

  @Test
  public void setTimeout_overrides_default_timeouts() {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().onResponseStart(SocketEffect.Stall.INSTANCE).build());

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can not call " + mockWebServerRule.getServer().url("api/system/ping") + " due to network failure");

    newCall("api/system/ping")
      .setTimeoutMs(1L)
      .execute();
  }

  @Test
  public void charset_of_response_body_is_defined_by_header_ContentType() {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG)
      .setHeader("Content-Type", "text/plain; charset=iso-8859-1")
      .build());

    HttpResponse response = newCall("api/system/ping").execute();

    assertThat(response.getCharset()).isEqualTo(ISO_8859_1);
  }

  @Test
  public void charset_of_response_body_is_utf8_if_not_specified_by_header_ContentType() {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG)
      .setHeader("Content-Type", "text/plain")
      .build());

    HttpResponse response = newCall("api/system/ping").execute();

    assertThat(response.getCharset()).isEqualTo(UTF_8);
  }

  @Test
  public void charset_of_response_body_is_utf8_if_header_ContentType_is_missing() {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(PONG).build());

    HttpResponse response = newCall("api/system/ping").execute();

    assertThat(response.getCharset()).isEqualTo(UTF_8);
  }

  private void verifySuccess(HttpResponse response, String expectedBody) {
    assertThat(response.getCode()).isEqualTo(200);
    assertThat(response.getBodyAsString()).isEqualTo(expectedBody);
    assertThat(response.getBody()).isEqualTo(expectedBody.getBytes());
  }

  private void verifyRecorded(RecordedRequest recordedRequest, String expectedMethod, String expectedPath) {
    assertThat(recordedRequest.getMethod()).isEqualTo(expectedMethod);
    assertThat(recordedRequest.getTarget()).isEqualTo("/" + expectedPath);
  }

  private void verifyHeader(RecordedRequest recordedRequest, String key, String expectedValue) {
    assertThat(recordedRequest.getHeaders().get(key)).isEqualTo(expectedValue);
  }

  private HttpCall newCall(String path) {
    HttpClient underTest = HttpClientFactory.create();
    return underTest.newCall(mockWebServerRule.getServer().url(path));
  }
}
