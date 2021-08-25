package com.clumd.projects.intellijactionsplugin;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;

public class ProjectOpenCloseListener implements ProjectManagerListener {

    private Thread integrationServer;

    @Override
    public void projectOpened(@NotNull final Project project) {
        // Ensure this isn't part of testing
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }
        // Get the service
        StreamDeckInputServer streamDeckInputServer = ApplicationManager.getApplication().getService(StreamDeckInputServer.class);

        if (integrationServer == null) {
            integrationServer = new Thread(() -> streamDeckInputServer.run(project));
            integrationServer.start();
        }
    }

    @Override
    public void projectClosed(@NotNull final Project project) {
        // Ensure this isn't part of testing
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }
        // Stop the service
        if (integrationServer != null) {
            StreamDeckInputServer streamDeckInputServer = ApplicationManager.getApplication().getService(StreamDeckInputServer.class);
            streamDeckInputServer.stop();
            integrationServer = null;
        }

    }
}
