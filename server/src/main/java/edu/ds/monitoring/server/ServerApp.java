package edu.ds.monitoring.server;

import edu.ds.monitoring.common.config.SystemConstants;
import edu.ds.monitoring.common.dto.ThresholdConfig;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerApp {
    public static void main(String[] args) {
        System.out.println("Server baseline started");

        // Thresholds (server-side rules)
        ThresholdConfig th = new ThresholdConfig();
        th.cpuWarnPct = 70;
        th.cpuCritPct = 90;
        th.ramWarnPct = 75;
        th.ramCritPct = 90;
        th.diskWarnPct = 80;
        th.diskCritPct = 95;

        AgentStore store = new AgentStore();
        AlertStore alertStore = new AlertStore();

        // ---- UDP metrics server ----
        UdpMetricsServer udpServer = new UdpMetricsServer(NetConfig.UDP_PORT, store, th);
        Thread udpThread = new Thread(udpServer, "udp-metrics-server");
        udpThread.start();
        System.out.println("UDP server started ✅");

        // ---- OFFLINE monitor ----
        OfflineMonitor offline = new OfflineMonitor(store, NetConfig.OFFLINE_TIMEOUT_MS, NetConfig.OFFLINE_CHECK_PERIOD_MS);
        Thread offlineThread = new Thread(offline, "offline-monitor");
        offlineThread.start();
        System.out.println("Offline monitor started ✅");

        // ---- TCP alert server ----
        TcpAlertServer tcp = new TcpAlertServer(NetConfig.TCP_ALERT_PORT, alertStore);
        Thread tcpThread = new Thread(tcp, "tcp-alert-server");
        tcpThread.start();
        System.out.println("TCP alert server started ✅");

        // ---- RMI ----
        RmiMonitoringServiceImpl service = null;
        try {
            int port = SystemConstants.RMI_REGISTRY_PORT;
            Registry registry = LocateRegistry.createRegistry(port);

            service = new RmiMonitoringServiceImpl(store);
            registry.rebind("MonitoringService", service);

            System.out.println("[RMI] Bound MonitoringService on port " + port);
        } catch (Exception e) {
            System.err.println("[RMI] Failed: " + e.getMessage());
            e.printStackTrace();
        }

        // ---- REST (only if RMI service exists) ----
        if (service != null) {
            try {
                RestApiServer rest = new RestApiServer(NetConfig.REST_PORT, store, service);
                rest.start();
            } catch (Exception e) {
                System.err.println("[REST] Failed: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("[REST] Not started because RMI service is null.");
        }

        // ---- Debug print snapshots (optional for dev; keep if you want) ----
        while (true) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                break;
            }

            System.out.println("---- SNAPSHOTS ----");
            store.all().forEach((id, s) -> System.out.println(
                    id + " cpu=" + s.cpuPct +
                    " ram=" + s.ramPct +
                    " disk=" + s.diskPct +
                    " status=" + s.status +
                    " lastSeenTs=" + s.lastSeenTs
            ));
        }
    }
}

