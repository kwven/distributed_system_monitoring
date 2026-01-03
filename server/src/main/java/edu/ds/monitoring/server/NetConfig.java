package edu.ds.monitoring.server;

public final class NetConfig {
    private NetConfig() {}

    public static final int UDP_PORT = 9999;
    public static final int UDP_BUFFER_SIZE = 8192;
    public static final long OFFLINE_TIMEOUT_MS = 15_000; // 15s (à ajuster)
    public static final long OFFLINE_CHECK_PERIOD_MS = 2_000; // check toutes les 2s
    public static final int TCP_ALERT_PORT = 9998;
    public static final int REST_PORT = 8080;


}
