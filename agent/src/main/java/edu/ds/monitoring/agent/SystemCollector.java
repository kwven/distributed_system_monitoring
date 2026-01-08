package edu.ds.monitoring.agent;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Retourne maintenant les valeurs en octets, pas en pourcentages
 */
public class SystemCollector {

    public static class SystemMetrics {
        public final double cpu;           // en pourcentage (0-100)
        public final long ramUsed;         // en octets
        public final long ramTotal;        // en octets
        public final long diskUsed;        // en octets
        public final long diskTotal;       // en octets

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

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.matches("\\d+")) {
                    return Double.parseDouble(line);
                }
            }
            reader.close();
        } catch (Exception e) {
            // Erreur silencieuse
        }
        return Math.random() * 50; // Fallback
    }

    private long getRamTotalBytes() {
        try {
            // wmic retourne la mémoire en octets
            Process process = Runtime.getRuntime().exec("wmic ComputerSystem get TotalPhysicalMemory");
            process.waitFor();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.matches("\\d+")) {
                    return Long.parseLong(line);
                }
            }
            reader.close();
        } catch (Exception e) {
            // Fallback: 8GB par défaut
            return 8L * 1024 * 1024 * 1024;
        }
        return 8L * 1024 * 1024 * 1024; // 8GB par défaut
    }

    private long getRamUsedBytes() {
        try {
            long total = getRamTotalBytes();

            // Mémoire libre en octets
            Process process = Runtime.getRuntime().exec("wmic OS get FreePhysicalMemory");
            process.waitFor();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.matches("\\d+")) {
                    long freeKB = Long.parseLong(line);  // en KB
                    long freeBytes = freeKB * 1024;       // convert en octets
                    return total - freeBytes;
                }
            }
            reader.close();
        } catch (Exception e) {
            // Fallback: 50% utilisé
            long total = getRamTotalBytes();
            return total / 2;
        }
        long total = getRamTotalBytes();
        return total / 2;
    }

    private long getDiskTotalBytes() {
        try {
            java.io.File disk = new java.io.File("C:");
            return disk.getTotalSpace();
        } catch (Exception e) {
            // Fallback: 256GB
            return 256L * 1024 * 1024 * 1024;
        }
    }

    private long getDiskUsedBytes() {
        try {
            java.io.File disk = new java.io.File("C:");
            long total = disk.getTotalSpace();
            long free = disk.getFreeSpace();
            return total - free;
        } catch (Exception e) {
            // Fallback: 70% utilisé
            long total = getDiskTotalBytes();
            return (long)(total * 0.7);
        }
    }
}
