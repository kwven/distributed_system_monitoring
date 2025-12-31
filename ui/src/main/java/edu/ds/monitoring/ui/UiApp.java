package edu.ds.monitoring.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class UiApp extends Application {
  @Override
  public void start(Stage stage) throws Exception {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/dashboard.fxml"));
    Scene scene = new Scene(loader.load(), 1200, 800);
    scene.getStylesheets().add(getClass().getResource("/theme.css").toExternalForm());
    stage.setTitle("Distributed Monitoring UI");
    stage.setScene(scene);
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
