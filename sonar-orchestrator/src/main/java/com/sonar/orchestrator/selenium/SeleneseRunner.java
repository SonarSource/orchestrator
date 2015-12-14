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
package com.sonar.orchestrator.selenium;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.util.NetworkUtils;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.server.RemoteControlConfiguration;
import org.openqa.selenium.server.SeleniumServer;
import org.openqa.selenium.server.htmlrunner.HTMLLauncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeleneseRunner {

  public static final long DEFAULT_TIMEOUT = 3L * 60 * 60 * 1000;
  private static final Logger LOG = LoggerFactory.getLogger(SeleneseRunner.class);

  private final Configuration config;
  private final SeleneseSuiteGenerator suiteGenerator;

  public SeleneseRunner(Configuration config) {
    this.config = config;
    this.suiteGenerator = new SeleneseSuiteGenerator();
  }

  public void runHtmlSuite(Server runtime, Selenese selenese) {
    SeleniumServer server = null;
    try {
      File htmlSuite = prepareSuite(selenese);
      File reportFile = prepareReportFile(selenese);
      int seleniumPort = NetworkUtils.getNextAvailablePort();
      boolean multiWindow = true;

      LoggerFactory.getLogger(getClass()).info("Start Selenium on port " + seleniumPort);

      RemoteControlConfiguration conf = new RemoteControlConfiguration();
      conf.setPort(seleniumPort);
      conf.setSingleWindow(!multiWindow);
      server = new SeleniumServer(false, conf);
      server.start();

      HTMLLauncher launcher = new HTMLLauncher(server);
      String status = launcher.runHTMLSuite("*firefox", runtime.getUrl() + "/", htmlSuite, reportFile, DEFAULT_TIMEOUT, multiWindow);
      if (!"PASSED".equals(status)) {
        throw new SeleneseFailureException("Selenium test failed with status " + status + ". Check " + reportFile.getAbsolutePath());
      }

    } catch (SeleneseFailureException e) {
      throw e;

    } catch (Exception e) {
      throw new IllegalStateException("Fail to execute Selenium HTML suite: " + selenese.getHtmlSuite(), e);

    } finally {
      if (server != null) {
        server.stop();
      }
    }
  }

  private File prepareSuite(Selenese selenese) throws IOException {
    if (selenese.getHtmlSuite() != null) {
      return selenese.getHtmlSuite();
    }
    File dir = createDir("selenium-tests");
    File suite = suiteGenerator.generateSuite(selenese.getSuiteName(), dir, selenese.getHtmlTests());
    LOG.info("Selenium suite: " + suite.getCanonicalPath());
    return suite;
  }

  File prepareReportFile(Selenese selenese) throws IOException {
    File reportsDir = createDir("selenium-reports");
    File reportFile = new File(reportsDir, "report-" + selenese.getSuiteName() + ".html");
    LOG.info("Selenium report: " + reportFile.getCanonicalPath());
    return reportFile;
  }

  private File createDir(String dirname) throws IOException {
    File dir = new File(config.fileSystem().workspace(), dirname);
    if (!dir.isDirectory() || !dir.exists()) {
      FileUtils.forceMkdir(dir);
    }
    return dir;
  }
}
