package edu.ds.monitoring.server;

import java.util.concurrent.ConcurrentHashMap;

public class AlertStore {
    private final ConcurrentHashMap<String, String> lastAlertByAgent = new ConcurrentHashMap<>();

    public void update(String agentId, String rawJson) {
        if (agentId == null) agentId = "UNKNOWN";
        lastAlertByAgent.put(agentId, rawJson);
    }

    public ConcurrentHashMap<String, String> all() {
        return lastAlertByAgent;
    }
}
