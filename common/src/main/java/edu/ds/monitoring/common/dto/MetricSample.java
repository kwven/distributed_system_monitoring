package edu.ds.monitoring.common.dto;

import java.io.Serializable;

public class MetricSample implements Serializable {
  private static final long serialVersionUID = 1L;

  public int schema = 1;

  public String agentId;
  public String host;
  public String ip;

  public long ts; // epoch millis

  public double cpuPct; // 0..100
  public long ramUsedBytes;
  public long ramTotalBytes;
  public long diskUsedBytes;
  public long diskTotalBytes;

  public MetricSample() {}
}
