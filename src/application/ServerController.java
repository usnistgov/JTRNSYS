package application;

import com.socket.ServerRunnable;
import com.socket.ServerRunnableMatlab;

import commonClasses.SingleObjectDataTransfer;
import commonClasses.UIInputData;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;


/**
 * @author Farhad Omar
 * 		   National Institute of Standards and Technology
 *		   farhad.omar@nist.gov
 */

/* Controller class is the brain*/
public class ServerController {
	
	@FXML private Button launch_server;
	@FXML private Button end_server;
	@FXML private Label sim_label;
	
	@FXML private Button submit_inputs;
	@FXML private TextField input_1;
	@FXML private TextField input_2;
	@FXML private TextField input_3;
	@FXML private TextField input_4;
	
	private static String input1;
	private static String input2;
	private static String input3;
	private static String input4;
	


	public static UIInputData uiDataClass = new UIInputData();
	
	//=============================================================================================
	/* Uncomment the line below for exchanging data between TRNSYS and Java to perform PVT related
	 * calculations in Java. */
	
	ServerRunnable serverClass = new ServerRunnable();
	//=============================================================================================

	
	//=============================================================================================
	/* Uncomment the line below to launch MATLAB and perform PVT related calculations in MATLAB. 
	 * Make sure to comment the ServerRunnable line above. Use the ServerRunnableMatlab object to 
	 * exchange data between TRNSYS and MATLAB through Java.
	 */
	
	//ServerRunnableMatlab serverClass = new ServerRunnableMatlab();
	//=============================================================================================

	@FXML
	public void launchServerButtonClicked() {
		launch_server.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
			
				// Run the server
				serverClass.startServer();
				
			}
			
		});
		
	} 
	
	@FXML
	public void closeServerButtonClicked(){
		
		end_server.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				
				boolean closedServer = serverClass.interruptServer();
				
				if (closedServer != true) {
					
					Stage stage = (Stage) end_server.getScene().getWindow();
					stage.close();
					System.out.println("Server closed!");
					
				} 
			}
			
		});
	}
	
		
	@FXML
	public void submitInputButtonClicked(){
		submit_inputs.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				
				/* Local static methods*/			
				setInput1(input_1.getText());
				setInput2(input_2.getText()); 
				setInput3(input_3.getText());			
				setInput4(input_4.getText());
	
				System.out.println(getInput1() + " " + getInput2() + " " + getInput3() + " " + getInput4());				
			}
			
		});
	}
	
	
	/* Sending user defined simulation parameters and advance simulation time for other classes to use. 
	 * The user defined data is acquired from the GUI.*/
	public static void sendUIData(){
		
		SingleObjectDataTransfer objectInstance = SingleObjectDataTransfer.getInstance();	
		
		uiDataClass.setInput_1(Double.parseDouble(getInput1()));
		uiDataClass.setInput_2(Double.parseDouble(getInput2()));
		uiDataClass.setInput_3(Double.parseDouble(getInput3()));
		uiDataClass.setInput_4(Double.parseDouble(getInput4()));
		
		objectInstance.setInput_1(uiDataClass.getInput_1());
		objectInstance.setInput_2(uiDataClass.getInput_2());
		objectInstance.setInput_3(uiDataClass.getInput_3());
		objectInstance.setInput_4(uiDataClass.getInput_4());
			
	}
	
	public static String getInput1() {
		return input1;
	}

	public static void setInput1(String input1) {
		ServerController.input1 = input1;
	}

	public static String getInput2() {
		return input2;
	}

	public static void setInput2(String input2) {
		ServerController.input2 = input2;
	}

	public static String getInput3() {
		return input3;
	}

	public static void setInput3(String input3) {
		ServerController.input3 = input3;
	}

	public static String getInput4() {
		return input4;
	}

	public static void setInput4(String input4) {
		ServerController.input4 = input4;
	}

	
}
