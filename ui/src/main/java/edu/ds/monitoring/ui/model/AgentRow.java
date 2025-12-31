package edu.ds.monitoring.ui.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import edu.ds.monitoring.common.dto.AgentSnapshot;
import edu.ds.monitoring.common.dto.AgentStatus;
import javafx.beans.property.*;

public class AgentRow {
  private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

  private final StringProperty agentId = new SimpleStringProperty();
  private final StringProperty host = new SimpleStringProperty();
  private final StringProperty ip = new SimpleStringProperty();
  private final DoubleProperty cpuPct = new SimpleDoubleProperty();
  private final DoubleProperty ramPct = new SimpleDoubleProperty();
  private final DoubleProperty diskPct = new SimpleDoubleProperty();
  private final LongProperty lastSeenTs = new SimpleLongProperty();
  private final StringProperty lastSeenAgo = new SimpleStringProperty();
  private final ObjectProperty<AgentStatus> status = new SimpleObjectProperty<>();

  public static AgentRow fromSnapshot(AgentSnapshot s) {
    AgentRow r = new AgentRow();
    r.agentId.set(s.agentId);
    r.host.set(s.host);
    r.ip.set(s.ip);
    r.cpuPct.set(s.cpuPct);
    r.ramPct.set(s.ramPct);
    r.diskPct.set(s.diskPct);
    r.lastSeenTs.set(s.lastSeenTs);
    long now = System.currentTimeMillis();
    long diffSec = Math.max(0, (now - s.lastSeenTs) / 1000);
    if (diffSec < 90) {
      r.lastSeenAgo.set(diffSec + "s ago");
    } else {
      r.lastSeenAgo.set(TIME_FMT.format(Instant.ofEpochMilli(s.lastSeenTs).atZone(ZoneId.systemDefault())));
    }
    r.status.set(s.status);
    return r;
  }

  public StringProperty agentIdProperty() { return agentId; }
  public StringProperty hostProperty() { return host; }
  public StringProperty ipProperty() { return ip; }
  public DoubleProperty cpuPctProperty() { return cpuPct; }
  public DoubleProperty ramPctProperty() { return ramPct; }
  public DoubleProperty diskPctProperty() { return diskPct; }
  public LongProperty lastSeenTsProperty() { return lastSeenTs; }
  public StringProperty lastSeenAgoProperty() { return lastSeenAgo; }
  public ObjectProperty<AgentStatus> statusProperty() { return status; }

  public String getAgentId() { return agentId.get(); }
  public AgentStatus getStatus() { return status.get(); }
  public long getLastSeenTs() { return lastSeenTs.get(); }
}
