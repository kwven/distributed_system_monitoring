package edu.ds.monitoring.agent;

import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

public class AgentMain {

    private static final String AGENT_ID = "agent-" + UUID.randomUUID().toString().substring(0, 8);
    private static final String SERVER_HOST = "localhost";
    private static final int UDP_PORT = 9999;
    private static final int TCP_PORT = 9998;
    private static final int COLLECTION_INTERVAL = 2000;

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
        System.out.println("=== Agent de Surveillance ===");
        System.out.println("ID   : " + AGENT_ID);
        System.out.println("Host : " + HOST_NAME);
        System.out.println("IP   : " + IP_ADDRESS);

        SystemCollector collector = new SystemCollector();
        UdpSender udpSender = new UdpSender(SERVER_HOST, UDP_PORT);
        TcpAlertSender tcpSender = new TcpAlertSender(SERVER_HOST, TCP_PORT);

        Thread udpThread = new Thread(() -> {
            while (true) {
                try {
                    SystemCollector.SystemMetrics metrics = collector.getSystemMetrics();
                    udpSender.send(buildUdpJson(metrics));
                    Thread.sleep(COLLECTION_INTERVAL);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.err.println("[UDP] Erreur : " + e.getMessage());
                }
            }
        });

        Thread tcpThread = new Thread(() -> {
            while (true) {
                try {
                    SystemCollector.SystemMetrics metrics = collector.getSystemMetrics();
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

    private static String buildUdpJson(SystemCollector.SystemMetrics m) {
        long ts = Instant.now().toEpochMilli();
        return String.format(
            "{\"schema\":1,\"agentId\":\"%s\",\"host\":\"%s\",\"ip\":\"%s\",\"ts\":%d," +
                "\"cpuPct\":%.1f,\"ramUsedBytes\":%d,\"ramTotalBytes\":%d," +
                "\"diskUsedBytes\":%d,\"diskTotalBytes\":%d}",
            AGENT_ID, HOST_NAME, IP_ADDRESS, ts, m.cpu, m.ramUsed, m.ramTotal, m.diskUsed, m.diskTotal
        );
    }

    private static void checkAndSendAlerts(TcpAlertSender tcpSender, SystemCollector.SystemMetrics m) {
        long ts = Instant.now().toEpochMilli();
        double ramPct = (m.ramUsed * 100.0) / m.ramTotal;
        double diskPct = (m.diskUsed * 100.0) / m.diskTotal;

        if (m.cpu >= 90) {
            tcpSender.sendAlert(buildAlertJson("CRITICAL", "CPU", m.cpu, 90, "CPU critique", ts));
        } else if (m.cpu >= 70) {
            tcpSender.sendAlert(buildAlertJson("WARN", "CPU", m.cpu, 70, "CPU warning", ts));
        }

        if (ramPct >= 90) {
            tcpSender.sendAlert(buildAlertJson("CRITICAL", "RAM", ramPct, 90, "RAM critique", ts));
        } else if (ramPct >= 75) {
            tcpSender.sendAlert(buildAlertJson("WARN", "RAM", ramPct, 75, "RAM warning", ts));
        }

        if (diskPct >= 95) {
            tcpSender.sendAlert(buildAlertJson("CRITICAL", "DISK", diskPct, 95, "Disque critique", ts));
        } else if (diskPct >= 80) {
            tcpSender.sendAlert(buildAlertJson("WARN", "DISK", diskPct, 80, "Disque warning", ts));
        }
    }

    private static String buildAlertJson(String severity, String metric, double value, double threshold, String message, long ts) {
        return String.format(
            "{\"schema\":1,\"agentId\":\"%s\",\"ts\":%d,\"severity\":\"%s\",\"metric\":\"%s\"," +
                "\"value\":%.1f,\"threshold\":%.1f,\"message\":\"%s\"}",
            AGENT_ID, ts, severity, metric, value, threshold, message
        );
    }
}
