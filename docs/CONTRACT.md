# CONTRACT v1 — Distributed Monitoring System

## Global rules
- Encoding: UTF-8
- Timestamp: epoch millis (long)
- agentId: string unique (ex: "agent-01")
- Metrics interval: 2s
- UI polling interval: 1s
- Agent OFFLINE if (now - lastSeenTs) > 6000 ms

## Network ports
- UDP metrics: 9999
- TCP alerts: 9998
- RMI registry: 1099

---

## UDP Metrics (Agent -> Server)
Transport: UDP datagram
Payload: JSON (1 datagram = 1 MetricSample)

### Fields
schema: int (always 1)
agentId: string
host: string
ip: string
ts: long (epoch ms)
cpuPct: double (0..100)
ramUsedBytes: long
ramTotalBytes: long
diskUsedBytes: long
diskTotalBytes: long

### Example
{
  "schema": 1,
  "agentId": "agent-01",
  "host": "pc-lab-01",
  "ip": "192.168.1.10",
  "ts": 1730000000000,
  "cpuPct": 42.5,
  "ramUsedBytes": 2147483648,
  "ramTotalBytes": 8589934592,
  "diskUsedBytes": 12884901888,
  "diskTotalBytes": 256000000000
}

---

## TCP Alerts (Agent -> Server)
Transport: TCP
Payload: NDJSON (1 line = 1 AlertEvent JSON)

### Fields
schema: int (always 1)
agentId: string
ts: long (epoch ms)
severity: "WARN" | "CRITICAL"
metric: "CPU" | "RAM" | "DISK"
value: double
threshold: double
message: string

### Example line
{"schema":1,"agentId":"agent-01","ts":1730000000123,"severity":"CRITICAL","metric":"CPU","value":95.2,"threshold":90.0,"message":"CPU above threshold"}

---

## Thresholds (Server side rules)
ThresholdConfig fields (percent):
cpuWarnPct: 70
cpuCritPct: 90
ramWarnPct: 75
ramCritPct: 90
diskWarnPct: 80
diskCritPct: 95

Server computes:
ramPct = 100 * ramUsedBytes / ramTotalBytes
diskPct = 100 * diskUsedBytes / diskTotalBytes

Alert triggering:
- WARN if metricPct >= warnPct
- CRITICAL if metricPct >= critPct
