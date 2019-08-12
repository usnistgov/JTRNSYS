package com.socket;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.socket.JTRNSYSProto.DataTypes;
import com.socket.JTRNSYSProto.JTRNSYSData;

import commonClasses.SingleObjectDataTransfer;
import commonClasses.UIInputData;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;

/**
 * @author Farhad Omar
 * 		   National Institute of Standards and Technology
 *		   farhad.omar@nist.gov
 */

public class ServerRunnableMatlab implements Runnable {
	
	// Socket communication related instance variables
	private static int portNumber = 1345;
	
	// Initialize socket variables
	private static ServerSocket server = null;
	private static Socket client = null; 
	private static JTRNSYSData jTRN;
	
	// Handle exception when input stream is null
	private static byte[] inputStreamDataFromLastSimulation;
	
	// An object for handling UI data.
	UIInputData uiInputData = new UIInputData();
	
	// Used to call Matlab 
	public static MatlabProxy proxy;
	public static Object[] dataFromMatlab;
	

	
	// Make the UI buttons responsive, launching the server on a new thread
	private Thread worker;
	private AtomicBoolean running = new AtomicBoolean(false);
	
	
	@Override
	public void run() {
		
		running.set(true);
		
		// Get the only object of SingleObjectDataTransfer
		SingleObjectDataTransfer objectInstanceRead = SingleObjectDataTransfer.getInstance();
		List<Double> TRNSYSData = null; 
		
		double[] dataFromTRNSYS = null;

		/* Get the UI data from the singleton object */
		uiInputData.setInput_1(objectInstanceRead.getInput_1());
		uiInputData.setInput_2(objectInstanceRead.getInput_2());
		uiInputData.setInput_3(objectInstanceRead.getInput_3());
		uiInputData.setInput_4(objectInstanceRead.getInput_4());
		
		while (running.get()) {
			
			// ========================================================================
			// Socket Communication
			// ========================================================================
			try {
				server = new ServerSocket(portNumber);

			} 
			catch (IOException e) {
				System.out.println("Error on port: " + portNumber + ", " + e);
				System.exit(1);
			}
			
			System.out.println("Server setup and waiting for client connection ...");
			
			/*
			 * Keeps the server running regardless of the clients status. In other words, the server
			 * as it is written right now will keep the 1345 port open and waits for a single client
			 * to connect to it and transmit data. 
			 */
			int keep = 0;
			
			// Launch MATLAB
			try {
				
				runMatlab();
				System.out.println("Matlab proxy has started...");
				
			} catch (MatlabConnectionException e1) {
				e1.printStackTrace();
			}


			try {

				while (true){

					try {

						client = server.accept(); 
						System.out.println("Client connection accepted ...");
					} 
					catch (IOException e) {
						
						System.out.println("Did not accept connection: " + e);
						System.exit(1);
					}

					byte[] msg;

					try{

						/* ************************************** Process Incoming Data *********************************************
						 * Read the input stream from a client and parse the incoming data into an array of type doubles. 
						 * ***********************************************************************************************************/
						
						// Read input stream
						InputStream inStream = client.getInputStream(); 
						
						// Get the bytes from input stream
						msg = receivedMessage(inStream);

						// De-serialize input bytes using protocol buffers
						jTRN = JTRNSYSData.parseFrom(msg);
						
						// Create an array list of type doubles, holding data from the client
						TRNSYSData = new ArrayList<Double>();
						TRNSYSData = PrintDataTRNSYS(jTRN); 

						// Display the value of the first input
						//System.out.println("Server received input1 from TRNSYS: " + TRNSYSData.get(0));

						// Create an array of type doubles so that the data can be easily send to other processes
						dataFromTRNSYS = new double[TRNSYSData.size()];

						for (int index = 0; index < dataFromTRNSYS.length; index++){
							dataFromTRNSYS[index] = TRNSYSData.get(index);
						}	        	

						//****************************************** END *********************************************************

						
			        	
						// ##########################################################################################################
						// ********************************* To Do - Manipulate Incoming Data from TRNSYS ***************************
						
						// TRNSYS can communicate with other software environments
						// and participate in co-simulation. 
															
						// dataFromMatlab is storing returned data in an array of Objects from Matlab
						dataFromMatlab = sendAndReceiveDataToFromMatlab(dataFromTRNSYS);
						
						//Parse the Object[] data returned from Matlab
						double[] dataForTRNSYS = parseMatlabData(dataFromMatlab);	
						
						// ****************************************** END **********************************************************
						// #########################################################################################################
						
						
						// *************************************** Serialize and Send Data to TRNSYS ******************************
						sendDataToTRNSYS(dataForTRNSYS);
						
						// ************************************************** END **************************************************

					} catch(IOException e){ 

						System.out.println("IO Error in streams " + e.getMessage());


					} finally {
						
						// de-referencing objects for garbage collection
						msg = null; 
						TRNSYSData = null;
						dataFromTRNSYS = null;
					}

					keep++;
					System.out.println("Server ran: " + keep);
		

				}//end of while
				
			} catch (Exception e) {
				
				e.printStackTrace();
				
			} finally {

				try {
					
					client.close();
					
				} catch (IOException e) {
					
					e.printStackTrace();
				}
			}
		}
		
	}
	

	/*
	 * The information in this method is obtained from: 
	 * https://code.google.com/archive/p/matlabcontrol/wikis/Walkthrough.wiki
	 */
	public static void runMatlab() throws MatlabConnectionException{
		/*
		 * This implementation will open a single session of Matlab and all subsequent java calls 
		 * from this class (or similar ones) will not open multiple Matlab sessions. Obtained from
		 * stackoverflow.
		 */
		 MatlabProxyFactoryOptions options =
		            new MatlabProxyFactoryOptions.Builder().setUsePreviouslyControlledSession(true).build();
		        MatlabProxyFactory factory = new MatlabProxyFactory(options);
		        proxy = factory.getProxy();
	}

	
	public static Object[] sendAndReceiveDataToFromMatlab(double[] dToMatlab){
		Object[] returnedDataFromMatlab = null;
		try {
			
			/*
			 * proxy function's format: proxy.returningFeval(String functionName, int nargout, Object args):
			 * 
			 * "functionName"	: JavaMatlabDemoFunction is the entry function where data is exchanged between Java and MATLAB;
			 * "int nargout"	: Returns the number of output arguments specified in the JavaMatlabDemoFunction; and
			 * "Object args"	: In this implementation, it is a 1D-array of type doubles.
			 * 
			 */
		
			returnedDataFromMatlab = proxy.returningFeval("JavaMatlabDemoFunction", 1, dToMatlab);
			
		} catch (MatlabInvocationException e) {
			
			e.printStackTrace();
		}
		return returnedDataFromMatlab;
		
	}
	
	// The data from Matlab is returned in an array of Objects, need to convert it to an array of type doubles
	public static double[] parseMatlabData(Object[] dataRecievedFromMatlab){
	
		
		double[] dataFromMat = null;
		
		try {
			
			Object firstArgument = dataRecievedFromMatlab[0];
			dataFromMat = ((double[]) firstArgument);
			
			System.out.println("Matlab retuned values: " + Arrays.toString(dataFromMat));
			
		} catch (NullPointerException e){
			System.out.println("Data recieved from Matlab is null: " + e.getMessage());
		}
		
		return dataFromMat;
	}
	

	// Extract the correct number of bytes from the input stream
	public static byte[] receivedMessage(InputStream inputstream) {
		
		byte inputStreamHolder[];
		
        try {
        	
        	// Create a large buffer to hold incoming bytes
            inputStreamHolder = new byte[4096]; 	
            
            // Get the actual number of bytes (message) received
            int numberOfBytesReceived = inputstream.read(inputStreamHolder);	
            
            // Create an array of bytes to store the actual number of bytes received
            byte bytesReceived[] = new byte[numberOfBytesReceived];	
            
            for (int i = 0; i < numberOfBytesReceived; i++) { 
            	bytesReceived[i] = inputStreamHolder[i]; 
            }
            
            /* Store bytes received from the input stream and use it when there is an exception thrown 
             * because InputStream might be null.*/
            inputStreamDataFromLastSimulation = bytesReceived;
            
            // Returning bytes
            return bytesReceived;
            
        } catch (Exception e) {
            System.out.println("In receivedMessage() InputStream exception ocurred! " + e.getMessage());
      	
            	byte placeHolder[] = inputStreamDataFromLastSimulation;
            	return placeHolder;     
            
        } finally {
        	
        	inputStreamHolder = null;
        }
        
    }
	
	public static List<Double> PrintDataTRNSYS(JTRNSYSData jTRN) throws NullPointerException{
		// Clearing all previously stored data in the dataList vector
		List<Double> list = null;
		
		//dataList.clear();
		try {
			
			list = new ArrayList<Double>();
			
			for (DataTypes data: jTRN.getDataList()){
    		
	    		//Array data types (multiple values)
				int counter = 0;
				
	    		for (DataTypes.ArrayDataTypes arrayData : data.getArraysList()) {
	    			
	    			//System.out.println("Double["+counter+"] values received from TRNSYS: " + arrayData.getDoubleArray());
	    			list.add(counter, arrayData.getDoubleArray());
	    			
	    			counter++;
	    		}
    		
			}
			
			
		} catch (NullPointerException e) {
			System.out.println("PrintDataTRNSYS: " + e.getMessage());
			
		}
		return list;
		
	}
	
	public static void sendDataToTRNSYS(double[] dataForTRNS){
		 /**
		  * Protocol Buffers License: Copyright 2008 Google Inc.  All rights reserved.
		  */
		// dataForTRNS is an array of modified values originated in TRNSYS Java
					
		// outArray holds the data that is being sent to the TRNSYS model via the server. Set the size
		// of the outArray based on the number of data that is being sent to TRNSYS.
		double[] outArray = new double[1];
		
		for (int i = 0; i < dataForTRNS.length; i++) {
			
			outArray[i] = dataForTRNS[i];
		}
					
		// sending data back to the client
    	DataTypes.Builder clientBoundMessage = DataTypes.newBuilder();
       	
    	//System.out.println("Number of elements in the dataList: " + sizeOfDataList);
    	double[] holdIncomingData = outArray;
    	int mSize = holdIncomingData.length;
    	
    	for (int i = 0; i < mSize; i++){
    		clientBoundMessage.addArrays(i, DataTypes.ArrayDataTypes.newBuilder().setDoubleArray(holdIncomingData[i]));
    	}
 
    	//Initially, the size of the message is sent to socket.
    	byte[] sendToClient = clientBoundMessage.build().toByteArray();
    	
    	int messageSize = sendToClient.length;// get the size of the message in number of bytes
    	
    	DataTypes.Builder sizeOfM = DataTypes.newBuilder();// creating a protobuf message for sending the size the message
    	
    	sizeOfM.setIntData(messageSize); //setting the value of the intData variable in protobuf's message
    	
    	byte[] sizeOfMessage = sizeOfM.build().toByteArray(); //converting the protobuf message to a byte[]
    	//System.out.println("Sending message size ...");
    	
    	try {
    		//sending the size of the message that is to be send
			client.getOutputStream().write(sizeOfMessage);
			
			// sending the actual message
			client.getOutputStream().write(sendToClient); 
        	
        	
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			
			System.out.println("Number of bytes sent from the server: " + messageSize);
		}			
	}
	
	// Enable the UI's "Close Server" button to close the server connection and end the thread 
	public boolean interruptServer() {
		
		boolean flag = false;
		
		running.set(false);
		try {
			
			if (server != null) { 
				server.close();
				flag = true;
			} 

		} catch (NullPointerException | IOException e) {
			flag = false;
			e.getSuppressed();
		}
		
		return flag;
	}

	// Launch the server in a new thread
	public void startServer() {
		worker = new Thread(this);
		worker.start();
	}
}