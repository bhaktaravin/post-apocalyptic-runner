package demo;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX App
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        // Initialize Firebase with service account key from resources
        try {
            String keyPath = getClass().getResource("/serviceAccountKey.json").getPath();
            FirebaseService.getInstance().initialize(keyPath);
        } catch (Exception e) {
            System.err.println("Could not load Firebase key from resources, running in mock mode");
            FirebaseService.getInstance().initializeMock();
        }
        
        // Show main menu
        MainMenu menu = new MainMenu(stage);
        menu.show();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}