package edu.ds.monitoring.agent;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpSender {
    private final String serverHost;
    private final int port;

    public UdpSender(String serverHost, int port) {
        this.serverHost = serverHost;
        this.port = port;
    }

    public void send(String jsonMessage) {
        try {
            DatagramSocket socket = new DatagramSocket();
            byte[] data = jsonMessage.getBytes("UTF-8"); // UTF-8 selon contrat
            InetAddress address = InetAddress.getByName(serverHost);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
            socket.close();

            // Debug: afficher le début du JSON
            System.out.println("UDP JSON envoyé: " + jsonMessage.substring(0, Math.min(80, jsonMessage.length())) + "...");

        } catch (Exception e) {
            System.err.println("Erreur UDP: " + e.getMessage());
        }
    }
}
