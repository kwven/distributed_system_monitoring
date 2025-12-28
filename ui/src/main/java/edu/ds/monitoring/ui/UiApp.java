package edu.ds.monitoring.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class UiApp extends Application {
  @Override
  public void start(Stage stage) {
    stage.setTitle("Monitoring UI (baseline)");
    stage.setScene(new Scene(new Label("UI baseline OK"), 400, 200));
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
