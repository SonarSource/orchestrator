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
package com.sonar.orchestrator.config;

import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.version.Version;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class LicensesTest {

  private static final String TOKEN_VALUE = "the_user_token";

  @Rule
  public MockWebServer github = new MockWebServer();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void download_dev_license_before_7_2() throws Exception {
    Licenses underTest = newLicenses(true);
    github.enqueue(new MockResponse().setBody("dev1234"));

    Version version = Version.create("6.7.0.10000");
    assertThat(underTest.getLicense(Edition.DEVELOPER, version)).isEqualTo("dev1234");
    verifyRequested("/master/it/dev.txt");

    // kept in cache
    assertThat(underTest.getLicense(Edition.DEVELOPER, version)).isEqualTo("dev1234");
    assertThat(github.getRequestCount()).isEqualTo(1);

    // same license for another editions
    assertThat(underTest.getLicense(Edition.ENTERPRISE, version)).isEqualTo("dev1234");
    assertThat(underTest.getLicense(Edition.DATACENTER, version)).isEqualTo("dev1234");
    assertThat(github.getRequestCount()).isEqualTo(1);
  }

  @Test
  public void download_edition_license_since_7_2() throws Exception {
    Licenses underTest = newLicenses(true);
    github.enqueue(new MockResponse().setBody("de1234"));
    github.enqueue(new MockResponse().setBody("ee1234"));

    Version version = Version.create("7.2.0.10000");
    assertThat(underTest.getLicense(Edition.DEVELOPER, version)).isEqualTo("de1234");
    verifyRequested("/master/edition_testing/de.txt");

    // kept in cache
    assertThat(underTest.getLicense(Edition.DEVELOPER, version)).isEqualTo("de1234");
    assertThat(github.getRequestCount()).isEqualTo(1);

    // request another edition
    assertThat(underTest.getLicense(Edition.ENTERPRISE, version)).isEqualTo("ee1234");
    verifyRequested("/master/edition_testing/ee.txt");
  }

  @Test
  public void download_edition_license_and_remove_header_if_present() {
    Licenses underTest = newLicenses(true);
    github.enqueue(new MockResponse().setBody("-----\nheader\n----\nabcde\n\r\n"));

    Version version = Version.create("7.2.0.10000");
    assertThat(underTest.getLicense(Edition.DEVELOPER, version)).isEqualTo("abcde");
  }

  @Test
  public void fail_in_license_not_found() {
    github.enqueue(new MockResponse().setResponseCode(404));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to download license. URL [http://localhost:" + github.getPort() + "/master/it/dev.txt] returned code [404]");

    newLicenses(true).getLicense(Edition.DEVELOPER, Version.create("6.7.0.10000"));
  }

  @Test
  public void fail_if_connection_failure() throws Exception {
    github.shutdown();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can not call http://localhost:" + github.getPort() + "/master/it/dev.txt due to network failure");

    newLicenses(true).getLicense(Edition.DEVELOPER, Version.create("6.7.0.10000"));
  }

  @Test
  public void fail_if_github_token_is_not_defined() {
    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("Please provide your GitHub token with the property github.token or the env variable GITHUB_TOKEN");

    newLicenses(false).getLicense(Edition.DEVELOPER, Version.create("6.7.0.10000"));
  }

  private Licenses newLicenses(boolean withGithubToken) {
    Configuration configuration = Configuration.builder()
      .setProperty("github.token", withGithubToken ? TOKEN_VALUE : null)
      .build();
    return new Licenses(configuration, "http://localhost:" + github.getPort() + "/");
  }

  private void verifyRequested(String path) throws Exception {
    RecordedRequest recordedRequest = github.takeRequest();
    assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("token " + TOKEN_VALUE);
    assertThat(recordedRequest.getRequestUrl().encodedPath()).isEqualTo(path);
  }
}
