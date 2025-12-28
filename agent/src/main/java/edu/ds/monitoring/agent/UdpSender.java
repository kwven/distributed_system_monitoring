package edu.ds.monitoring.agent;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Envoi périodique via UDP
 * Exigences PDF: Envoi périodique via UDP
 */
public class UdpSender {
    private final String serverHost;
    private final int port;

    public UdpSender(String serverHost, int port) {
        this.serverHost = serverHost;
        this.port = port;
    }

    public void send(String message) {
        try {
            DatagramSocket socket = new DatagramSocket();
            byte[] data = message.getBytes();
            InetAddress address = InetAddress.getByName(serverHost);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
            socket.close();

            System.out.println("UDP envoyé: " + message);
        } catch (Exception e) {
            System.err.println("Erreur UDP: " + e.getMessage());
        }
    }
}
