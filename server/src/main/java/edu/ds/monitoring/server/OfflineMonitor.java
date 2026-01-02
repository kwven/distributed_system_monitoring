package edu.ds.monitoring.server;

import edu.ds.monitoring.common.dto.AgentSnapshot;
import edu.ds.monitoring.common.dto.AgentStatus;

public class OfflineMonitor implements Runnable {
    private final AgentStore store;
    private final long timeoutMs;
    private final long periodMs;
    private volatile boolean running = true;

    public OfflineMonitor(AgentStore store, long timeoutMs, long periodMs) {
        this.store = store;
        this.timeoutMs = timeoutMs;
        this.periodMs = periodMs;
    }

    public void stop() { running = false; }

    @Override
    public void run() {
        while (running) {
            long now = System.currentTimeMillis();

            store.all().forEach((id, s) -> {
                long age = now - s.lastSeenTs;
                if (age > timeoutMs && s.status != AgentStatus.OFFLINE) {
                    s.status = AgentStatus.OFFLINE; // on modifie l'objet stocké
                    System.out.println("[OFFLINE] " + id + " (lastSeen " + age + " ms ago)");
                }
            });

            try {
                Thread.sleep(periodMs);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
