package com.clumd.projects.intellijactionsplugin;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class ProjectOpenCloseListener implements ProjectManagerListener {

    private final Logger log = Logger.getLogger(ProjectOpenCloseListener.class.getName());

    private Thread integrationServer;

    @Override
    public void projectOpened(@NotNull final Project project) {
        // Ensure this isn't part of testing
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }

        log.fine("Detected a new project opening, creating a new handler service.");

        // Get the service
        StreamDeckInputServer streamDeckInputServer = ApplicationManager.getApplication().getService(StreamDeckInputServer.class);

        // Create a new instance of the service when we open a new project.
        if (integrationServer == null) {
            log.fine("Running newly created integration server.");
            integrationServer = new Thread(() -> streamDeckInputServer.run(project));
            integrationServer.start();
        } else {
            log.warning("Integration service already exists, skipping.");
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
            log.fine("Trying to terminate StreamDeck integration service.");
            StreamDeckInputServer streamDeckInputServer = ApplicationManager.getApplication().getService(StreamDeckInputServer.class);
            streamDeckInputServer.stop();
            integrationServer = null;
        }
    }
}
