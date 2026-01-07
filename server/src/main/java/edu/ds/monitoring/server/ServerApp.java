package edu.ds.monitoring.server;

import edu.ds.monitoring.common.config.SystemConstants;
import edu.ds.monitoring.common.dto.ThresholdConfig;

import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerApp {
    public static void main(String[] args) {
        System.out.println("Server started");

        ThresholdConfig defaults = new ThresholdConfig();
        defaults.cpuWarnPct = 70;
        defaults.cpuCritPct = 90;
        defaults.ramWarnPct = 75;
        defaults.ramCritPct = 90;
        defaults.diskWarnPct = 80;
        defaults.diskCritPct = 95;

        AgentStore store = new AgentStore();
        MetricStore metricStore = new MetricStore(15 * 60_000L, 2000);
        AlertStore alertStore = new AlertStore(1000);
        ThresholdStore thresholdStore = new ThresholdStore(defaults);

        try {
            int port = SystemConstants.RMI_REGISTRY_PORT;
            String name = "MonitoringService";
            Registry registry = LocateRegistry.createRegistry(port);
            RmiMonitoringServiceImpl service = new RmiMonitoringServiceImpl(store, metricStore, alertStore, thresholdStore);
            registry.rebind(name, service);
            System.out.println("[RMI] Bound " + name + " on port " + port);
        } catch (Exception e) {
            System.err.println("[RMI] Failed: " + e.getMessage());
            e.printStackTrace();
        }

        UdpMetricsServer udpServer = new UdpMetricsServer(NetConfig.UDP_PORT, store, metricStore, thresholdStore);
        Thread udpThread = new Thread(udpServer, "udp-metrics-server");
        udpThread.start();

        OfflineMonitor offline = new OfflineMonitor(store, NetConfig.OFFLINE_TIMEOUT_MS, NetConfig.OFFLINE_CHECK_PERIOD_MS);
        Thread offlineThread = new Thread(offline, "offline-monitor");
        offlineThread.start();
        System.out.println("Offline monitor started");

        TcpAlertServer tcp = new TcpAlertServer(NetConfig.TCP_ALERT_PORT, alertStore);
        Thread tcpThread = new Thread(tcp, "tcp-alert-server");
        tcpThread.start();
        System.out.println("TCP alert server started");

        try {
            RestApiServer rest = new RestApiServer(NetConfig.REST_PORT, store, alertStore, metricStore);
            rest.start();
            System.out.println("[REST] Listening on port " + NetConfig.REST_PORT);
        } catch (IOException e) {
            System.err.println("[REST] Failed: " + e.getMessage());
            e.printStackTrace();
        }

        while (true) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                break;
            }
            System.out.println("---- SNAPSHOTS ----");
            store.all().forEach((id, s) -> {
                System.out.println(
                    id + " cpu=" + s.cpuPct +
                    " ram=" + s.ramPct +
                    " disk=" + s.diskPct +
                    " status=" + s.status +
                    " lastSeenTs=" + s.lastSeenTs
                );
            });
        }
    }
}
