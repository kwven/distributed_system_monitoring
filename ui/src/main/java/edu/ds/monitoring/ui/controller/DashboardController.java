package edu.ds.monitoring.ui.controller;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.Collectors;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import javafx.scene.chart.NumberAxis;
import org.controlsfx.control.Notifications;

import edu.ds.monitoring.common.config.SystemConstants;
import edu.ds.monitoring.common.dto.AgentSnapshot;
import edu.ds.monitoring.common.dto.AgentStatus;
import edu.ds.monitoring.common.dto.AlertEvent;
import edu.ds.monitoring.ui.model.AgentRow;
import edu.ds.monitoring.ui.controller.AgentDetailsController;
import edu.ds.monitoring.ui.service.FakeMonitoringClient;
import edu.ds.monitoring.ui.service.MonitoringClient;
import edu.ds.monitoring.ui.service.RmiMonitoringClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DashboardController {

  @FXML private TextField hostField;
  @FXML private Label connectionLabel;
  @FXML private Label connectionDot;
  @FXML private Label alertsLabel;
  @FXML private TextField searchField;
  @FXML private Label kpiOnline;
  @FXML private Label kpiWarn;
  @FXML private Label kpiCritical;
  @FXML private Label kpiOffline;
  @FXML private Label kpiAlerts;
  @FXML private Label kpiSelCpu;
  @FXML private Label kpiSelRam;
  @FXML private Label kpiSelDisk;

  @FXML private TableView<AgentRow> agentsTable;
  @FXML private TableColumn<AgentRow, String> colAgent;
  @FXML private TableColumn<AgentRow, String> colHost;
  @FXML private TableColumn<AgentRow, String> colIp;
  @FXML private TableColumn<AgentRow, String> colLastSeen;
  @FXML private TableColumn<AgentRow, Number> colCpu;
  @FXML private TableColumn<AgentRow, Number> colRam;
  @FXML private TableColumn<AgentRow, Number> colDisk;
  @FXML private TableColumn<AgentRow, AgentStatus> colStatus;

  @FXML private LineChart<Number, Number> cpuChart;
  @FXML private LineChart<Number, Number> ramChart;
  @FXML private LineChart<Number, Number> diskChart;
  @FXML private ComboBox<String> chartWindowCombo;
  @FXML private Label selectedAgentLabel;
  @FXML private ComboBox<String> alertsAgentFilter;
  @FXML private ComboBox<String> alertsSeverityFilter;
  @FXML private TableView<AlertEvent> recentAlertsTable;
  @FXML private TableColumn<AlertEvent, String> colAlertTs;
  @FXML private TableColumn<AlertEvent, String> colAlertAgent;
  @FXML private TableColumn<AlertEvent, String> colAlertSeverity;
  @FXML private TableColumn<AlertEvent, String> colAlertMetric;
  @FXML private TableColumn<AlertEvent, String> colAlertValue;
  @FXML private TableColumn<AlertEvent, String> colAlertThreshold;
  @FXML private TableColumn<AlertEvent, String> colAlertMessage;
  @FXML private Label recentAlertsCounter;
  @FXML private TextField cpuWarnField;
  @FXML private TextField cpuCritField;
  @FXML private TextField ramWarnField;
  @FXML private TextField ramCritField;
  @FXML private TextField diskWarnField;
  @FXML private TextField diskCritField;
  @FXML private Label thresholdErrorLabel;

  private final ObservableList<AgentRow> backingList = FXCollections.observableArrayList();
  private FilteredList<AgentRow> filtered;
  private ScheduledExecutorService scheduler;

  // Choix: commence avec Fake pour UI dev, puis switch RMI
  private MonitoringClient client = new FakeMonitoringClient();
  private final AtomicLong lastAlertsTs = new AtomicLong(0);

  private XYChart.Series<Number, Number> cpuSeries = new XYChart.Series<>();
  private XYChart.Series<Number, Number> ramSeries = new XYChart.Series<>();
  private XYChart.Series<Number, Number> diskSeries = new XYChart.Series<>();
  private XYChart.Series<Number, Number> cpuWarnSeries = new XYChart.Series<>();
  private XYChart.Series<Number, Number> cpuCritSeries = new XYChart.Series<>();
  private XYChart.Series<Number, Number> ramWarnSeries = new XYChart.Series<>();
  private XYChart.Series<Number, Number> ramCritSeries = new XYChart.Series<>();
  private XYChart.Series<Number, Number> diskWarnSeries = new XYChart.Series<>();
  private XYChart.Series<Number, Number> diskCritSeries = new XYChart.Series<>();

  private int chartX = 0;
  private int totalAlerts = 0;
  private String currentAgentId = null;
  private boolean suppressSelectionReset = false;
  private final Deque<Long> alertTimestamps = new ArrayDeque<>();
  private final ObservableList<AlertEvent> recentAlerts = FXCollections.observableArrayList();
  private FilteredList<AlertEvent> recentAlertsFiltered;
  private int chartWindowSizeSec = 60;
  private static final int DEF_CPU_WARN = 70;
  private static final int DEF_CPU_CRIT = 90;
  private static final int DEF_RAM_WARN = 75;
  private static final int DEF_RAM_CRIT = 90;
  private static final int DEF_DISK_WARN = 80;
  private static final int DEF_DISK_CRIT = 95;
  private int cpuWarn = DEF_CPU_WARN;
  private int cpuCrit = DEF_CPU_CRIT;
  private int ramWarn = DEF_RAM_WARN;
  private int ramCrit = DEF_RAM_CRIT;
  private int diskWarn = DEF_DISK_WARN;
  private int diskCrit = DEF_DISK_CRIT;
  private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
  private final Map<String, Long> highlightUntil = new HashMap<>();
  private final Preferences prefs = Preferences.userNodeForPackage(DashboardController.class);

  @FXML
  public void initialize() {
    hostField.setText("localhost");
    chartWindowCombo.getItems().addAll("Last 1 min", "Last 5 min", "Last 15 min");
    chartWindowCombo.getSelectionModel().select(0);
    chartWindowCombo.setOnAction(e -> {
      int idx = chartWindowCombo.getSelectionModel().getSelectedIndex();
      chartWindowSizeSec = switch (idx) {
        case 1 -> 300;
        case 2 -> 900;
        default -> 60;
      };
      boolean c1 = updateAxisWindow(cpuChart);
      boolean c2 = updateAxisWindow(ramChart);
      boolean c3 = updateAxisWindow(diskChart);
      if (c1 || c2 || c3) addThresholdLines();
    });
    loadThresholdsFromPrefs();
    populateThresholdFields();

    colAgent.setCellValueFactory(v -> v.getValue().agentIdProperty());
    colHost.setCellValueFactory(v -> v.getValue().hostProperty());
    colIp.setCellValueFactory(v -> v.getValue().ipProperty());
    colLastSeen.setCellValueFactory(v -> v.getValue().lastSeenAgoProperty());
    colCpu.setCellValueFactory(v -> v.getValue().cpuPctProperty());
    colRam.setCellValueFactory(v -> v.getValue().ramPctProperty());
    colDisk.setCellValueFactory(v -> v.getValue().diskPctProperty());
    colStatus.setCellValueFactory(v -> v.getValue().statusProperty());
    setupProgressColumn(colCpu);
    setupProgressColumn(colRam);
    setupProgressColumn(colDisk);
    setupStatusBadgeColumn(colStatus);
    setupLastSeenColumn(colLastSeen);

    recentAlertsFiltered = new FilteredList<>(recentAlerts, a -> true);
    recentAlertsTable.setItems(recentAlertsFiltered);
    setupAlertsTable();
    setupAlertFilters();

    filtered = new FilteredList<>(backingList, r -> true);
    SortedList<AgentRow> sorted = new SortedList<>(filtered);
    sorted.comparatorProperty().bind(agentsTable.comparatorProperty());
    agentsTable.setItems(sorted);

    searchField.textProperty().addListener((obs, oldV, newV) -> {
      String q = newV == null ? "" : newV.trim().toLowerCase();
      filtered.setPredicate(r -> q.isEmpty()
          || r.agentIdProperty().get().toLowerCase().contains(q)
          || r.hostProperty().get().toLowerCase().contains(q)
          || r.ipProperty().get().toLowerCase().contains(q));
    });

    // Row coloring by status
    agentsTable.setRowFactory(tv -> new TableRow<>() {
      @Override protected void updateItem(AgentRow item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null || item.getStatus() == null) {
          setStyle("");
          return;
        }
        long now = System.currentTimeMillis();
        Long hl = highlightUntil.get(item.getAgentId());
        if (hl != null && hl > now) {
          setStyle("-fx-background-color: rgba(255,64,64,0.32);");
          return;
        }
        switch (item.getStatus()) {
          case CRITICAL -> setStyle("-fx-background-color: rgba(255,0,0,0.18);");
          case WARN -> setStyle("-fx-background-color: rgba(255,165,0,0.18);");
          case OFFLINE -> setStyle("-fx-background-color: rgba(128,128,128,0.18);");
          default -> setStyle("");
        }
      }
      {
        setOnMouseClicked(ev -> {
          if (ev.getClickCount() == 2 && !isEmpty()) {
            openDetails(getItem());
          }
        });
      }
    });

    // Charts setup
    cpuChart.getData().add(cpuSeries);
    ramChart.getData().add(ramSeries);
    diskChart.getData().add(diskSeries);
    cpuChart.getData().addAll(cpuWarnSeries, cpuCritSeries);
    ramChart.getData().addAll(ramWarnSeries, ramCritSeries);
    diskChart.getData().addAll(diskWarnSeries, diskCritSeries);
    cpuSeries.setName("CPU %");
    ramSeries.setName("RAM %");
    diskSeries.setName("Disk %");
    cpuWarnSeries.setName("CPU Warn");
    cpuCritSeries.setName("CPU Crit");
    ramWarnSeries.setName("RAM Warn");
    ramCritSeries.setName("RAM Crit");
    diskWarnSeries.setName("Disk Warn");
    diskCritSeries.setName("Disk Crit");
    configureChartAxes(cpuChart);
    configureChartAxes(ramChart);
    configureChartAxes(diskChart);
    cpuChart.setTitle("CPU %");
    ramChart.setTitle("RAM %");
    diskChart.setTitle("Disk %");
    addThresholdLines();

    // Update charts when selection changes
    agentsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldR, newR) -> {
      if (newR == null) return; // ignore transient clears during refresh
      if (suppressSelectionReset) {
        suppressSelectionReset = false;
        currentAgentId = newR.getAgentId();
        updateSelectedKpis(newR);
        selectedAgentLabel.setText(newR.getAgentId());
        return; // skip reset when we re-select the same agent after refresh
      }
      String newId = newR.getAgentId();
      String oldId = oldR == null ? null : oldR.getAgentId();
      if (newId != null && newId.equals(oldId)) return; // same agent, keep history
      resetCharts();
      chartX = 0;
      currentAgentId = newId;
      addChartPoint(newR);
      updateSelectedKpis(newR);
      selectedAgentLabel.setText(newR.getAgentId());
    });

    startPolling();
  }

  @FXML
  public void onConnect() {
    String host = hostField.getText().trim();
    client = new RmiMonitoringClient(host);
    connectionLabel.setText("Connecting...");
    lastAlertsTs.set(System.currentTimeMillis());
  }

  @FXML
  public void onShowDetails() {
    AgentRow row = agentsTable.getSelectionModel().getSelectedItem();
    openDetails(row);
  }

  private void startPolling() {
    scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "ui-polling");
      t.setDaemon(true);
      return t;
    });

    scheduler.scheduleAtFixedRate(() -> {
      try {
        List<AgentSnapshot> agents = client.getAgentsSnapshot();
        List<AlertEvent> alerts = client.getAlertsSince(lastAlertsTs.get());

        long now = System.currentTimeMillis();
        lastAlertsTs.set(now);

        Platform.runLater(() -> {
      connectionLabel.setText("Connected (" + Instant.ofEpochMilli(now) + ")");
      connectionDot.getStyleClass().setAll("status-dot", "connected");
      updateAgents(agents);
      handleAlerts(alerts);
      updateChartsFromSelection();
        });

      } catch (Exception ex) {
        Platform.runLater(() -> {
          connectionLabel.setText("Disconnected");
          connectionDot.getStyleClass().setAll("status-dot", "disconnected");
        });
      }
    }, 0, SystemConstants.UI_POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
  }

  private void updateAgents(List<AgentSnapshot> agents) {
    AgentRow sel = agentsTable.getSelectionModel().getSelectedItem();
    final String selectedId = sel == null ? null : sel.getAgentId();

    backingList.setAll(new java.util.ArrayList<>(agents.stream().map(AgentRow::fromSnapshot).toList()));

    updateKpiCounts();

    if (!backingList.isEmpty()) {
      // Reselect same agent if still present, else fallback to first
      if (selectedId != null) {
        backingList.stream()
            .filter(r -> selectedId.equals(r.getAgentId()))
            .findFirst()
            .ifPresentOrElse(
                r -> {
                  suppressSelectionReset = true;
                  agentsTable.getSelectionModel().select(r);
                },
                () -> agentsTable.getSelectionModel().select(0));
      } else {
        agentsTable.getSelectionModel().select(0);
      }
    }
  }

  private void handleAlerts(List<AlertEvent> alerts) {
    if (alerts == null || alerts.isEmpty()) {
      alertsLabel.setText("0 new / " + totalAlerts + " total");
      updateAlertWindow();
      updateRecentAlertsCounter(0);
      return;
    }
    totalAlerts += alerts.size();
    for (AlertEvent a : alerts) {
      alertTimestamps.addLast(a.ts);
      if (a.severity == edu.ds.monitoring.common.dto.Severity.CRITICAL) {
        highlightUntil.put(a.agentId, System.currentTimeMillis() + 4000);
      }
    }
    updateAlertWindow();
    appendRecentAlerts(alerts);
    updateRecentAlertsCounter(alerts.size());
    alertsLabel.setText(alerts.size() + " new / " + totalAlerts + " total");
    for (AlertEvent a : alerts) {
      String title = a.severity + " - " + a.metric;
      String text = a.agentId + " : " + a.message + " (" + a.value + " >= " + a.threshold + ")";
      Notifications.create()
          .title(title)
          .text(text)
          .graphic(buildAlertBadge(a))
          .darkStyle()
          .showWarning();
    }
  }

  private void updateChartsFromSelection() {
    AgentRow selected = agentsTable.getSelectionModel().getSelectedItem();
    if (selected != null) {
      addChartPoint(selected);
      updateSelectedKpis(selected);
      selectedAgentLabel.setText(selected.getAgentId());
    }
  }

  private void openDetails(AgentRow row) {
    if (row == null) return;
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/agent_details.fxml"));
      Parent root = loader.load();
      AgentDetailsController ctrl = loader.getController();
      ctrl.setContext(client, row);
      Stage stage = new Stage();
      stage.setTitle("Agent Details - " + row.getAgentId());
      Scene scene = new Scene(root, 900, 700);
      scene.getStylesheets().add(getClass().getResource("/theme.css").toExternalForm());
      stage.setScene(scene);
      stage.show();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private void resetCharts() {
    cpuSeries.getData().clear();
    ramSeries.getData().clear();
    diskSeries.getData().clear();
    currentAgentId = null;
  }

  private void addChartPoint(AgentRow row) {
    // On garde max 60 points
    cpuSeries.getData().add(new XYChart.Data<>(chartX, row.cpuPctProperty().get()));
    ramSeries.getData().add(new XYChart.Data<>(chartX, row.ramPctProperty().get()));
    diskSeries.getData().add(new XYChart.Data<>(chartX, row.diskPctProperty().get()));
    chartX++;

    trimSeries(cpuSeries);
    trimSeries(ramSeries);
    trimSeries(diskSeries);
    boolean c1 = updateAxisWindow(cpuChart);
    boolean c2 = updateAxisWindow(ramChart);
    boolean c3 = updateAxisWindow(diskChart);
    if (c1 || c2 || c3) addThresholdLines();
    attachTooltip(cpuSeries);
    attachTooltip(ramSeries);
    attachTooltip(diskSeries);
  }

  private void trimSeries(XYChart.Series<Number, Number> s) {
    double cutoff = chartX - chartWindowSizeSec;
    s.getData().removeIf(d -> d.getXValue().doubleValue() < cutoff);
  }

  private void configureChartAxes(LineChart<Number, Number> chart) {
    NumberAxis x = (NumberAxis) chart.getXAxis();
    NumberAxis y = (NumberAxis) chart.getYAxis();
    x.setAutoRanging(false);
    x.setLowerBound(0);
    x.setUpperBound(chartWindowSizeSec);
    x.setTickUnit(tickUnitForWindow());
    y.setAutoRanging(false);
    y.setLowerBound(0);
    y.setUpperBound(100);
    y.setTickUnit(10);
  }

  private boolean updateAxisWindow(LineChart<Number, Number> chart) {
    NumberAxis x = (NumberAxis) chart.getXAxis();
    int window = chartWindowSizeSec;
    int lower = Math.max(0, chartX - window);
    double oldLower = x.getLowerBound();
    double oldUpper = x.getUpperBound();
    x.setLowerBound(lower);
    x.setUpperBound(lower + window);
    x.setTickUnit(tickUnitForWindow());
    return oldLower != x.getLowerBound() || oldUpper != x.getUpperBound();
  }

  private void setupPercentColumn(TableColumn<AgentRow, Number> col) {
    col.setCellFactory(c -> new TableCell<>() {
      @Override protected void updateItem(Number value, boolean empty) {
        super.updateItem(value, empty);
        if (empty || value == null) {
          setText("");
        } else {
          setText(String.format("%.1f%%", value.doubleValue()));
        }
      }
    });
  }

  private void setupProgressColumn(TableColumn<AgentRow, Number> col) {
    col.setCellFactory(c -> new TableCell<>() {
      private final ProgressBar bar = new ProgressBar();
      private final Label lbl = new Label();
      private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(6, bar, lbl);
      {
        bar.setPrefWidth(90);
        bar.setMinWidth(90);
        lbl.setStyle("-fx-text-fill: #e5e7eb;");
      }
      @Override protected void updateItem(Number value, boolean empty) {
        super.updateItem(value, empty);
        if (empty || value == null) {
          setGraphic(null);
        } else {
          double v = value.doubleValue();
          bar.setProgress(Math.max(0, Math.min(1, v / 100.0)));
          lbl.setText(String.format("%.1f%%", v));
          setGraphic(box);
        }
      }
    });
  }

  private void setupStatusBadgeColumn(TableColumn<AgentRow, AgentStatus> col) {
    col.setCellFactory(c -> new TableCell<>() {
      private final Label badge = new Label();
      @Override protected void updateItem(AgentStatus status, boolean empty) {
        super.updateItem(status, empty);
        if (empty || status == null) {
          setGraphic(null);
          return;
        }
        badge.getStyleClass().setAll("badge");
        switch (status) {
          case OK -> badge.getStyleClass().addAll("badge-ok");
          case WARN -> badge.getStyleClass().addAll("badge-warn");
          case CRITICAL -> badge.getStyleClass().addAll("badge-critical");
          case OFFLINE -> badge.getStyleClass().addAll("badge-offline");
        }
        badge.setText(status.name());
        setGraphic(badge);
      }
    });
  }

  private void setupLastSeenColumn(TableColumn<AgentRow, String> col) {
    col.setCellFactory(c -> new TableCell<>() {
      @Override protected void updateItem(String text, boolean empty) {
        super.updateItem(text, empty);
        if (empty || text == null) {
          setText(null);
          setStyle("");
          return;
        }
        AgentRow row = (AgentRow) getTableRow().getItem();
        setText(text);
        if (row != null) {
          long age = System.currentTimeMillis() - row.getLastSeenTs();
          if (age > SystemConstants.OFFLINE_TIMEOUT_MS) {
            setStyle("-fx-text-fill: #9ca3af;");
          } else {
            setStyle("-fx-text-fill: #e5e7eb;");
          }
        }
      }
    });
  }

  private void addThresholdLines() {
    refreshThresholdSeries(cpuWarnSeries, cpuWarn, cpuChart);
    refreshThresholdSeries(cpuCritSeries, cpuCrit, cpuChart);
    refreshThresholdSeries(ramWarnSeries, ramWarn, ramChart);
    refreshThresholdSeries(ramCritSeries, ramCrit, ramChart);
    refreshThresholdSeries(diskWarnSeries, diskWarn, diskChart);
    refreshThresholdSeries(diskCritSeries, diskCrit, diskChart);
  }

  private void refreshThresholdSeries(XYChart.Series<Number, Number> s, double value, LineChart<Number, Number> chart) {
    NumberAxis x = (NumberAxis) chart.getXAxis();
    double lower = x.getLowerBound();
    double upper = x.getUpperBound();
    s.getData().setAll(
        new XYChart.Data<>(lower, value),
        new XYChart.Data<>(upper, value)
    );
  }

  private void attachTooltip(XYChart.Series<Number, Number> series) {
    for (XYChart.Data<Number, Number> d : series.getData()) {
      if (d.getNode() != null) {
        Tooltip tp = new Tooltip(String.format("t=%ss\n%.1f%%", d.getXValue().intValue(), d.getYValue().doubleValue()));
        Tooltip.install(d.getNode(), tp);
      }
    }
  }

  private double tickUnitForWindow() {
    if (chartWindowSizeSec <= 60) return 10;
    if (chartWindowSizeSec <= 300) return 60;
    if (chartWindowSizeSec <= 900) return 180;
    return 300;
  }

  private void setupAlertsTable() {
    colAlertAgent.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().agentId));
    colAlertSeverity.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
        c.getValue().severity == null ? "" : c.getValue().severity.name()));
    colAlertMetric.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
        c.getValue().metric == null ? "" : c.getValue().metric.name()));
    colAlertValue.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
        String.format("%.1f", c.getValue().value)));
    colAlertThreshold.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
        String.format("%.1f", c.getValue().threshold)));
    colAlertMessage.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
        c.getValue().message == null ? "" : c.getValue().message));
    colAlertTs.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
        TS_FMT.format(java.time.Instant.ofEpochMilli(c.getValue().ts).atZone(ZoneId.systemDefault()))));
  }

  private void setupAlertFilters() {
    alertsSeverityFilter.getItems().setAll("All", "WARN", "CRITICAL");
    alertsSeverityFilter.getSelectionModel().select(0);
    alertsAgentFilter.getItems().setAll("All");
    alertsAgentFilter.getSelectionModel().select(0);
    alertsSeverityFilter.setOnAction(e -> applyAlertsFilter());
    alertsAgentFilter.setOnAction(e -> applyAlertsFilter());
  }

  private void applyAlertsFilter() {
    String agent = alertsAgentFilter.getSelectionModel().getSelectedItem();
    String sev = alertsSeverityFilter.getSelectionModel().getSelectedItem();
    recentAlertsFiltered.setPredicate(a -> {
      if (a == null) return false;
      if (agent != null && !"All".equals(agent) && !agent.equals(a.agentId)) return false;
      if (sev != null && !"All".equals(sev) && (a.severity == null || !sev.equals(a.severity.name()))) return false;
      return true;
    });
  }

  private void appendRecentAlerts(List<AlertEvent> alerts) {
    if (alerts == null) return;
    for (AlertEvent a : alerts) {
      recentAlerts.add(0, a);
    }
    while (recentAlerts.size() > 10) {
      recentAlerts.remove(recentAlerts.size() - 1);
    }
    updateAlertFiltersChoices();
    recentAlertsTable.refresh();
  }

  private void updateAlertFiltersChoices() {
    List<String> ids = recentAlerts.stream()
        .map(a -> a.agentId)
        .filter(id -> id != null && !id.isBlank())
        .distinct()
        .sorted()
        .collect(Collectors.toList());
    String selected = alertsAgentFilter.getSelectionModel().getSelectedItem();
    alertsAgentFilter.getItems().setAll("All");
    alertsAgentFilter.getItems().addAll(ids);
    if (selected != null && alertsAgentFilter.getItems().contains(selected)) {
      alertsAgentFilter.getSelectionModel().select(selected);
    } else {
      alertsAgentFilter.getSelectionModel().select(0);
    }
  }

  private void updateRecentAlertsCounter(int newCount) {
    recentAlertsCounter.setText(recentAlerts.size() + " recent / " + totalAlerts + " total");
  }

  @FXML
  private void onExportAlertsCsv() {
    if (recentAlertsFiltered == null || recentAlertsFiltered.isEmpty()) return;
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Export alerts CSV");
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
    chooser.setInitialFileName("alerts.csv");
    java.io.File file = chooser.showSaveDialog(recentAlertsTable.getScene().getWindow());
    if (file == null) return;
    try (java.io.PrintWriter out = new java.io.PrintWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
      out.println("ts,agentId,severity,metric,value,threshold,message");
      for (AlertEvent a : recentAlertsFiltered) {
        out.printf("%d,%s,%s,%s,%.1f,%.1f,%s%n",
            a.ts,
            a.agentId,
            a.severity,
            a.metric,
            a.value,
            a.threshold,
            a.message == null ? "" : a.message.replace(',', ' '));
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private javafx.scene.Node buildAlertBadge(AlertEvent a) {
    String txt = a.severity + " - " + a.metric;
    Label badge = new Label(txt);
    badge.getStyleClass().add("badge");
    if (a.severity == edu.ds.monitoring.common.dto.Severity.CRITICAL) {
      badge.getStyleClass().add("badge-critical");
    } else {
      badge.getStyleClass().add("badge-warn");
    }
    badge.setStyle("-fx-text-fill: #ffffff;");
    return badge;
  }

  @FXML
  private void onSaveThresholds() {
    thresholdErrorLabel.setText("");
    try {
      int cWarn = parsePercent(cpuWarnField.getText());
      int cCrit = parsePercent(cpuCritField.getText());
      int rWarn = parsePercent(ramWarnField.getText());
      int rCrit = parsePercent(ramCritField.getText());
      int dWarn = parsePercent(diskWarnField.getText());
      int dCrit = parsePercent(diskCritField.getText());
      validateOrder(cWarn, cCrit, "CPU");
      validateOrder(rWarn, rCrit, "RAM");
      validateOrder(dWarn, dCrit, "Disk");
      cpuWarn = cWarn; cpuCrit = cCrit;
      ramWarn = rWarn; ramCrit = rCrit;
      diskWarn = dWarn; diskCrit = dCrit;
      saveThresholdsToPrefs();
      addThresholdLines();
      thresholdErrorLabel.setText("Saved");
    } catch (IllegalArgumentException ex) {
      thresholdErrorLabel.setText(ex.getMessage());
    }
  }

  @FXML
  private void onLoadThresholdDefaults() {
    cpuWarn = DEF_CPU_WARN; cpuCrit = DEF_CPU_CRIT;
    ramWarn = DEF_RAM_WARN; ramCrit = DEF_RAM_CRIT;
    diskWarn = DEF_DISK_WARN; diskCrit = DEF_DISK_CRIT;
    populateThresholdFields();
    saveThresholdsToPrefs();
    addThresholdLines();
    thresholdErrorLabel.setText("Defaults loaded");
  }

  private void populateThresholdFields() {
    if (cpuWarnField == null) return;
    cpuWarnField.setText(Integer.toString(cpuWarn));
    cpuCritField.setText(Integer.toString(cpuCrit));
    ramWarnField.setText(Integer.toString(ramWarn));
    ramCritField.setText(Integer.toString(ramCrit));
    diskWarnField.setText(Integer.toString(diskWarn));
    diskCritField.setText(Integer.toString(diskCrit));
  }

  private void loadThresholdsFromPrefs() {
    cpuWarn = prefs.getInt("cpuWarn", DEF_CPU_WARN);
    cpuCrit = prefs.getInt("cpuCrit", DEF_CPU_CRIT);
    ramWarn = prefs.getInt("ramWarn", DEF_RAM_WARN);
    ramCrit = prefs.getInt("ramCrit", DEF_RAM_CRIT);
    diskWarn = prefs.getInt("diskWarn", DEF_DISK_WARN);
    diskCrit = prefs.getInt("diskCrit", DEF_DISK_CRIT);
  }

  private void saveThresholdsToPrefs() {
    prefs.putInt("cpuWarn", cpuWarn);
    prefs.putInt("cpuCrit", cpuCrit);
    prefs.putInt("ramWarn", ramWarn);
    prefs.putInt("ramCrit", ramCrit);
    prefs.putInt("diskWarn", diskWarn);
    prefs.putInt("diskCrit", diskCrit);
  }

  private int parsePercent(String s) {
    try {
      int v = Integer.parseInt(s.trim());
      if (v < 0 || v > 100) throw new IllegalArgumentException("0-100 only");
      return v;
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Numbers only (0-100)");
    }
  }

  private void validateOrder(int warn, int crit, String label) {
    if (warn >= crit) throw new IllegalArgumentException(label + " warn < crit");
  }

  private void updateKpiCounts() {
    long online = backingList.stream().filter(r -> r.getStatus() == AgentStatus.OK).count();
    long warn = backingList.stream().filter(r -> r.getStatus() == AgentStatus.WARN).count();
    long critical = backingList.stream().filter(r -> r.getStatus() == AgentStatus.CRITICAL).count();
    long offline = backingList.stream().filter(r -> r.getStatus() == AgentStatus.OFFLINE).count();
    kpiOnline.setText(Long.toString(online));
    kpiWarn.setText(Long.toString(warn));
    kpiCritical.setText(Long.toString(critical));
    kpiOffline.setText(Long.toString(offline));
  }

  private void updateSelectedKpis(AgentRow row) {
    if (row == null) {
      kpiSelCpu.setText("--");
      kpiSelRam.setText("--");
      kpiSelDisk.setText("--");
      return;
    }
    kpiSelCpu.setText(String.format("%.0f%%", row.cpuPctProperty().get()));
    kpiSelRam.setText(String.format("%.0f%%", row.ramPctProperty().get()));
    kpiSelDisk.setText(String.format("%.0f%%", row.diskPctProperty().get()));
  }

  private void updateAlertWindow() {
    long cutoff = System.currentTimeMillis() - 5 * 60_000L;
    while (!alertTimestamps.isEmpty() && alertTimestamps.peekFirst() < cutoff) {
      alertTimestamps.removeFirst();
    }
    kpiAlerts.setText(Integer.toString(alertTimestamps.size()));
  }
}
