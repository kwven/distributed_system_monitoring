package edu.ds.monitoring.common.dto;

import java.io.Serializable;

public class ThresholdConfig implements Serializable {
  private static final long serialVersionUID = 1L;

  // Percent thresholds
  public double cpuWarnPct = 70;
  public double cpuCritPct = 90;

  public double ramWarnPct = 75;
  public double ramCritPct = 90;

  public double diskWarnPct = 80;
  public double diskCritPct = 95;

  public ThresholdConfig() {}
}
