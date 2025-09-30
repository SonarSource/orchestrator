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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import mockwebserver3.SocketEffect;
import mockwebserver3.junit5.StartStop;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Timeout(60)
class HttpCallTest {

  private static final String PONG = "pong";

  @StartStop
  public final MockWebServer server = new MockWebServer();

  private static String base64(String s) {
    return Base64.getEncoder().encodeToString(s.getBytes());
  }

  @Test
  void setHeader_adds_header_to_http_request() throws Exception {
    server.enqueue(new MockResponse.Builder().body(PONG).build());

    HttpResponse response = newCall("")
      .setHeader("foo", "bar")
      .execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = server.takeRequest();
    Assertions.assertThat(recordedRequest.getHeaders().get("foo")).isEqualTo("bar");
  }

  @Test
  void setHeader_throws_NPE_if_key_is_null() {
    HttpCall httpCall = newCall("");

    assertThatThrownBy(() -> httpCall.setHeader(null, "foo"))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Header key cannot be null");
  }

  @Test
  void setHeader_throws_NPE_if_value_is_null() {
    HttpCall httpCall = newCall("");

    assertThatThrownBy(() -> httpCall.setHeader("foo", null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Header [foo] cannot have null value");
  }

  @Test
  void execute_GET_request_should_return_response() throws Exception {
    server.enqueue(new MockResponse.Builder().body(PONG).build());

    HttpResponse response = newCall("").execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = server.takeRequest();
    verifyRecorded(recordedRequest, "GET", "");
  }

  @Test
  void GET_parameters_should_be_sent_in_url_query() throws Exception {
    server.enqueue(new MockResponse.Builder().body(PONG).build());

    HttpResponse response = newCall("api/system/ping")
      .setParam("foo", "foz")
      .setParam("bar", "baz")
      .execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = server.takeRequest();
    verifyRecorded(recordedRequest, "GET", "api/system/ping?foo=foz&bar=baz");
  }

  @Test
  void GET_parameter_key_with_null_value_is_set_in_url_query() throws Exception {
    server.enqueue(new MockResponse.Builder().body(PONG).build());

    HttpResponse response = newCall("api/system/ping")
      .setParam("foo", null)
      .execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = server.takeRequest();
    verifyRecorded(recordedRequest, "GET", "api/system/ping?foo");
  }

  @Test
  void POST_parameter_with_null_value() throws Exception {
    server.enqueue(new MockResponse.Builder().body(PONG).build());

    HttpResponse response = newCall("api/system/ping")
      .setMethod(HttpMethod.POST)
      .setParam("foo", null)
      .execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = server.takeRequest();
    verifyRecorded(recordedRequest, "POST", "api/system/ping");
    Assertions.assertThat(recordedRequest.getBody().utf8()).isEmpty();
  }

  @Test
  void GET_parameters_defined_with_setParams_should_be_sent_in_url_query() throws Exception {
    server.enqueue(new MockResponse.Builder().body(PONG).build());

    HttpResponse response = newCall("api/system/ping")
      .setParams("foo", "foz", "bar", "baz")
      .execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = server.takeRequest();
    verifyRecorded(recordedRequest, "GET", "api/system/ping?foo=foz&bar=baz");
  }

  @Test
  void setParams_throws_IAE_if_odd_number_of_varargs() {
    HttpCall httpCall = newCall("api/system/ping");
    assertThatThrownBy(() -> httpCall.setParams("foo", "foz", "one", "two", "three"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(format("Expecting even number of arguments: [%s]", "one, two, three"));
  }

  @Test
  void parameters_of_POST_request_should_be_sent_in_body() throws Exception {
    server.enqueue(new MockResponse.Builder().body(PONG).build());

    HttpResponse response = newCall("api/system/ping")
      .setMethod(HttpMethod.POST)
      .setParam("foo", "foz")
      .setParam("bar", "baz")
      .execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = server.takeRequest();
    verifyRecorded(recordedRequest, "POST", "api/system/ping");
    Assertions.assertThat(recordedRequest.getBody().utf8()).isEqualTo("foo=foz&bar=baz");
  }

  @Test
  void parameters_of_MULTIPART_POST_request_should_be_sent_as_multipart_in_body() throws Exception {
    server.enqueue(new MockResponse.Builder().body(PONG).build());

    HttpResponse response = newCall("api/system/ping")
      .setMethod(HttpMethod.MULTIPART_POST)
      .setParam("foo", "foz")
      .setParam("bar", "baz")
      .execute();

    verifySuccess(response, PONG);
    RecordedRequest recordedRequest = server.takeRequest();
    verifyRecorded(recordedRequest, "POST", "api/system/ping");
    Assertions.assertThat(recordedRequest.getBody().utf8())
      .containsSubsequence("Content-Disposition: form-data; name=\"foo\"", "foz", "Content-Disposition: form-data; name=\"bar\"", "baz");
  }

  @Test
  void HttpResponse_contains_headers() {
    server.enqueue(new MockResponse.Builder().body(PONG)
      .setHeader("foo", "foo_val")
      .setHeader("bar", "bar_val")
      .build());

    HttpResponse response = newCall("api/system/ping").execute();

    assertThat(response.getHeader("foo")).isEqualTo("foo_val");
    assertThat(response.getHeader("bar")).isEqualTo("bar_val");
    assertThat(response.getHeader("missing")).isNull();
  }

  @Test
  void user_agent_should_be_set_with_default_value_if_not_manually_defined() throws Exception {
    server.enqueue(new MockResponse.Builder().body(PONG).build());

    newCall("").execute();

    RecordedRequest recordedRequest = server.takeRequest();
    verifyHeader(recordedRequest, "User-Agent", "Orchestrator");
  }

  @Test
  void user_agent_should_be_overridden_if_defined() throws Exception {
    server.enqueue(new MockResponse.Builder().body(PONG).build());

    newCall("")
      .setHeader("User-Agent", "Firefox")
      .execute();

    RecordedRequest recordedRequest = server.takeRequest();
    verifyHeader(recordedRequest, "User-Agent", "Firefox");
  }

  @Test
  void setCredentials_adds_Authorization_header() throws Exception {
    server.enqueue(new MockResponse.Builder().body(PONG).build());

    newCall("")
      .setCredentials("foo", "bar")
      .execute();

    RecordedRequest recordedRequest = server.takeRequest();
    verifyHeader(recordedRequest, "Authorization", "Basic " + base64("foo:bar"));
  }

  @Test
  void setAdminCredentials_adds_Authorization_header_with_default_admin_account() throws Exception {
    server.enqueue(new MockResponse.Builder().body(PONG).build());

    newCall("").setAdminCredentials().execute();

    RecordedRequest recordedRequest = server.takeRequest();
    verifyHeader(recordedRequest, "Authorization", "Basic " + base64("admin:admin"));
  }

  @Test
  void setAuthenticationToken_adds_Authorization_header_with_token_as_login_and_empty_password() throws Exception {
    server.enqueue(new MockResponse.Builder().body(PONG).build());

    newCall("").setAuthenticationToken("abcde").execute();

    RecordedRequest recordedRequest = server.takeRequest();
    verifyHeader(recordedRequest, "Authorization", "Basic " + base64("abcde:"));
  }

  @Test
  void Authorization_header_is_not_defined_by_default() throws Exception {
    server.enqueue(new MockResponse.Builder().body(PONG).build());

    newCall("").execute();

    RecordedRequest recordedRequest = server.takeRequest();
    Assertions.assertThat(recordedRequest.getHeaders().get("Authorization")).isNull();
  }

  @Test
  void execute_throws_HttpException_if_response_code_is_not_2xx() {
    server.enqueue(new MockResponse.Builder().code(404).body("<error>").build());

    HttpCall httpCall = newCall("api/system/ping");
    assertThatThrownBy(httpCall::execute)
      .isInstanceOfSatisfying(HttpException.class, e -> {
        assertThat(e.getMessage())
          .isEqualTo(String.format("URL [http://%s:%d/api/system/ping] returned code [404]", server.getHostName(), server.getPort()));
        assertThat(e.getCode()).isEqualTo(404);
        assertThat(e.getUrl()).isEqualTo(server.url("api/system/ping").toString());
        assertThat(e.getBody()).isEqualTo("<error>");
      });
  }

  @Test
  void executeUnsafely_does_not_throw_HttpException_if_response_code_is_not_2xx() {
    server.enqueue(new MockResponse.Builder().code(404).body("<error>").build());

    HttpResponse response = newCall("api/system/ping").executeUnsafely();

    assertThat(response.getCode()).isEqualTo(404);
    assertThat(response.getBodyAsString()).isEqualTo("<error>");
  }

  @Test
  void executeUnsafely_returns_response_if_code_is_2xx() {
    server.enqueue(new MockResponse.Builder().body(PONG).build());

    HttpResponse response = newCall("api/system/ping").executeUnsafely();

    verifySuccess(response, PONG);
  }

  @Test
  void downloadToFile_overrides_content_of_existing_file(@TempDir Path temp) throws Exception {
    server.enqueue(new MockResponse.Builder().body(PONG).build());
    File target = temp.resolve("file").toFile();
    FileUtils.write(target, "<before>", StandardCharsets.UTF_8);

    newCall("api/system/ping").downloadToFile(target);

    assertThat(target).hasContent(PONG);
  }

  @Test
  void downloadToFile_creates_the_file_if_it_does_not_exist(@TempDir Path dir) throws Exception {
    server.enqueue(new MockResponse.Builder().body(PONG).build());
    File file = new File(dir.toFile(), "ping.txt");
    assertThat(file).doesNotExist();

    newCall("api/system/ping").downloadToFile(file);

    assertThat(file).exists().isFile().hasContent(PONG);
  }

  @Test
  void downloadToFile_creates_the_file_and_its_parent_dirs_if_they_do_not_exist(@TempDir Path dir) throws Exception {
    server.enqueue(new MockResponse.Builder().body(PONG).build());
    File file = new File(dir.toFile(), "foo/bar/ping.txt");
    assertThat(file).doesNotExist();

    newCall("api/system/ping").downloadToFile(file);

    assertThat(file).exists().isFile().hasContent(PONG);
  }

  @Test
  void downloadToFile_throws_ISE_if_target_is_a_directory(@TempDir File dir) {
    server.enqueue(new MockResponse.Builder().body(PONG).build());

    HttpCall httpCall = newCall("api/system/ping");

    assertThatThrownBy(() -> httpCall.downloadToFile(dir))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Can not call " + server.url("api/system/ping"));
  }

  @Test
  void downloadToFile_throws_HttpException_if_response_code_is_not_2xx(@TempDir Path temp) {
    server.enqueue(new MockResponse.Builder().code(500).body("<error>").build());
    File file = temp.resolve("file").toFile();

    HttpCall httpCall = newCall("api/system/ping");
    assertThatThrownBy(() -> httpCall.downloadToFile(file))
      .isInstanceOfSatisfying(HttpException.class, e -> {
        assertThat(e.getCode()).isEqualTo(500);
        assertThat(e.getUrl()).isEqualTo(server.url("api/system/ping").toString());
        assertThat(e.getBody()).isEqualTo("<error>");
      });
  }

  @Test
  void downloadToDir_downloads_content_in_file_named_specified_by_ContentDisposition_header(@TempDir Path dir) {
    server.enqueue(new MockResponse.Builder().body(PONG)
      .setHeader("Content-Disposition", "attachment; filename=foo.jar")
      .build());

    newCall("api/system/ping").downloadToDirectory(dir.toFile());

    assertThat(new File(dir.toFile(), "foo.jar")).isFile().exists().hasContent(PONG);
  }

  @Test
  void downloadToDir_downloads_content_in_file_named_by_URL_if_ContentDisposition_header_is_not_present(@TempDir Path dir) {
    server.enqueue(new MockResponse.Builder().body(PONG).build());

    newCall("api/system/ping.txt").downloadToDirectory(dir.toFile());

    assertThat(new File(dir.toFile(), "ping.txt")).isFile().exists().hasContent(PONG);
  }

  @Test
  void downloadToDir_throws_ISE_if_ContentDisposition_header_is_not_present_and_URL_does_not_contain_filename(@TempDir File dir) {
    server.enqueue(new MockResponse.Builder().body(PONG).build());

    HttpCall httpCall = newCall("");

    assertThatThrownBy(() -> httpCall.downloadToDirectory(dir))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Can not guess the target filename for download of " + server.url("") + ". Header Content-Disposition is missing or empty.");
  }

  @Test
  void downloadToDir_throws_ISE_if_ContentDisposition_header_has_vulnerability(@TempDir File dir) {
    server.enqueue(new MockResponse.Builder().body(PONG)
      .setHeader("Content-Disposition", "attachment; filename=/etc/password")
      .build());

    HttpCall httpCall = newCall("");

    assertThatThrownBy(() -> httpCall.downloadToDirectory(dir))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Header Content-Disposition has invalid value: /etc/password");
  }

  @Test
  void downloadToDir_creates_dir_if_it_does_not_exist(@TempDir Path dir) throws IOException {
    server.enqueue(new MockResponse.Builder().body(PONG)
      .setHeader("Content-Disposition", "attachment; filename=foo.jar")
      .build());
    Files.delete(dir);

    newCall("api/system/ping").downloadToDirectory(dir.toFile());

    assertThat(new File(dir.toFile(), "foo.jar")).isFile().exists().hasContent(PONG);
  }

  @Test
  void downloadToDir_throws_HttpException_if_response_code_is_not_2xx(@TempDir File dir) {
    server.enqueue(new MockResponse.Builder().body("<error>").code(500).build());

    HttpCall httpCall = newCall("api/system/ping");

    assertThatThrownBy(() -> httpCall.downloadToDirectory(dir))
      .isInstanceOfSatisfying(HttpException.class, e -> {
        assertThat(e.getCode()).isEqualTo(500);
        assertThat(e.getUrl()).isEqualTo(server.url("api/system/ping").toString());
        assertThat(e.getBody()).isEqualTo("<error>");
      });
  }

  @Test
  void setTimeout_overrides_default_timeouts() {
    server.enqueue(new MockResponse.Builder().onResponseStart(SocketEffect.Stall.INSTANCE).build());

    HttpCall httpCall = newCall("api/system/ping")
      .setTimeoutMs(1L);

    assertThatThrownBy(httpCall::execute)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageStartingWith("Can not call " + server.url("api/system/ping") + " due to network failure");
  }

  @Test
  void charset_of_response_body_is_defined_by_header_ContentType() {
    server.enqueue(new MockResponse.Builder().body(PONG)
      .setHeader("Content-Type", "text/plain; charset=iso-8859-1")
      .build());

    HttpResponse response = newCall("api/system/ping").execute();

    assertThat(response.getCharset()).isEqualTo(ISO_8859_1);
  }

  @Test
  void charset_of_response_body_is_utf8_if_not_specified_by_header_ContentType() {
    server.enqueue(new MockResponse.Builder().body(PONG)
      .setHeader("Content-Type", "text/plain")
      .build());

    HttpResponse response = newCall("api/system/ping").execute();

    assertThat(response.getCharset()).isEqualTo(UTF_8);
  }

  @Test
  void charset_of_response_body_is_utf8_if_header_ContentType_is_missing() {
    server.enqueue(new MockResponse.Builder().body(PONG).build());

    HttpResponse response = newCall("api/system/ping").execute();

    assertThat(response.getCharset()).isEqualTo(UTF_8);
  }

  private void verifySuccess(HttpResponse response, String expectedBody) {
    assertThat(response.getCode()).isEqualTo(200);
    assertThat(response.getBodyAsString()).isEqualTo(expectedBody);
    assertThat(response.getBody()).isEqualTo(expectedBody.getBytes());
  }

  private void verifyRecorded(RecordedRequest recordedRequest, String expectedMethod, String expectedPath) {
    Assertions.assertThat(recordedRequest.getMethod()).isEqualTo(expectedMethod);
    Assertions.assertThat(recordedRequest.getTarget()).isEqualTo("/" + expectedPath);
  }

  private void verifyHeader(RecordedRequest recordedRequest, String key, String expectedValue) {
    Assertions.assertThat(recordedRequest.getHeaders().get(key)).isEqualTo(expectedValue);
  }

  private HttpCall newCall(String path) {
    HttpClient underTest = HttpClientFactory.create();
    return underTest.newCall(server.url(path));
  }
}
