package edu.ds.monitoring.ui.control;

import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.shape.Line;

public class ThresholdLineChart extends LineChart<Number, Number> {

  private final Line warnLine = new Line();
  private final Line critLine = new Line();
  private double warnValue = Double.NaN;
  private double critValue = Double.NaN;

  public ThresholdLineChart() {
    this(new NumberAxis(), new NumberAxis());
  }

  public ThresholdLineChart(Axis<Number> xAxis, Axis<Number> yAxis) {
    super(xAxis, yAxis);
    init();
  }

  private void init() {
    setAnimated(false);
    warnLine.getStyleClass().addAll("threshold-line", "threshold-warn");
    critLine.getStyleClass().addAll("threshold-line", "threshold-crit");
    warnLine.setMouseTransparent(true);
    critLine.setMouseTransparent(true);
    getPlotChildren().addAll(warnLine, critLine);
  }

  public void setThresholds(double warn, double crit) {
    warnValue = warn;
    critValue = crit;
    requestChartLayout();
  }

  @Override
  protected void layoutPlotChildren() {
    super.layoutPlotChildren();
    layoutLine(warnLine, warnValue);
    layoutLine(critLine, critValue);
  }

  private void layoutLine(Line line, double value) {
    if (Double.isNaN(value)) {
      line.setVisible(false);
      return;
    }
    NumberAxis yAxis = (NumberAxis) getYAxis();
    NumberAxis xAxis = (NumberAxis) getXAxis();
    double y = yAxis.getDisplayPosition(value);
    double x0 = xAxis.getDisplayPosition(xAxis.getLowerBound());
    double x1 = xAxis.getDisplayPosition(xAxis.getUpperBound());
    if (Double.isNaN(y) || Double.isInfinite(y) || Double.isNaN(x0) || Double.isNaN(x1)) {
      line.setVisible(false);
      return;
    }
    line.setStartX(x0);
    line.setEndX(x1);
    line.setStartY(y);
    line.setEndY(y);
    line.setVisible(true);
  }
}
