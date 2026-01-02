package edu.ds.monitoring.server;

import edu.ds.monitoring.common.api.MonitoringService;
import edu.ds.monitoring.common.dto.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RmiMonitoringServiceImpl extends UnicastRemoteObject implements MonitoringService {

    private final AgentStore agentStore;

    // seuils par agent (minimal)
    private final ConcurrentHashMap<String, ThresholdConfig> thresholdsByAgent = new ConcurrentHashMap<>();

    public RmiMonitoringServiceImpl(AgentStore agentStore) throws RemoteException {
        super();
        this.agentStore = agentStore;
    }

    @Override
    public List<AgentSnapshot> getAgentsSnapshot() throws RemoteException {
        return new ArrayList<>(agentStore.all().values());
    }

    @Override
    public List<AlertEvent> getAlertsSince(long sinceTs) throws RemoteException {
        // Minimal nécessaire: si tu n’as pas encore un AlertStore<List<AlertEvent>>, renvoie vide.
        return Collections.emptyList();
    }

    @Override
    public List<MetricSample> getMetrics(String agentId, long fromTs, long toTs) throws RemoteException {
        // Minimal nécessaire: si tu n’as pas encore un historique metrics, renvoie vide.
        return Collections.emptyList();
    }

    @Override
    public ThresholdConfig getThresholds(String agentId) throws RemoteException {
        // Retourner les seuils existants, sinon default
        return thresholdsByAgent.computeIfAbsent(agentId, id -> defaultThresholds());
    }

    @Override
    public void setThresholds(String agentId, ThresholdConfig cfg) throws RemoteException {
        if (agentId == null || cfg == null) return;
        thresholdsByAgent.put(agentId, cfg);
    }

    private ThresholdConfig defaultThresholds() {
        ThresholdConfig th = new ThresholdConfig();
        th.cpuWarnPct = 70;
        th.cpuCritPct = 90;
        th.ramWarnPct = 75;
        th.ramCritPct = 90;
        th.diskWarnPct = 80;
        th.diskCritPct = 95;
        return th;
    }
}
