package application;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * @author Farhad Omar
 * 		   National Institute of Standards and Technology
 *		   farhad.omar@nist.gov
 */

// Launch the main user interface
public class ServerUI extends Application {
	
	@Override
	public void start(Stage primaryStage) throws IOException {
		
		Parent root = FXMLLoader.load(getClass().getResource("Main.fxml"));
		primaryStage.setTitle("Launching Server");
		primaryStage.setScene(new Scene(root));
		primaryStage.show();
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
}
