package edu.ds.monitoring.server;

import edu.ds.monitoring.common.api.MonitoringService;
import edu.ds.monitoring.common.dto.AlertEvent;
import edu.ds.monitoring.common.dto.AgentSnapshot;
import edu.ds.monitoring.common.dto.MetricSample;
import edu.ds.monitoring.common.dto.ThresholdConfig;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RmiMonitoringServiceImpl extends UnicastRemoteObject implements MonitoringService {

    private final AgentStore agentStore;
    private final MetricStore metricStore;
    private final AlertStore alertStore;

    private final ThresholdStore thresholdStore;

    public RmiMonitoringServiceImpl(AgentStore agentStore, MetricStore metricStore, AlertStore alertStore, ThresholdStore thresholdStore) throws RemoteException {
        super();
        this.agentStore = agentStore;
        this.metricStore = metricStore;
        this.alertStore = alertStore;
        this.thresholdStore = thresholdStore;
    }

    @Override
    public List<AgentSnapshot> getAgentsSnapshot() throws RemoteException {
        return new ArrayList<>(agentStore.all().values());
    }

    @Override
    public List<AlertEvent> getAlertsSince(long sinceTs) throws RemoteException {
        if (alertStore == null) return Collections.emptyList();
        return alertStore.getSince(sinceTs);
    }

    @Override
    public List<MetricSample> getMetrics(String agentId, long fromTs, long toTs) throws RemoteException {
        if (metricStore == null) return Collections.emptyList();
        return metricStore.getMetrics(agentId, fromTs, toTs);
    }

    @Override
    public ThresholdConfig getThresholds(String agentId) throws RemoteException {
        return thresholdStore.get(agentId);
    }

    @Override
    public void setThresholds(String agentId, ThresholdConfig cfg) throws RemoteException {
        thresholdStore.set(agentId, cfg);
    }

}
