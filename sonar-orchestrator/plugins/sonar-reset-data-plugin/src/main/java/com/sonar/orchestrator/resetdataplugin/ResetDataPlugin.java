package com.sonar.orchestrator.resetdataplugin;

import org.sonar.api.Plugin;

public final class ResetDataPlugin implements Plugin {

  /*
   * 
   * 
   * IMPORTANT : do not forget to copy the plugin JAR into src/main/resources/com/sonar/orchestrator
   */

  public void define(Context context) {
    context.addExtension(ResetDataWebService.class);
  }
}
