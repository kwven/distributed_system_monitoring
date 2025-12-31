package edu.ds.monitoring.ui.service;

import edu.ds.monitoring.common.dto.*;
import java.util.List;

public interface MonitoringClient {
  List<AgentSnapshot> getAgentsSnapshot() throws Exception;
  List<AlertEvent> getAlertsSince(long sinceTs) throws Exception;
  List<MetricSample> getMetrics(String agentId, long fromTs, long toTs) throws Exception;

  ThresholdConfig getThresholds(String agentId) throws Exception;
  void setThresholds(String agentId, ThresholdConfig cfg) throws Exception;
}
