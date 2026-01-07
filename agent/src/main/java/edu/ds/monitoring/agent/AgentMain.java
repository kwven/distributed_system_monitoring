package edu.ds.monitoring.agent;

import edu.ds.monitoring.common.api.MonitoringService;
import edu.ds.monitoring.common.config.SystemConstants;
import edu.ds.monitoring.common.dto.ThresholdConfig;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class AgentMain {
    private static final String AGENT_ID = loadOrCreateAgentId();
    private static final String SERVER_HOST = "localhost";
    private static final int UDP_PORT = 9999;
    private static final int TCP_PORT = 9998;
    private static final int COLLECTION_INTERVAL = 2000;
    private static final int ALERT_INTERVAL = 1000;
    private static final int THRESHOLD_REFRESH_INTERVAL = 5000;
    private static boolean DEBUG = false;
    private static long lastDebugTs = 0;
    private static final String AGENT_ID_FILE = ".ds-monitoring-agent-id";

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
        for (String arg : args) {
            if ("--debug".equalsIgnoreCase(arg)) DEBUG = true;
        }
        System.out.println("Agent started");
        System.out.println("ID: " + AGENT_ID);
        System.out.println("Host: " + HOST_NAME);
        System.out.println("IP: " + IP_ADDRESS);

        SystemCollector collector = new SystemCollector();
        UdpSender udpSender = new UdpSender(SERVER_HOST, UDP_PORT);
        TcpAlertSender tcpSender = new TcpAlertSender(SERVER_HOST, TCP_PORT);

        AtomicReference<SystemCollector.SystemMetrics> lastMetrics = new AtomicReference<>();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
        AtomicReference<ThresholdConfig> thresholdsRef = new AtomicReference<>(defaultThresholds());

        scheduler.scheduleAtFixedRate(() -> {
            try {
                SystemCollector.SystemMetrics metrics = collector.getSystemMetrics();
                lastMetrics.set(metrics);
                String udpJson = buildUdpJson(metrics);
                udpSender.send(udpJson);
                maybeLogDebug(udpJson, metrics);
            } catch (Exception e) {
                System.err.println("UDP error: " + e.getMessage());
            }
        }, 0, COLLECTION_INTERVAL, TimeUnit.MILLISECONDS);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                SystemCollector.SystemMetrics metrics = lastMetrics.get();
                if (metrics == null) return;
                checkAndSendAlerts(tcpSender, metrics, thresholdsRef.get());
            } catch (Exception e) {
                System.err.println("TCP error: " + e.getMessage());
            }
        }, 0, ALERT_INTERVAL, TimeUnit.MILLISECONDS);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                ThresholdConfig cfg = fetchThresholds();
                thresholdsRef.set(cfg);
                if (DEBUG) {
                    System.out.println(String.format(
                        "[DEBUG] thresholds cpu=%s/%s ram=%s/%s disk=%s/%s",
                        cfg.cpuWarnPct, cfg.cpuCritPct,
                        cfg.ramWarnPct, cfg.ramCritPct,
                        cfg.diskWarnPct, cfg.diskCritPct
                    ));
                }
            } catch (Exception e) {
                if (DEBUG) {
                    System.out.println("[DEBUG] thresholds fetch failed: " + e.getMessage());
                }
            }
        }, 0, THRESHOLD_REFRESH_INTERVAL, TimeUnit.MILLISECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> scheduler.shutdownNow()));

        try {
            new CountDownLatch(1).await();
        } catch (InterruptedException e) {
            System.out.println("Agent stopped");
        }
    }

    private static String buildUdpJson(SystemCollector.SystemMetrics metrics) {
        long ts = Instant.now().toEpochMilli();
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

    private static void checkAndSendAlerts(TcpAlertSender tcpSender, SystemCollector.SystemMetrics metrics, ThresholdConfig cfg) {
        long ts = Instant.now().toEpochMilli();
        double ramPct = (metrics.ramUsed * 100.0) / metrics.ramTotal;
        double diskPct = (metrics.diskUsed * 100.0) / metrics.diskTotal;

        ThresholdConfig th = (cfg == null) ? defaultThresholds() : cfg;
        double cpuWarn = th.cpuWarnPct, cpuCrit = th.cpuCritPct;
        double ramWarn = th.ramWarnPct, ramCrit = th.ramCritPct;
        double diskWarn = th.diskWarnPct, diskCrit = th.diskCritPct;

        if (metrics.cpu >= cpuCrit) {
            String alertJson = buildAlertJson("CRITICAL", "CPU", metrics.cpu, cpuCrit, "CPU above critical", ts);
            tcpSender.sendAlert(alertJson);
        } else if (metrics.cpu >= cpuWarn) {
            String alertJson = buildAlertJson("WARN", "CPU", metrics.cpu, cpuWarn, "CPU above warning", ts);
            tcpSender.sendAlert(alertJson);
        }

        if (ramPct >= ramCrit) {
            String alertJson = buildAlertJson("CRITICAL", "RAM", ramPct, ramCrit, "RAM above critical", ts);
            tcpSender.sendAlert(alertJson);
        } else if (ramPct >= ramWarn) {
            String alertJson = buildAlertJson("WARN", "RAM", ramPct, ramWarn, "RAM above warning", ts);
            tcpSender.sendAlert(alertJson);
        }

        if (diskPct >= diskCrit) {
            String alertJson = buildAlertJson("CRITICAL", "DISK", diskPct, diskCrit, "Disk above critical", ts);
            tcpSender.sendAlert(alertJson);
        } else if (diskPct >= diskWarn) {
            String alertJson = buildAlertJson("WARN", "DISK", diskPct, diskWarn, "Disk above warning", ts);
            tcpSender.sendAlert(alertJson);
        }
    }

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

    private static void maybeLogDebug(String udpJson, SystemCollector.SystemMetrics metrics) {
        if (!DEBUG) return;
        long now = System.currentTimeMillis();
        if (now - lastDebugTs < 5000) return;
        lastDebugTs = now;
        double ramPct = (metrics.ramTotal == 0) ? 0 : (metrics.ramUsed * 100.0 / metrics.ramTotal);
        double diskPct = (metrics.diskTotal == 0) ? 0 : (metrics.diskUsed * 100.0 / metrics.diskTotal);
        System.out.println("[DEBUG] UDP JSON: " + udpJson);
        System.out.println(String.format(
            "[DEBUG] raw cpu=%.1f ramUsed=%d ramTotal=%d (%.1f%%) diskUsed=%d diskTotal=%d (%.1f%%)",
            metrics.cpu,
            metrics.ramUsed, metrics.ramTotal, ramPct,
            metrics.diskUsed, metrics.diskTotal, diskPct
        ));
    }

    private static ThresholdConfig defaultThresholds() {
        return new ThresholdConfig();
    }

    private static String loadOrCreateAgentId() {
        Path path = Paths.get(System.getProperty("user.home"), AGENT_ID_FILE);
        try {
            if (Files.exists(path)) {
                String id = Files.readString(path, StandardCharsets.UTF_8).trim();
                if (!id.isEmpty()) return id;
            }
        } catch (Exception e) {
            // fall through to generate a new id
        }
        String id = "agent-" + UUID.randomUUID().toString().substring(0, 8);
        try {
            Files.writeString(
                path,
                id + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (Exception e) {
            // ignore persistence errors, keep generated id
        }
        return id;
    }

    private static ThresholdConfig fetchThresholds() throws Exception {
        Registry registry = LocateRegistry.getRegistry(SERVER_HOST, SystemConstants.RMI_REGISTRY_PORT);
        MonitoringService stub = (MonitoringService) registry.lookup(SystemConstants.RMI_BINDING_NAME);
        ThresholdConfig cfg = stub.getThresholds(AGENT_ID);
        return (cfg == null) ? defaultThresholds() : cfg;
    }
}
