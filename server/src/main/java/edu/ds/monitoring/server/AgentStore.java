package edu.ds.monitoring.server;

import edu.ds.monitoring.common.dto.AgentSnapshot;
import java.util.concurrent.ConcurrentHashMap;

public class AgentStore {
    private final ConcurrentHashMap<String, AgentSnapshot> latestByAgent = new ConcurrentHashMap<>();

    public void update(AgentSnapshot snapshot) {
        if (snapshot == null || snapshot.agentId == null) return;
        latestByAgent.put(snapshot.agentId, snapshot);
    }

    public AgentSnapshot getLatest(String agentId) {
        return latestByAgent.get(agentId);
    }

    public ConcurrentHashMap<String, AgentSnapshot> all() {
        return latestByAgent;
    }
}

