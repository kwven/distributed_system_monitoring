package edu.ds.monitoring.agent;

import java.util.Locale;

public class AgentMain {
    private static final String AGENT_ID = java.util.UUID.randomUUID().toString();
    private static final String SERVER_HOST = "localhost";
    private static final int UDP_PORT = 9999;
    private static final int TCP_PORT = 9998;
    private static final int COLLECTION_INTERVAL = 2000;

    public static void main(String[] args) {
        System.out.println("Agent de Surveillance démarré");
        System.out.println("ID: " + AGENT_ID);

        SystemCollector collector = new SystemCollector();
        UdpSender udpSender = new UdpSender(SERVER_HOST, UDP_PORT);
        TcpAlertSender tcpSender = new TcpAlertSender(SERVER_HOST, TCP_PORT);

        Thread mainThread = new Thread(() -> {
            while (true) {
                try {
                    // Collecte
                    double cpu = collector.getCpuUsage();
                    double ram = collector.getMemoryUsage();
                    double disk = collector.getDiskUsage();

                    // FORMAT CORRECT avec point décimal
                    String message = String.format(Locale.US,
                        "%s|%.1f|%.1f|%.1f",
                        AGENT_ID, cpu, ram, disk);

                    udpSender.send(message);

                    // Alertes
                    if (cpu > 90) {
                        tcpSender.sendAlert(AGENT_ID, "CPU", cpu, "CPU > 90%");
                    }
                    if (ram > 90) {
                        tcpSender.sendAlert(AGENT_ID, "RAM", ram, "RAM > 90%");
                    }
                    if (disk > 90) {
                        tcpSender.sendAlert(AGENT_ID, "DISK", disk, "DISK > 90%");
                    }

                    Thread.sleep(COLLECTION_INTERVAL);

                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.err.println("Erreur: " + e.getMessage());
                }
            }
        });

        mainThread.setName("Agent-Main-Thread");
        mainThread.start();

        try {
            mainThread.join();
        } catch (InterruptedException e) {
            System.out.println("Agent arrêté");
        }
    }
}
