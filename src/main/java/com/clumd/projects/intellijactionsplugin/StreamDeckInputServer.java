package com.clumd.projects.intellijactionsplugin;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

@Service(value = Service.Level.PROJECT)
public final class StreamDeckInputServer {

    private final Logger log = Logger.getLogger(StreamDeckInputServer.class.getName());

    private static final int[] PORT_RANGE = new int[]{48160, 48159, 48158, 48157, 48156, 48155, 48154, 48153, 48152, 48151, 48150};
    private static final int SOCKET_ACCEPT_TIMEOUT_IN_SECONDS = 5;
    private boolean running = true;

    private ServerSocket getFreePortFromList() throws IOException {

        // Try each port in our allowed PORT RANGE until one is found to be available.
        // This is only useful if you have multiple instances of IntelliJ open since each ide instance will need its own ServerSocket.
        for (int port : PORT_RANGE) {
            log.finer("Testing if port {" + port + "} is free for new instance of integration service.");
            try {
                return new ServerSocket(port, 0, InetAddress.getLoopbackAddress());
            } catch (IOException ex) {
                // try next port
            }
        }

        // If we reach here, no port in the range is available.
        throw new IOException("No ports in range were available.");
    }

    @SuppressWarnings({"squid:S2189", "squid:S106"})
    public void run(@NotNull final Project project) {
        try (
                ServerSocket server = getFreePortFromList()
        ) {
            log.info("StreamDeck Integration Server created and listening on port: " + server.getLocalPort());
            server.setSoTimeout(SOCKET_ACCEPT_TIMEOUT_IN_SECONDS * 1000);

            while (running) {
                acceptAndHandleClient(project, server);
            }
        } catch (Exception ex) {
            log.warning(ex.getMessage());
        }
    }

    private void acceptAndHandleClient(@NotNull final Project project, @NotNull final ServerSocket server) throws IOException {
        try {
            // Accept a new connection & handle the client.
            Socket client = server.accept();
            new StreamDeckConnection(client, project).run();
        } catch (SocketTimeoutException e) {
            // Do nothing, so we can try to accept again.
        }
    }

    public void stop() {
        running = false;
    }
}
