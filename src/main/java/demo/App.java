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
            var keyResource = getClass().getResourceAsStream("/serviceAccountKey.json");
            if (keyResource != null) {
                System.out.println("Found serviceAccountKey.json in resources");
                // Save to temp file since Firebase needs a file path
                java.io.File tempFile = java.io.File.createTempFile("firebase-key", ".json");
                tempFile.deleteOnExit();
                java.nio.file.Files.copy(keyResource, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                FirebaseService.getInstance().initialize(tempFile.getAbsolutePath());
            } else {
                System.err.println("serviceAccountKey.json not found in resources, running in mock mode");
                FirebaseService.getInstance().initializeMock();
            }
        } catch (Exception e) {
            System.err.println("Error loading Firebase key: " + e.getMessage());
            e.printStackTrace();
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