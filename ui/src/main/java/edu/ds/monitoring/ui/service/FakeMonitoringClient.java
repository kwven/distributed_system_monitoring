package edu.ds.monitoring.ui.service;

import edu.ds.monitoring.common.dto.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class FakeMonitoringClient implements MonitoringClient {

  private final Map<String, Long> lastTs = new HashMap<>();
  private long alertTs = System.currentTimeMillis();

  @Override
  public List<AgentSnapshot> getAgentsSnapshot() {
    long now = System.currentTimeMillis();
    List<AgentSnapshot> list = new ArrayList<>();

    for (int i = 1; i <= 5; i++) {
      String id = "agent-0" + i;

      AgentSnapshot s = new AgentSnapshot();
      s.agentId = id;
      s.host = "pc-lab-0" + i;
      s.ip = "192.168.1." + (10 + i);
      s.lastSeenTs = now;

      s.cpuPct = rand(5, 95);
      s.ramPct = rand(10, 90);
      s.diskPct = rand(20, 98);

      if (s.cpuPct >= 90 || s.ramPct >= 90 || s.diskPct >= 95) s.status = AgentStatus.CRITICAL;
      else if (s.cpuPct >= 70 || s.ramPct >= 75 || s.diskPct >= 80) s.status = AgentStatus.WARN;
      else s.status = AgentStatus.OK;

      list.add(s);
      lastTs.put(id, now);
    }
    return list;
  }

  @Override
  public List<AlertEvent> getAlertsSince(long sinceTs) {
    long now = System.currentTimeMillis();
    List<AlertEvent> out = new ArrayList<>();
    if (now - alertTs > 3000) {
      alertTs = now;

      AlertEvent a = new AlertEvent();
      a.agentId = "agent-01";
      a.ts = now;
      a.metric = MetricType.CPU;
      a.severity = Severity.CRITICAL;
      a.value = rand(90, 99);
      a.threshold = 90;
      a.message = "CPU above threshold";
      out.add(a);
    }
    return out;
  }

  @Override
  public List<MetricSample> getMetrics(String agentId, long fromTs, long toTs) {
    List<MetricSample> list = new ArrayList<>();
    long t = Math.max(fromTs, System.currentTimeMillis() - 60_000);
    while (t <= toTs) {
      MetricSample m = new MetricSample();
      m.agentId = agentId;
      m.ts = t;
      m.cpuPct = rand(5, 95);
      m.ramUsedBytes = (long) (rand(2, 7) * 1024 * 1024 * 1024L);
      m.ramTotalBytes = 8L * 1024 * 1024 * 1024;
      m.diskUsedBytes = (long) (rand(50, 220) * 1024 * 1024 * 1024L);
      m.diskTotalBytes = 256L * 1024 * 1024 * 1024;
      list.add(m);
      t += 1000;
    }
    return list;
  }

  @Override
  public ThresholdConfig getThresholds(String agentId) {
    return new ThresholdConfig();
  }

  @Override
  public void setThresholds(String agentId, ThresholdConfig cfg) {
    // no-op
  }

  private double rand(double min, double max) {
    return ThreadLocalRandom.current().nextDouble(min, max);
  }
}
