package edu.ds.monitoring.agent;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.io.File;

public class SystemCollector {

    private final OperatingSystemMXBean osBean;

    // ================== CONSTRUCTEUR ==================
    public SystemCollector() {
        this.osBean = (OperatingSystemMXBean)
            ManagementFactory.getOperatingSystemMXBean();
    }

    // ================== STRUCTURE DES METRIQUES ==================
    public static class SystemMetrics {
        public double cpu;
        public long ramUsed;
        public long ramTotal;
        public long diskUsed;
        public long diskTotal;
    }

    // ================== COLLECTE DES METRIQUES ==================
    public SystemMetrics getSystemMetrics() {

        SystemMetrics metrics = new SystemMetrics();

        // -------- CPU --------
        metrics.cpu = osBean.getSystemCpuLoad() * 100;

        // -------- RAM --------
        long totalMemory = osBean.getTotalPhysicalMemorySize();
        long freeMemory = osBean.getFreePhysicalMemorySize();

        metrics.ramTotal = totalMemory;
        metrics.ramUsed = totalMemory - freeMemory;

        // -------- DISQUE --------
        File root = new File("/");

        metrics.diskTotal = root.getTotalSpace();
        metrics.diskUsed = metrics.diskTotal - root.getFreeSpace();

        return metrics;
    }
}
