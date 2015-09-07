package com.sonar.orchestrator.resetdataplugin;

import org.sonar.api.SonarPlugin;

import java.util.Arrays;
import java.util.List;

public final class ResetDataPlugin extends SonarPlugin {

  /*
   * 
   * 
   * IMPORTANT : do not forget to copy the plugin JAR into src/main/resources/com/sonar/orchestrator
   */

  public List getExtensions() {
    return Arrays.asList(ResetDataWebService.class);
  }
}
