package edu.ds.monitoring.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import edu.ds.monitoring.common.dto.AlertEvent;
import edu.ds.monitoring.common.dto.MetricType;
import edu.ds.monitoring.common.dto.Severity;

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
                AlertEvent alert = parseAlert(line);
                if (alert != null) {
                    store.add(alert);
                    System.out.println("[TCP] Alert from agent=" + alert.agentId + " -> " + line);
                } else {
                    System.out.println("[TCP] Invalid alert -> " + line);
                }
            }

        } catch (IOException e) {
            System.err.println("[TCP] Client error: " + e.getMessage());
        }
    }

    private AlertEvent parseAlert(String json) {
        String agentId = SimpleJson.getString(json, "agentId");
        if (agentId == null) agentId = "UNKNOWN";
        Long ts = SimpleJson.getLong(json, "ts");
        String sev = SimpleJson.getString(json, "severity");
        String metric = SimpleJson.getString(json, "metric");
        Double value = SimpleJson.getDouble(json, "value");
        Double threshold = SimpleJson.getDouble(json, "threshold");
        String message = SimpleJson.getString(json, "message");

        AlertEvent a = new AlertEvent();
        a.agentId = agentId;
        a.ts = ts != null ? ts : System.currentTimeMillis();
        a.value = value != null ? value : 0.0;
        a.threshold = threshold != null ? threshold : 0.0;
        a.message = message == null ? "" : message;
        try {
            if (sev != null) a.severity = Severity.valueOf(sev);
        } catch (IllegalArgumentException ignored) {
        }
        try {
            if (metric != null) a.metric = MetricType.valueOf(metric);
        } catch (IllegalArgumentException ignored) {
        }
        return a;
    }
}
