package edu.ds.monitoring.common.dto;

import java.io.Serializable;

public class AgentSnapshot implements Serializable {
  private static final long serialVersionUID = 1L;

  public String agentId;
  public String host;
  public String ip;

  public long lastSeenTs;     // epoch millis
  public AgentStatus status;  // OK/WARN/CRITICAL/OFFLINE

  // Percent metrics prepared by server (simplifies UI)
  public double cpuPct;
  public double ramPct;
  public double diskPct;

  public AgentSnapshot() {}
}
