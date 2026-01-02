package edu.ds.monitoring.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

import edu.ds.monitoring.common.dto.AgentSnapshot;
import edu.ds.monitoring.common.dto.ThresholdConfig;
import edu.ds.monitoring.server.NetConfig;
import edu.ds.monitoring.server.SimpleJson;

public class UdpMetricsServer implements Runnable {

    private final AgentStore store;
    private final ThresholdConfig thresholds;
    private final int port;
    private volatile boolean running = true;
    private DatagramSocket socket;

    public UdpMetricsServer(int port, AgentStore store, ThresholdConfig thresholds ) {
        this.port = port;
        this.store = store;
        this.thresholds = thresholds;
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
                AgentSnapshot snapshot = SnapshotParser.fromJson(msg, thresholds);
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
