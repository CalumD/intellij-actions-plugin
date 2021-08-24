package com.clumd.projects.intellijactionsplugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public class StreamDeckInputServer implements StartupActivity {

    private static final int[] PORT_RANGE = new int[]{48160, 48159, 48158, 48157, 48156, 48155, 48154, 48153, 48152, 48151, 48150};
    private Project project;

    private static ServerSocket getFreePortFromList() throws IOException {

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

    @SuppressWarnings({"InfiniteLoopStatement", "squid:S2189"})
    @Override
    public void runActivity(@NotNull Project project) {
        this.project = project;

        try (
                ServerSocket server = getFreePortFromList();
        ) {
            System.out.println("StreamDeck Integration Server listening on port: " + server.getLocalPort());

            while (true) {
                new StreamDeckConnection(server.accept(), project).run();
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }
}
