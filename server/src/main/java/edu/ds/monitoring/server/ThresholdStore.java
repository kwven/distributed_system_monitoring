package edu.ds.monitoring.server;

import edu.ds.monitoring.common.dto.ThresholdConfig;
import java.util.concurrent.ConcurrentHashMap;

public class ThresholdStore {
    private final ConcurrentHashMap<String, ThresholdConfig> byAgent = new ConcurrentHashMap<>();
    private final ThresholdConfig defaults;

    public ThresholdStore(ThresholdConfig defaults) {
        this.defaults = defaults;
    }

    public ThresholdConfig get(String agentId) {
        if (agentId == null) return defaults;
        return byAgent.getOrDefault(agentId, defaults);
    }

    public void set(String agentId, ThresholdConfig cfg) {
        if (agentId == null || cfg == null) return;
        byAgent.put(agentId, cfg);
    }

    public ThresholdConfig getDefaults() {
        return defaults;
    }
}
