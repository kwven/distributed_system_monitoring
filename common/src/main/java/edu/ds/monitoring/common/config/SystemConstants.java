package edu.ds.monitoring.common.config;

public final class SystemConstants {
  private SystemConstants() {}

  public static final int UDP_METRICS_PORT = 9999;
  public static final int TCP_ALERTS_PORT = 9998;
  public static final int RMI_REGISTRY_PORT = 1099;

  public static final long AGENT_SEND_INTERVAL_MS = 2000;
  public static final long UI_POLL_INTERVAL_MS = 1000;
  public static final long OFFLINE_TIMEOUT_MS = 6000;

  public static final int SCHEMA_VERSION = 1;

  public static final String RMI_BINDING_NAME = "MonitoringService";
}
