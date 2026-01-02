package edu.ds.monitoring.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TcpAlertServer implements Runnable {
    private final int port;
    private final AlertStore store;
    private volatile boolean running = true;

    public TcpAlertServer(int port, AlertStore store) {
        this.port = port;
        this.store = store;
    }

    public void stop() { running = false; }

    @Override
    public void run() {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("[TCP] Alert server listening on port " + port);

            while (running) {
                Socket client = server.accept();
                new Thread(() -> handle(client), "tcp-alert-client").start();
            }

        } catch (IOException e) {
            System.err.println("[TCP] Server error: " + e.getMessage());
        }
    }

    private void handle(Socket client) {
        try (client;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = in.readLine()) != null) {
                String agentId = SimpleJson.getString(line, "agentId"); // si présent
                store.update(agentId, line);
                System.out.println("[TCP] Alert from agent=" + agentId + " -> " + line);
            }

        } catch (IOException e) {
            System.err.println("[TCP] Client error: " + e.getMessage());
        }
    }
}
