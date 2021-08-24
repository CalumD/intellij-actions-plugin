package com.clumd.projects.intellijactionsplugin;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;

import java.io.*;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

class StreamDeckConnection implements Runnable {

    private final Socket streamDeckClientSocket;
    private final Project project;
    private boolean isRunning = true;

    StreamDeckConnection(Socket streamDeckClientSocket, Project project) {
        this.streamDeckClientSocket = streamDeckClientSocket;
        this.project = project;
    }

    @Override
    public void run() {
        try (
                ObjectInputStream inputStream = new ObjectInputStream(streamDeckClientSocket.getInputStream());
                ObjectOutputStream outputStream = new ObjectOutputStream(streamDeckClientSocket.getOutputStream());
        ) {
            while(isRunning) {
                handleRequest(inputStream, outputStream);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void handleRequest(ObjectInputStream inputStream, ObjectOutputStream outputStream) throws IOException, ExecutionException, TimeoutException {
        ActionManager actionManager = ActionManager.getInstance();
        int previousTimeout;

        AnActionEvent actionEvent = new AnActionEvent(
                null,
                Objects.requireNonNull(DataManager.getInstance().getDataContextFromFocusAsync().blockingGet(2000)),
                ActionPlaces.UNKNOWN,
                new Presentation(),
                ActionManager.getInstance(),
                0,
                false,
                false
        );

        int command = inputStream.readInt();
        System.out.println("Got command: " + command);
        switch (command) {
            case 0:
                // plugin Control: SHUTDOWN
                isRunning = false;
                break;
            case 1:
                // plugin Control: CUSTOM IDE ACTION
                previousTimeout = streamDeckClientSocket.getSoTimeout();
                streamDeckClientSocket.setSoTimeout(2000);
                String commandName = inputStream.readUTF();
                streamDeckClientSocket.setSoTimeout(previousTimeout);
                actionManager.getAction(commandName).actionPerformed(actionEvent);
                break;

            case 100:
                // Run action: BUILD
                actionManager.getAction(IdeActions.ACTION_COMPILE_PROJECT).actionPerformed(actionEvent);
                break;
            case 101:
                // Run action: RUN
                actionManager.getAction(IdeActions.ACTION_DEFAULT_RUNNER).actionPerformed(actionEvent);
                break;
            case 102:
                // Run action: DEBUG
                actionManager.getAction(IdeActions.ACTION_DEFAULT_DEBUGGER).actionPerformed(actionEvent);
                break;
            case 103:
                // Run action: RUN WITH COVERAGE
                actionManager.getAction("Coverage").actionPerformed(actionEvent);
                break;
            case 104:
                // Run action: STOP
                actionManager.getAction(IdeActions.ACTION_STOP_PROGRAM).actionPerformed(actionEvent);
                break;

            case 201:
                // Run action: GIT UPDATE
                actionManager.getAction("Vcs.UpdateProject").actionPerformed(actionEvent);
                break;
            case 202:
                // Run action: GIT COMMIT
                actionManager.getAction("CheckinProject").actionPerformed(actionEvent);
                break;
            case 203:
                // Run action: GIT PUSH
                actionManager.getAction("Vcs.Push").actionPerformed(actionEvent);
                break;

            case 301:
                // Run action: CURSOR BACK
                actionManager.getAction(IdeActions.ACTION_GOTO_BACK).actionPerformed(actionEvent);
                break;
            case 302:
                // Run action: CURSOR FORWARD
                actionManager.getAction(IdeActions.ACTION_GOTO_FORWARD).actionPerformed(actionEvent);
                break;
            case 303:
                // Run action: ADD ADDITIONAL CARET
                actionManager.getAction("EditorAddOrRemoveAdditionalCaret").actionPerformed(actionEvent);
                break;
            case 304:
                // Run action: REFACTOR
                actionManager.getAction("Refactorings.QuickListPopupAction").actionPerformed(actionEvent);
                break;


            case 401:
                // Run action: INTELLIJ SETTINGS
                actionManager.getAction(IdeActions.ACTION_SHOW_SETTINGS).actionPerformed(actionEvent);
                break;
            case 402:
                // Run action: PROJECT STRUCTURE
                actionManager.getAction("ShowProjectStructureSettings").actionPerformed(actionEvent);
                break;
            case 403:
                // Run action: FORMAT CODE
                actionManager.getAction(IdeActions.ACTION_EDITOR_REFORMAT).actionPerformed(actionEvent);
                break;

            case 501:
                // Get Data: PROJECT NAME
                previousTimeout = streamDeckClientSocket.getSoTimeout();
                streamDeckClientSocket.setSoTimeout(2000);
                try {
                    outputStream.writeChars(project.getName());
                } finally {
                    streamDeckClientSocket.setSoTimeout(previousTimeout);
                }
                break;



            case 999:
                System.out.println("testing only.");
                break;

            default:
                System.err.println("Unknown request ID provided to plugin, doing nothing.");
        }
    }
}
