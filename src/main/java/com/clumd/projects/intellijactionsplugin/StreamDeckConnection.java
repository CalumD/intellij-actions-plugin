package com.clumd.projects.intellijactionsplugin;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

final class StreamDeckConnection implements Runnable {

    private final Logger log = Logger.getLogger(StreamDeckConnection.class.getName());
    private final ActionManager actionManager = ActionManager.getInstance();
    private final Project project;

    private final Socket streamDeckClientSocket;
    private boolean isRunning = true;

    StreamDeckConnection(Socket streamDeckClientSocket, Project project) {
        this.streamDeckClientSocket = streamDeckClientSocket;
        this.project = project;
    }

    @Override
    public void run() {
        try (
                ObjectInputStream inputStream = new ObjectInputStream(streamDeckClientSocket.getInputStream());
                ObjectOutputStream outputStream = new ObjectOutputStream(streamDeckClientSocket.getOutputStream())
        ) {
            while (isRunning) {
                handleRequest(inputStream, outputStream);
            }
        } catch (Exception e) {
            log.severe(e.getMessage());
        }
    }

    private void handleRequest(ObjectInputStream inputStream, ObjectOutputStream outputStream) throws IOException, ExecutionException, TimeoutException {

        int previousTimeout;
        int command = inputStream.readInt();
        log.info("Received command ID : " + command);

        switch (command) {
            case 0: // plugin Control: NOTHING - RESERVED AS AN 'SUCCESS' Code
                break;
            case 1: // plugin Control: PING
                writeData(outputStream, 1);
                break;
            case 2: // plugin Control: CUSTOM IDE ACTION
                previousTimeout = streamDeckClientSocket.getSoTimeout();
                streamDeckClientSocket.setSoTimeout(2000);
                String commandName = inputStream.readUTF();
                streamDeckClientSocket.setSoTimeout(previousTimeout);
                runAction(commandName);
                break;
            case 9: // plugin Control: KILL
                isRunning = false;
                break;

            case 100: // Run action: BUILD
                runAction(IdeActions.ACTION_COMPILE_PROJECT);
                break;
            case 101: // Run action: RUN
                runAction(IdeActions.ACTION_DEFAULT_RUNNER);
                break;
            case 102: // Run action: DEBUG
                runAction(IdeActions.ACTION_DEFAULT_DEBUGGER);
                break;
            case 103: // Run action: RUN WITH COVERAGE
                runAction("Coverage");
                break;
            case 104: // Run action: STOP
                runAction(IdeActions.ACTION_STOP_PROGRAM);
                break;

            case 201: // Run action: GIT UPDATE
                runAction("Vcs.UpdateProject");
                break;
            case 202: // Run action: GIT COMMIT
                runAction("CheckinProject");
                break;
            case 203: // Run action: GIT PUSH
                runAction("Vcs.Push");
                break;

            case 301: // Run action: CURSOR BACK
                runAction(IdeActions.ACTION_GOTO_BACK);
                break;
            case 302: // Run action: CURSOR FORWARD
                runAction(IdeActions.ACTION_GOTO_FORWARD);
                break;
            case 303: // Run action: ADD ADDITIONAL CARET
                runAction("EditorAddOrRemoveAdditionalCaret");
                break;
            case 304: // Run action: REFACTOR
                runAction("Refactorings.QuickListPopupAction");
                break;


            case 401: // Run action: INTELLIJ SETTINGS
                runAction(IdeActions.ACTION_SHOW_SETTINGS);
                break;
            case 402: // Run action: PROJECT STRUCTURE
                runAction("ShowProjectStructureSettings");
                break;
            case 403: // Run action: FORMAT CODE
                runAction(IdeActions.ACTION_EDITOR_REFORMAT);
                break;

            case 501: // Get Data: PROJECT NAME
                writeData(outputStream, project.getName());
                break;

            default:
                log.warning("Unknown request ID provided to plugin, doing nothing.");
        }

    }

    private void runAction(String actionID) throws ExecutionException, TimeoutException {
        final AnAction actionToPerform = actionManager.getAction(actionID);

        if (actionToPerform != null) {
            DataContext dataContext = DataManager.getInstance().getDataContextFromFocusAsync().blockingGet(100000);

            ApplicationManager.getApplication().invokeLater(() ->
                    actionToPerform.actionPerformed(
                            AnActionEvent.createFromDataContext(
                                    ActionPlaces.KEYBOARD_SHORTCUT,
                                    null,
                                    dataContext == null ? DataContext.EMPTY_CONTEXT : dataContext
                            )
                    ));
        }
    }

    private void writeData(@NotNull final ObjectOutputStream outputStream, Object data) throws IOException {
        int previousTimeout = streamDeckClientSocket.getSoTimeout();
        streamDeckClientSocket.setSoTimeout(2000);
        try {
            if (data instanceof String) {
                outputStream.writeUTF((String) data);
            } else if (data instanceof Integer) {
                outputStream.writeInt((int) data);
            } else {
                outputStream.writeObject(data);
            }
            outputStream.flush();
        } finally {
            streamDeckClientSocket.setSoTimeout(previousTimeout);
        }
    }
}
