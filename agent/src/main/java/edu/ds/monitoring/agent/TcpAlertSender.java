package edu.ds.monitoring.agent;

import java.io.OutputStream;
import java.net.Socket;

public class TcpAlertSender {
    private final String serverHost;
    private final int port;

    public TcpAlertSender(String serverHost, int port) {
        this.serverHost = serverHost;
        this.port = port;
    }

    public void sendAlert(String jsonAlert) {
        try (Socket socket = new Socket(serverHost, port);
             OutputStream output = socket.getOutputStream()) {

            String ndjson = jsonAlert + "\n";
            output.write(ndjson.getBytes("UTF-8"));
            output.flush();

            System.out.println("Alerte TCP NDJSON: " + jsonAlert);
        } catch (Exception e) {
            System.err.println("Erreur TCP: " + e.getMessage());
        }
    }
}
