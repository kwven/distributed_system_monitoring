package edu.ds.monitoring.server;

import edu.ds.monitoring.common.dto.MetricSample;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MetricStore {
    private final ConcurrentHashMap<String, Deque<MetricSample>> byAgent = new ConcurrentHashMap<>();
    private final long retentionMs;
    private final int maxSamples;

    public MetricStore(long retentionMs, int maxSamples) {
        this.retentionMs = retentionMs;
        this.maxSamples = maxSamples;
    }

    public void add(MetricSample sample) {
        if (sample == null || sample.agentId == null) return;
        Deque<MetricSample> q = byAgent.computeIfAbsent(sample.agentId, id -> new ArrayDeque<>());
        synchronized (q) {
            q.addLast(sample);
            trim(q, sample.ts);
        }
    }

    public List<MetricSample> getMetrics(String agentId, long fromTs, long toTs) {
        if (agentId == null) return List.of();
        Deque<MetricSample> q = byAgent.get(agentId);
        if (q == null) return List.of();
        long from = fromTs;
        long to = toTs;
        if (to < from) {
            long tmp = from;
            from = to;
            to = tmp;
        }
        List<MetricSample> out = new ArrayList<>();
        synchronized (q) {
            for (MetricSample m : q) {
                long ts = m.ts;
                if (ts >= from && ts <= to) out.add(m);
            }
        }
        return out;
    }

    private void trim(Deque<MetricSample> q, long nowTs) {
        while (!q.isEmpty()) {
            MetricSample first = q.peekFirst();
            if (q.size() > maxSamples || (nowTs - first.ts) > retentionMs) {
                q.removeFirst();
            } else {
                break;
            }
        }
    }
}
