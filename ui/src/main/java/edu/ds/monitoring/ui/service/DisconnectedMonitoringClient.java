package edu.ds.monitoring.ui.service;

import edu.ds.monitoring.common.dto.AlertEvent;
import edu.ds.monitoring.common.dto.AgentSnapshot;
import edu.ds.monitoring.common.dto.MetricSample;
import edu.ds.monitoring.common.dto.ThresholdConfig;

import java.util.List;

public class DisconnectedMonitoringClient implements MonitoringClient {

  @Override
  public List<AgentSnapshot> getAgentsSnapshot() {
    return List.of();
  }

  @Override
  public List<AlertEvent> getAlertsSince(long sinceTs) {
    return List.of();
  }

  @Override
  public List<MetricSample> getMetrics(String agentId, long fromTs, long toTs) {
    return List.of();
  }

  @Override
  public ThresholdConfig getThresholds(String agentId) {
    ThresholdConfig cfg = new ThresholdConfig();
    cfg.cpuWarnPct = 70;
    cfg.cpuCritPct = 90;
    cfg.ramWarnPct = 75;
    cfg.ramCritPct = 90;
    cfg.diskWarnPct = 80;
    cfg.diskCritPct = 95;
    return cfg;
  }

  @Override
  public void setThresholds(String agentId, ThresholdConfig cfg) {
    // no-op
  }
}
