package edu.ds.monitoring.common.api;

import edu.ds.monitoring.common.dto.*;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface MonitoringService extends Remote {

  List<AgentSnapshot> getAgentsSnapshot() throws RemoteException;

  List<AlertEvent> getAlertsSince(long sinceTs) throws RemoteException;

  List<MetricSample> getMetrics(String agentId, long fromTs, long toTs) throws RemoteException;

  ThresholdConfig getThresholds(String agentId) throws RemoteException;

  void setThresholds(String agentId, ThresholdConfig cfg) throws RemoteException;
}
