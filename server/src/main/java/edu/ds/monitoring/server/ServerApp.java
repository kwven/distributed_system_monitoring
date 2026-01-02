package edu.ds.monitoring.server;
import edu.ds.monitoring.common.dto.ThresholdConfig;




public class ServerApp {
    public static void main(String[] args) {
        System.out.println("Server baseline started");
        ThresholdConfig th = new ThresholdConfig();
        th.cpuWarnPct = 70;
        th.cpuCritPct = 90;
        th.ramWarnPct = 75;
        th.ramCritPct = 90;
        th.diskWarnPct = 80;
        th.diskCritPct = 95;

        AgentStore store = new AgentStore();

        UdpMetricsServer udpServer = new UdpMetricsServer(NetConfig.UDP_PORT, store, th);
        Thread udpThread = new Thread(udpServer, "udp-metrics-server");
        udpThread.start();
        OfflineMonitor offline = new OfflineMonitor(store, NetConfig.OFFLINE_TIMEOUT_MS, NetConfig.OFFLINE_CHECK_PERIOD_MS);
        Thread offlineThread = new Thread(offline, "offline-monitor");
        offlineThread.start();
        System.out.println("Offline monitor started ✅");


        System.out.println("UDP server started");
        AlertStore alertStore = new AlertStore();
        TcpAlertServer tcp = new TcpAlertServer(NetConfig.TCP_ALERT_PORT, alertStore);
        Thread tcpThread = new Thread(tcp, "tcp-alert-server");
        tcpThread.start();
        System.out.println("TCP alert server started ✅");

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
