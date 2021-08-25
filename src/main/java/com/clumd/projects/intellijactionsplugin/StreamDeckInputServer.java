package com.clumd.projects.intellijactionsplugin;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

@Service(value = Service.Level.PROJECT)
public final class StreamDeckInputServer {

    private static final int[] PORT_RANGE = new int[]{48160, 48159, 48158, 48157, 48156, 48155, 48154, 48153, 48152, 48151, 48150};
    private static final int SOCKET_ACCEPT_TIMEOUT_IN_SECONDS = 5;
    private boolean running = true;
    private Thread clientHandlingThread;

    private ServerSocket getFreePortFromList() throws IOException {

        // Try each port in our allowed PORT RANGE until one is found to be available.
        // This is only useful if you have multiple instances of IntelliJ open since each ide instance will need its own ServerSocket.
        for (int port : PORT_RANGE) {
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
    public void run(@NotNull Project project) {
        try (
                ServerSocket server = getFreePortFromList();
        ) {
            System.out.println("StreamDeck Integration Server listening on port: " + server.getLocalPort());
            server.setSoTimeout(SOCKET_ACCEPT_TIMEOUT_IN_SECONDS * 1000);

            while (true) {
                try {
                    Socket client = server.accept();

                    clientHandlingThread = new Thread(new StreamDeckConnection(client, project));
                    clientHandlingThread.start();
                    clientHandlingThread.join();
                } catch (SocketTimeoutException e) {
                    if (!running) {
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    public void stop() {
        running = false;
        clientHandlingThread.interrupt();
    }
}
