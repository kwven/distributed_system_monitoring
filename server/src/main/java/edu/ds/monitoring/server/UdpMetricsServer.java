package edu.ds.monitoring.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

import edu.ds.monitoring.common.dto.AgentSnapshot;
import edu.ds.monitoring.common.dto.MetricSample;
import edu.ds.monitoring.common.dto.ThresholdConfig;
import edu.ds.monitoring.server.NetConfig;

public class UdpMetricsServer implements Runnable {

    private final AgentStore store;
    private final MetricStore metricStore;
    private final ThresholdStore thresholdStore;
    private final int port;
    private volatile boolean running = true;
    private DatagramSocket socket;

    public UdpMetricsServer(int port, AgentStore store, MetricStore metricStore, ThresholdStore thresholdStore ) {
        this.port = port;
        this.store = store;
        this.metricStore = metricStore;
        this.thresholdStore = thresholdStore;
    }

    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close(); // unblocks receive()
        }
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(port);
            System.out.println("[UDP] Listening on port " + port);

            byte[] buffer = new byte[NetConfig.UDP_BUFFER_SIZE];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // BLOQUANT

                String msg = new String(
                        packet.getData(),
                        packet.getOffset(),
                        packet.getLength(),
                        StandardCharsets.UTF_8
                );
                MetricSample metric = SnapshotParser.metricFromJson(msg);
                if (metric != null) {
                    metricStore.add(metric);
                }
                ThresholdConfig th = thresholdStore.get(metric != null ? metric.agentId : null);
                AgentSnapshot snapshot = SnapshotParser.fromJson(msg, th);
                if (snapshot == null) {
                    System.err.println("[UDP] Invalid snapshot -> " + msg);
                    continue;
                        }

                store.update(snapshot);

                System.out.println("[UDP] Updated agent=" + snapshot.agentId
                       + " cpu=" + snapshot.cpuPct
                       + " ram=" + snapshot.ramPct
                       + " disk=" + snapshot.diskPct
                       + " status=" + snapshot.status);

               }
 
        } catch (SocketException e) {
            if (running) {
                System.err.println("[UDP] Socket error: " + e.getMessage());
            } else {
                System.out.println("[UDP] Stopped.");
            }
        } catch (IOException e) {
            System.err.println("[UDP] IO error: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
}
