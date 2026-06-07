package energy;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class EnergyApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/main_view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        stage.setTitle("Energy Monitoring System");
        stage.setScene(scene);
        stage.show();
    }
}
