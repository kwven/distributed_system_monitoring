package edu.ds.monitoring.ui.controller;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import edu.ds.monitoring.common.dto.MetricSample;
import edu.ds.monitoring.ui.model.AgentRow;
import edu.ds.monitoring.ui.service.MonitoringClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

public class AgentDetailsController {

  @FXML private Label agentLabel;
  @FXML private Label hostLabel;
  @FXML private Label ipLabel;
  @FXML private Label statusLabel;
  @FXML private Label lastSeenLabel;

  @FXML private ComboBox<String> periodCombo;
  @FXML private Button refreshButton;
  @FXML private Button exportCsvButton;
  @FXML private Label statCpuAvg;
  @FXML private Label statCpuMax;
  @FXML private Label statCpuMin;
  @FXML private Label statCpuLast;
  @FXML private Label statRamAvg;
  @FXML private Label statRamMax;
  @FXML private Label statRamMin;
  @FXML private Label statRamLast;
  @FXML private Label statDiskAvg;
  @FXML private Label statDiskMax;
  @FXML private Label statDiskMin;
  @FXML private Label statDiskLast;

  @FXML private LineChart<Number, Number> cpuChart;
  @FXML private LineChart<Number, Number> ramChart;
  @FXML private LineChart<Number, Number> diskChart;

  private MonitoringClient client;
  private AgentRow agent;
  private List<MetricSample> lastSamples = List.of();

  @FXML
  public void initialize() {
    periodCombo.getItems().addAll("1 min", "5 min", "1 h");
    periodCombo.getSelectionModel().select(0);
    periodCombo.setOnAction(e -> refresh());
    refreshButton.setOnAction(e -> refresh());
    exportCsvButton.setOnAction(e -> exportCsv());
    configureChart(cpuChart, "CPU %");
    configureChart(ramChart, "RAM %");
    configureChart(diskChart, "Disk %");
  }

  public void setContext(MonitoringClient client, AgentRow agent) {
    this.client = client;
    this.agent = agent;
    updateAgentInfo();
    refresh();
  }

  private void updateAgentInfo() {
    if (agent == null) return;
    agentLabel.setText(agent.getAgentId());
    hostLabel.setText(agent.hostProperty().get());
    ipLabel.setText(agent.ipProperty().get());
    statusLabel.setText(agent.statusProperty().get() == null ? "" : agent.statusProperty().get().name());
    lastSeenLabel.setText(agent.lastSeenAgoProperty().get());
  }

  private void refresh() {
    if (client == null || agent == null) return;
    long periodMs = selectedPeriodMs();
    long now = System.currentTimeMillis();
    long from = now - periodMs;

    CompletableFuture
        .supplyAsync(() -> {
          try {
            return client.getMetrics(agent.getAgentId(), from, now);
          } catch (Exception e) {
            return List.<MetricSample>of();
          }
        })
        .thenAccept(samples -> Platform.runLater(() -> {
          lastSamples = samples;
          updateCharts(samples, from, now);
        }));
  }

  private void updateCharts(List<MetricSample> samples, long from, long to) {
    cpuChart.getData().clear();
    ramChart.getData().clear();
    diskChart.getData().clear();

    XYChart.Series<Number, Number> cpuSeries = new XYChart.Series<>();
    cpuSeries.setName("CPU %");
    XYChart.Series<Number, Number> ramSeries = new XYChart.Series<>();
    ramSeries.setName("RAM %");
    XYChart.Series<Number, Number> diskSeries = new XYChart.Series<>();
    diskSeries.setName("Disk %");

    samples.stream()
        .sorted(Comparator.comparingLong(m -> m.ts))
        .forEach(m -> {
          double x = (m.ts - from) / 1000.0; // seconds in window
          cpuSeries.getData().add(new XYChart.Data<>(x, m.cpuPct));
          double ramPct = m.ramTotalBytes == 0 ? 0 : 100.0 * m.ramUsedBytes / m.ramTotalBytes;
          double diskPct = m.diskTotalBytes == 0 ? 0 : 100.0 * m.diskUsedBytes / m.diskTotalBytes;
          ramSeries.getData().add(new XYChart.Data<>(x, ramPct));
          diskSeries.getData().add(new XYChart.Data<>(x, diskPct));
        });

    cpuChart.getData().add(cpuSeries);
    ramChart.getData().add(ramSeries);
    diskChart.getData().add(diskSeries);

    double windowSec = (to - from) / 1000.0;
    updateAxisWindow(cpuChart, windowSec);
    updateAxisWindow(ramChart, windowSec);
    updateAxisWindow(diskChart, windowSec);
    updateStats(samples);
  }

  private void configureChart(LineChart<Number, Number> chart, String name) {
    chart.setTitle(name + " over time");
    chart.setAnimated(false);
    NumberAxis x = (NumberAxis) chart.getXAxis();
    NumberAxis y = (NumberAxis) chart.getYAxis();
    x.setLabel("Seconds");
    y.setLabel("%");
    x.setAutoRanging(false);
    y.setAutoRanging(false);
    y.setLowerBound(0);
    y.setUpperBound(100);
    y.setTickUnit(10);
  }

  private void updateAxisWindow(LineChart<Number, Number> chart, double windowSec) {
    NumberAxis x = (NumberAxis) chart.getXAxis();
    x.setLowerBound(0);
    x.setUpperBound(Math.max(10, windowSec));
    x.setTickUnit(Math.max(5, windowSec / 6));
  }

  private long selectedPeriodMs() {
    String sel = periodCombo.getSelectionModel().getSelectedItem();
    if (sel == null) return 60_000;
    return switch (sel) {
      case "5 min" -> 5 * 60_000L;
      case "1 h" -> 60 * 60_000L;
      default -> 60_000L;
    };
  }

  private void exportCsv() {
    if (lastSamples == null || lastSamples.isEmpty()) return;
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Export metrics CSV");
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
    chooser.setInitialFileName(agent != null ? agent.getAgentId() + "-metrics.csv" : "metrics.csv");
    java.io.File file = chooser.showSaveDialog(exportCsvButton.getScene().getWindow());
    if (file == null) return;
    try (java.io.PrintWriter out = new java.io.PrintWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
      out.println("ts,agentId,cpuPct,ramPct,diskPct");
      for (MetricSample m : lastSamples.stream().sorted(Comparator.comparingLong(s -> s.ts)).toList()) {
        double ramPct = m.ramTotalBytes == 0 ? 0 : 100.0 * m.ramUsedBytes / m.ramTotalBytes;
        double diskPct = m.diskTotalBytes == 0 ? 0 : 100.0 * m.diskUsedBytes / m.diskTotalBytes;
        out.printf("%d,%s,%.1f,%.1f,%.1f%n", m.ts, m.agentId, m.cpuPct, ramPct, diskPct);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private void updateStats(List<MetricSample> samples) {
    if (samples == null || samples.isEmpty()) {
      statCpuAvg.setText("--");
      statCpuMax.setText("--");
      statCpuMin.setText("--");
      statCpuLast.setText("--");
      statRamAvg.setText("--");
      statRamMax.setText("--");
      statRamMin.setText("--");
      statRamLast.setText("--");
      statDiskAvg.setText("--");
      statDiskMax.setText("--");
      statDiskMin.setText("--");
      statDiskLast.setText("--");
      return;
    }
    Stats cpu = new Stats();
    Stats ram = new Stats();
    Stats disk = new Stats();
    for (MetricSample m : samples) {
      cpu.add(m.cpuPct);
      double ramPct = m.ramTotalBytes == 0 ? 0 : 100.0 * m.ramUsedBytes / m.ramTotalBytes;
      double diskPct = m.diskTotalBytes == 0 ? 0 : 100.0 * m.diskUsedBytes / m.diskTotalBytes;
      ram.add(ramPct);
      disk.add(diskPct);
    }
    statCpuAvg.setText(cpu.avgString());
    statCpuMax.setText(cpu.maxString());
    statCpuMin.setText(cpu.minString());
    statCpuLast.setText(cpu.lastString());
    statRamAvg.setText(ram.avgString());
    statRamMax.setText(ram.maxString());
    statRamMin.setText(ram.minString());
    statRamLast.setText(ram.lastString());
    statDiskAvg.setText(disk.avgString());
    statDiskMax.setText(disk.maxString());
    statDiskMin.setText(disk.minString());
    statDiskLast.setText(disk.lastString());
  }

  private static class Stats {
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    double sum = 0;
    int count = 0;
    double last = Double.NaN;
    void add(double v) {
      min = Math.min(min, v);
      max = Math.max(max, v);
      sum += v;
      count++;
      last = v;
    }
    String avgString() { return count == 0 ? "--" : String.format("%.1f%%", sum / count); }
    String maxString() { return count == 0 ? "--" : String.format("%.1f%%", max); }
    String minString() { return count == 0 ? "--" : String.format("%.1f%%", min); }
    String lastString() { return count == 0 ? "--" : String.format("%.1f%%", last); }
  }
}
