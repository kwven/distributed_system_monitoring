package edu.ds.monitoring.agent;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.io.File;

public class SystemCollector {

    private final OperatingSystemMXBean osBean;

    public SystemCollector() {
        this.osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }

    public static class SystemMetrics {
        public double cpu;
        public long ramUsed;
        public long ramTotal;
        public long diskUsed;
        public long diskTotal;

        public SystemMetrics(double cpu, long ramUsed, long ramTotal, long diskUsed, long diskTotal) {
            this.cpu = cpu;
            this.ramUsed = ramUsed;
            this.ramTotal = ramTotal;
            this.diskUsed = diskUsed;
            this.diskTotal = diskTotal;
        }
    }

    public SystemMetrics getSystemMetrics() {
        double cpuLoad = osBean.getSystemCpuLoad() * 100.0;
        if (cpuLoad < 0) cpuLoad = 0.0;

        long totalRam = osBean.getTotalPhysicalMemorySize();
        long freeRam = osBean.getFreePhysicalMemorySize();
        long usedRam = totalRam - freeRam;

        File root = new File("/");
        long totalDisk = root.getTotalSpace();
        long freeDisk = root.getFreeSpace();
        long usedDisk = totalDisk - freeDisk;

        return new SystemMetrics(cpuLoad, usedRam, totalRam, usedDisk, totalDisk);
    }
}
