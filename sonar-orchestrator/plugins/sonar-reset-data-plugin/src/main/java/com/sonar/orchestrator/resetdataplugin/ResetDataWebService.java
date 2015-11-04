package com.sonar.orchestrator.resetdataplugin;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.platform.BackendCleanup;

public class ResetDataWebService implements WebService {

  private final BackendCleanup backendCleanup;

  public ResetDataWebService(BackendCleanup backendCleanup) {
    this.backendCleanup = backendCleanup;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController("orchestrator")
      .setDescription("Orchestrator web service");

    controller.createAction("reset")
      .setDescription("Reset data")
      .setSince("Orchestrator 3.4")
      .setInternal(true)
      .setPost(true)
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) throws Exception {
          backendCleanup.resetData();
        }
      });

    controller.done();
  }

}
