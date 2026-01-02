package edu.ds.monitoring.server;

import edu.ds.monitoring.common.dto.AgentSnapshot;
import edu.ds.monitoring.common.dto.AgentStatus;
import edu.ds.monitoring.common.dto.ThresholdConfig;
import edu.ds.monitoring.server.SimpleJson;

public final class SnapshotParser {
    private SnapshotParser() {}

    public static AgentSnapshot fromJson(String json, ThresholdConfig th) {
    String agentId = SimpleJson.getString(json, "agentId");
    if (agentId == null) return null;

    String host = SimpleJson.getString(json, "host");
    String ip = SimpleJson.getString(json, "ip");
    Long ts = SimpleJson.getLong(json, "ts"); // ✅ l’agent envoie "ts"

    Double cpuPct = SimpleJson.getDouble(json, "cpuPct");
    Long ramUsed = SimpleJson.getLong(json, "ramUsedBytes");
    Long ramTotal = SimpleJson.getLong(json, "ramTotalBytes");
    Long diskUsed = SimpleJson.getLong(json, "diskUsedBytes");
    Long diskTotal = SimpleJson.getLong(json, "diskTotalBytes");

    AgentSnapshot s = new AgentSnapshot();
    s.agentId = agentId;
    s.host = host;
    s.ip = ip;

    // ✅ lastSeenTs doit refléter le heartbeat de l’agent si possible
    s.lastSeenTs = (ts != null) ? ts : System.currentTimeMillis();

    s.cpuPct = cpuPct != null ? cpuPct : 0.0;
    s.ramPct = pct(ramUsed, ramTotal);
    s.diskPct = pct(diskUsed, diskTotal);

    s.status = computeStatus(s, th);
    return s;
}


    private static double pct(Long used, Long total) {
        if (used == null || total == null || total <= 0) return 0.0;
        return (100.0 * used) / total;
    }

    private static AgentStatus computeStatus(AgentSnapshot s, ThresholdConfig th) {
        if (th == null) return AgentStatus.OK;

        boolean crit =
                s.cpuPct >= th.cpuCritPct ||
                s.ramPct >= th.ramCritPct ||
                s.diskPct >= th.diskCritPct;

        if (crit) return AgentStatus.CRITICAL;

        boolean warn =
                s.cpuPct >= th.cpuWarnPct ||
                s.ramPct >= th.ramWarnPct ||
                s.diskPct >= th.diskWarnPct;

        if (warn) return AgentStatus.WARN;

        return AgentStatus.OK;
    }
}
