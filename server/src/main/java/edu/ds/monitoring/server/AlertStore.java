package edu.ds.monitoring.server;

import edu.ds.monitoring.common.dto.AlertEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class AlertStore {
    private final Deque<AlertEvent> alerts = new ArrayDeque<>();
    private final int maxSize;

    public AlertStore() {
        this(1000);
    }

    public AlertStore(int maxSize) {
        this.maxSize = maxSize;
    }

    public void add(AlertEvent alert) {
        if (alert == null) return;
        synchronized (alerts) {
            alerts.addLast(alert);
            while (alerts.size() > maxSize) {
                alerts.removeFirst();
            }
        }
    }

    public List<AlertEvent> getSince(long sinceTs) {
        List<AlertEvent> out = new ArrayList<>();
        synchronized (alerts) {
            for (AlertEvent a : alerts) {
                if (a.ts >= sinceTs) out.add(a);
            }
        }
        return out;
    }
}
