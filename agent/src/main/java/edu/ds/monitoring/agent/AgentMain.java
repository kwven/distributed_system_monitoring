package edu.ds.monitoring.agent;

import java.net.InetAddress;
import java.time.Instant;

public class AgentMain {
    private static final String AGENT_ID = "agent-" + java.util.UUID.randomUUID().toString().substring(0, 8);
    private static final String SERVER_HOST = "localhost";
    private static final int UDP_PORT = 9999;
    private static final int TCP_PORT = 9998;
    private static final int COLLECTION_INTERVAL = 2000;

    // Nouveaux champs requis
    private static String HOST_NAME;
    private static String IP_ADDRESS;

    static {
        try {
            HOST_NAME = InetAddress.getLocalHost().getHostName();
            IP_ADDRESS = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            HOST_NAME = "unknown";
            IP_ADDRESS = "127.0.0.1";
        }
    }

    public static void main(String[] args) {
        System.out.println("Agent de Surveillance - CONTRACT v1");
        System.out.println("ID: " + AGENT_ID);
        System.out.println("Host: " + HOST_NAME);
        System.out.println("IP: " + IP_ADDRESS);

        SystemCollector collector = new SystemCollector();
        UdpSender udpSender = new UdpSender(SERVER_HOST, UDP_PORT);
        TcpAlertSender tcpSender = new TcpAlertSender(SERVER_HOST, TCP_PORT);

        Thread mainThread = new Thread(() -> {
            while (true) {
                try {
                    // Collecte des métriques
                    SystemCollector.SystemMetrics metrics = collector.getSystemMetrics();

                    // 1. Construire le JSON UDP selon CONTRACT
                    String udpJson = buildUdpJson(metrics);
                    udpSender.send(udpJson);

                    // 2. Vérifier et envoyer alertes
                    checkAndSendAlerts(tcpSender, metrics);

                    // Intervalle de 2 secondes
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

    /**
     * Construit le JSON UDP selon CONTRACT v1
     */
    private static String buildUdpJson(SystemCollector.SystemMetrics metrics) {
        long ts = Instant.now().toEpochMilli();

        // Format JSON selon contrat
        return String.format(
            "{\"schema\":1,\"agentId\":\"%s\",\"host\":\"%s\",\"ip\":\"%s\",\"ts\":%d," +
                "\"cpuPct\":%.1f,\"ramUsedBytes\":%d,\"ramTotalBytes\":%d," +
                "\"diskUsedBytes\":%d,\"diskTotalBytes\":%d}",
            AGENT_ID,
            HOST_NAME,
            IP_ADDRESS,
            ts,
            metrics.cpu,
            metrics.ramUsed,
            metrics.ramTotal,
            metrics.diskUsed,
            metrics.diskTotal
        );
    }

    /**
     * Vérifie les seuils et envoie les alertes
     */
    private static void checkAndSendAlerts(TcpAlertSender tcpSender, SystemCollector.SystemMetrics metrics) {
        long ts = Instant.now().toEpochMilli();

        // Calculer les pourcentages
        double ramPct = (metrics.ramUsed * 100.0) / metrics.ramTotal;
        double diskPct = (metrics.diskUsed * 100.0) / metrics.diskTotal;

        // Seuils (à définir - exemples)
        double cpuWarn = 70.0, cpuCrit = 90.0;
        double ramWarn = 75.0, ramCrit = 90.0;
        double diskWarn = 80.0, diskCrit = 95.0;

        // Vérifier CPU
        if (metrics.cpu >= cpuCrit) {
            String alertJson = buildAlertJson("CRITICAL", "CPU", metrics.cpu, cpuCrit, "CPU au-dessus du seuil critique", ts);
            tcpSender.sendAlert(alertJson);
        } else if (metrics.cpu >= cpuWarn) {
            String alertJson = buildAlertJson("WARN", "CPU", metrics.cpu, cpuWarn, "CPU au-dessus du seuil d'avertissement", ts);
            tcpSender.sendAlert(alertJson);
        }

        // Vérifier RAM
        if (ramPct >= ramCrit) {
            String alertJson = buildAlertJson("CRITICAL", "RAM", ramPct, ramCrit, "RAM au-dessus du seuil critique", ts);
            tcpSender.sendAlert(alertJson);
        } else if (ramPct >= ramWarn) {
            String alertJson = buildAlertJson("WARN", "RAM", ramPct, ramWarn, "RAM au-dessus du seuil d'avertissement", ts);
            tcpSender.sendAlert(alertJson);
        }

        // Vérifier DISK
        if (diskPct >= diskCrit) {
            String alertJson = buildAlertJson("CRITICAL", "DISK", diskPct, diskCrit, "Disque au-dessus du seuil critique", ts);
            tcpSender.sendAlert(alertJson);
        } else if (diskPct >= diskWarn) {
            String alertJson = buildAlertJson("WARN", "DISK", diskPct, diskWarn, "Disque au-dessus du seuil d'avertissement", ts);
            tcpSender.sendAlert(alertJson);
        }
    }

    /**
     * Construit le JSON d'alerte selon CONTRACT v1
     */
    private static String buildAlertJson(String severity, String metric, double value, double threshold, String message, long ts) {
        return String.format(
            "{\"schema\":1,\"agentId\":\"%s\",\"ts\":%d,\"severity\":\"%s\"," +
                "\"metric\":\"%s\",\"value\":%.1f,\"threshold\":%.1f,\"message\":\"%s\"}",
            AGENT_ID,
            ts,
            severity,
            metric,
            value,
            threshold,
            message
        );
    }
}
