package edu.ds.monitoring.agent;

import java.io.OutputStream;
import java.net.Socket;

/**
 * Alertes critiques via TCP
 * Exigences PDF: Alertes critiques via TCP
 */
public class TcpAlertSender {
    private final String serverHost;
    private final int port;

    public TcpAlertSender(String serverHost, int port) {
        this.serverHost = serverHost;
        this.port = port;
    }

    public void sendAlert(String agentId, String metric, double value, String message) {
        try {
            Socket socket = new Socket(serverHost, port);
            OutputStream output = socket.getOutputStream();

            String alert = agentId + "|" + metric + "|" + value + "|" + message;
            output.write(alert.getBytes());
            output.flush();
            output.close();
            socket.close();

            System.out.println("Alerte TCP: " + alert);
        } catch (Exception e) {
            System.err.println("Erreur TCP: " + e.getMessage());
        }
    }
}
