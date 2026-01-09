package edu.ds.monitoring.agent;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SystemCollector {

    public static class SystemMetrics {
        public final double cpu;
        public final long ramUsed;
        public final long ramTotal;
        public final long diskUsed;
        public final long diskTotal;

        public SystemMetrics(double cpu, long ramUsed, long ramTotal, long diskUsed, long diskTotal) {
            this.cpu = cpu;
            this.ramUsed = ramUsed;
            this.ramTotal = ramTotal;
            this.diskUsed = diskUsed;
            this.diskTotal = diskTotal;
        }
    }

    public SystemMetrics getSystemMetrics() {
        return new SystemMetrics(
            getCpuPercentage(),
            getRamUsedBytes(),
            getRamTotalBytes(),
            getDiskUsedBytes(),
            getDiskTotalBytes()
        );
    }

    private double getCpuPercentage() {
        try {
            Process process = Runtime.getRuntime().exec("wmic cpu get loadpercentage");
            process.waitFor();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.matches("\\d+")) return Double.parseDouble(line);
                }
            }
        } catch (Exception e) {}
        return Math.random() * 50;
    }

    private long getRamTotalBytes() {
        try {
            Process process = Runtime.getRuntime().exec("wmic ComputerSystem get TotalPhysicalMemory");
            process.waitFor();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.matches("\\d+")) return Long.parseLong(line);
                }
            }
        } catch (Exception e) {}
        return 8L * 1024 * 1024 * 1024;
    }

    private long getRamUsedBytes() {
        try {
            long total = getRamTotalBytes();
            Process process = Runtime.getRuntime().exec("wmic OS get FreePhysicalMemory");
            process.waitFor();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.matches("\\d+")) {
                        return total - (Long.parseLong(line) * 1024);
                    }
                }
            }
        } catch (Exception e) {}
        return getRamTotalBytes() / 2;
    }

    private long getDiskTotalBytes() {
        try {
            return new java.io.File("C:").getTotalSpace();
        } catch (Exception e) {
            return 256L * 1024 * 1024 * 1024;
        }
    }

    private long getDiskUsedBytes() {
        try {
            java.io.File disk = new java.io.File("C:");
            return disk.getTotalSpace() - disk.getFreeSpace();
        } catch (Exception e) {
            return (long)(getDiskTotalBytes() * 0.7);
        }
    }
}
