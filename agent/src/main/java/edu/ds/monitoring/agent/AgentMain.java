package edu.ds.monitoring.agent;

import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

public class AgentMain {

    // ================== CONFIGURATION ==================
    private static final String AGENT_ID =
        "agent-" + UUID.randomUUID().toString().substring(0, 8);

    private static final String SERVER_HOST = "localhost";
    private static final int UDP_PORT = 9999;
    private static final int TCP_PORT = 9998;
    private static final int COLLECTION_INTERVAL = 2000;

    // ================== HOST INFO ==================
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

    // ================== MAIN ==================
    public static void main(String[] args) {

        System.out.println("=== Agent de Surveillance ===");
        System.out.println("ID   : " + AGENT_ID);
        System.out.println("Host : " + HOST_NAME);
        System.out.println("IP   : " + IP_ADDRESS);

        SystemCollector collector = new SystemCollector();
        UdpSender udpSender = new UdpSender(SERVER_HOST, UDP_PORT);
        TcpAlertSender tcpSender = new TcpAlertSender(SERVER_HOST, TCP_PORT);

        // -------- THREAD UDP : métriques --------
        Thread udpThread = new Thread(() -> {
            while (true) {
                try {
                    SystemCollector.SystemMetrics metrics =
                        collector.getSystemMetrics();

                    String json = buildUdpJson(metrics);
                    udpSender.send(json);

                    Thread.sleep(COLLECTION_INTERVAL);

                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.err.println("[UDP] Erreur : " + e.getMessage());
                }
            }
        });

        // -------- THREAD TCP : alertes --------
        Thread tcpThread = new Thread(() -> {
            while (true) {
                try {
                    SystemCollector.SystemMetrics metrics =
                        collector.getSystemMetrics();

                    checkAndSendAlerts(tcpSender, metrics);

                    Thread.sleep(COLLECTION_INTERVAL);

                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.err.println("[TCP] Erreur : " + e.getMessage());
                }
            }
        });

        udpThread.setName("Agent-UDP-Thread");
        tcpThread.setName("Agent-TCP-Thread");

        udpThread.start();
        tcpThread.start();
    }

    // ================== UDP JSON ==================
    private static String buildUdpJson(SystemCollector.SystemMetrics m) {

        long ts = Instant.now().toEpochMilli();

        return String.format(
            "{\"schema\":1,\"agentId\":\"%s\",\"host\":\"%s\",\"ip\":\"%s\",\"ts\":%d," +
                "\"cpuPct\":%.1f,\"ramUsedBytes\":%d,\"ramTotalBytes\":%d," +
                "\"diskUsedBytes\":%d,\"diskTotalBytes\":%d}",
            AGENT_ID,
            HOST_NAME,
            IP_ADDRESS,
            ts,
            m.cpu,
            m.ramUsed,
            m.ramTotal,
            m.diskUsed,
            m.diskTotal
        );
    }

    // ================== ALERTES TCP ==================
    private static void checkAndSendAlerts(
        TcpAlertSender tcpSender,
        SystemCollector.SystemMetrics m) {

        long ts = Instant.now().toEpochMilli();

        double ramPct = (m.ramUsed * 100.0) / m.ramTotal;
        double diskPct = (m.diskUsed * 100.0) / m.diskTotal;

        double cpuWarn = 70, cpuCrit = 90;
        double ramWarn = 75, ramCrit = 90;
        double diskWarn = 80, diskCrit = 95;

        // CPU
        if (m.cpu >= cpuCrit) {
            tcpSender.sendAlert(buildAlertJson(
                "CRITICAL", "CPU", m.cpu, cpuCrit,
                "CPU au-dessus du seuil critique", ts));
        } else if (m.cpu >= cpuWarn) {
            tcpSender.sendAlert(buildAlertJson(
                "WARN", "CPU", m.cpu, cpuWarn,
                "CPU au-dessus du seuil d'avertissement", ts));
        }

        // RAM
        if (ramPct >= ramCrit) {
            tcpSender.sendAlert(buildAlertJson(
                "CRITICAL", "RAM", ramPct, ramCrit,
                "RAM au-dessus du seuil critique", ts));
        } else if (ramPct >= ramWarn) {
            tcpSender.sendAlert(buildAlertJson(
                "WARN", "RAM", ramPct, ramWarn,
                "RAM au-dessus du seuil d'avertissement", ts));
        }

        // DISK
        if (diskPct >= diskCrit) {
            tcpSender.sendAlert(buildAlertJson(
                "CRITICAL", "DISK", diskPct, diskCrit,
                "Disque au-dessus du seuil critique", ts));
        } else if (diskPct >= diskWarn) {
            tcpSender.sendAlert(buildAlertJson(
                "WARN", "DISK", diskPct, diskWarn,
                "Disque au-dessus du seuil d'avertissement", ts));
        }
    }

    // ================== ALERT JSON ==================
    private static String buildAlertJson(
        String severity,
        String metric,
        double value,
        double threshold,
        String message,
        long ts) {

        return String.format(
            "{\"schema\":1,\"agentId\":\"%s\",\"ts\":%d," +
                "\"severity\":\"%s\",\"metric\":\"%s\"," +
                "\"value\":%.1f,\"threshold\":%.1f,\"message\":\"%s\"}",
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
