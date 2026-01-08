package edu.ds.monitoring.ui.service;

import edu.ds.monitoring.common.api.MonitoringService;
import edu.ds.monitoring.common.config.SystemConstants;
import edu.ds.monitoring.common.dto.*;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

public class RmiMonitoringClient implements MonitoringClient {

  private final String host;
  private final int port;
  private final String bindingName;

  private volatile MonitoringService stub;

  public RmiMonitoringClient(String host) {
    this(host, SystemConstants.RMI_REGISTRY_PORT, "MonitoringService");
  }

  public RmiMonitoringClient(String host, int port, String bindingName) {
    this.host = host;
    this.port = port;
    this.bindingName = bindingName;
  }

  private MonitoringService getStub() throws Exception {
    if (stub != null) return stub;
    Registry registry = LocateRegistry.getRegistry(host, port);
    stub = (MonitoringService) registry.lookup(bindingName);
    return stub;
  }

  @Override
  public List<AgentSnapshot> getAgentsSnapshot() throws Exception {
    return getStub().getAgentsSnapshot();
  }

  @Override
  public List<AlertEvent> getAlertsSince(long sinceTs) throws Exception {
    return getStub().getAlertsSince(sinceTs);
  }

  @Override
  public List<MetricSample> getMetrics(String agentId, long fromTs, long toTs) throws Exception {
    return getStub().getMetrics(agentId, fromTs, toTs);
  }

  @Override
  public ThresholdConfig getThresholds(String agentId) throws Exception {
    return getStub().getThresholds(agentId);
  }

  @Override
  public void setThresholds(String agentId, ThresholdConfig cfg) throws Exception {
    getStub().setThresholds(agentId, cfg);
  }
}
