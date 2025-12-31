package edu.ds.monitoring.common.dto;

import java.io.Serializable;

public class AlertEvent implements Serializable {
  private static final long serialVersionUID = 1L;

  public int schema = 1;

  public String agentId;
  public long ts; // epoch millis

  public Severity severity;   // WARN | CRITICAL
  public MetricType metric;   // CPU | RAM | DISK

  public double value;
  public double threshold;
  public String message;

  public AlertEvent() {}
}
