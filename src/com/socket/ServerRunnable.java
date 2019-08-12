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
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * @author Farhad Omar
 * 		   National Institute of Standards and Technology
 *		   farhad.omar@nist.gov
 */

public class ServerRunnable implements Runnable {
	
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
				
				System.out.println("Error on port: " + portNumber + ", " + e.getStackTrace());
				System.exit(1);
			}
			
			System.out.println("Server setup and waiting for client connection ...");
			
			/*
			 * Keeps the server running regardless of the clients status. In other words, the server
			 * as it is written right now will keep the 1345 port open and waits for a single client
			 * to connect to it and transmit data. 
			 */
			int keep = 0;

			try {

				while (true){

					try {

						client = server.accept(); 
						System.out.println("Client connection accepted ...");
					} 
					catch (IOException e) {
						
						System.out.println("Did not accept connection: " + e.getStackTrace());
						System.exit(1);
					}

					byte[] msg;

					try{

						/* ************************************** Process Incoming Data *********************************************
						 * Read the input stream from a client and parse the incoming data into an array of type doubles. 
						 * ***********************************************************************************************************/
						
						// Read input stream
						InputStream inStream = client.getInputStream(); 
						
						// Get bytes from the input stream
						msg = receivedMessage(inStream);

						// De-serialize input bytes using protocol buffers
						jTRN = JTRNSYSData.parseFrom(msg);
						
						// Create an array list of type doubles, holding data from the client
						TRNSYSData = new ArrayList<Double>();
						TRNSYSData = PrintDataTRNSYS(jTRN); 

						// Create an array of type doubles from incoming data for sharing with other processes
						dataFromTRNSYS = new double[TRNSYSData.size()];

						for (int index = 0; index < dataFromTRNSYS.length; index++){
							dataFromTRNSYS[index] = TRNSYSData.get(index);
						}	        	

						//****************************************** END *********************************************************
						
						
						
						
						// ##########################################################################################################
						// ********************************* To Do - Manipulate Incoming Data from TRNSYS ***************************
						
						// Access incoming data from TRNSYS, perform calculations in Java or other software environment 
						double[] returnDataTo = PVT(dataFromTRNSYS);
						
						// ****************************************** END **********************************************************
						// #########################################################################################################
						
									
											
						
						// *************************************** Serialize and Send Data to TRNSYS ******************************			
						sendDataToTRNSYS(returnDataTo);
						// ************************************************** END **************************************************
						
						
					} catch(Exception  e){ 

						System.out.println(e.getStackTrace());
						

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
				
				System.out.println(e.getStackTrace());
				
			} finally {

				try {
					
					client.close();
					
				} catch (IOException e) {
					
					System.out.println(e.getStackTrace());
				}
			}
		}
		
	}
	

	
	private static double[] PVT(double[] dataFromTRNSYS) {
		
		System.out.println(Arrays.toString(dataFromTRNSYS));
		
		/* Change the size of the results array based on the number of outputs desired/required to be sent
		 * to TRNSYS. The outbound array for the PVT example only needs one value, pLoad. */
		
		double[] results = new double[1];
		
		double fLoad = dataFromTRNSYS[0];
		double pLoad = 500d * 3.6 * fLoad;
		
		results[0] = pLoad;
		
		return results;
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
            System.out.println("In receivedMessage() InputStream exception ocurred! " + e.getStackTrace());
      	
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
			System.out.println("PrintDataTRNSYS: " + e.getStackTrace());
			
		}
		return list;
		
	}
	
	public static void sendDataToTRNSYS(double[] dataForTRNS){
		 /**
		  * Protocol Buffers License: Copyright 2008 Google Inc.  All rights reserved.
		  */

		// ************************************* Serializing and Sending TRNSYS Bound Data ******************************************
		 
		// Sending data back to the client
    	DataTypes.Builder clientBoundMessage = DataTypes.newBuilder();
       	
    	double[] holdIncomingData = dataForTRNS;
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
			
			System.out.println(e.getStackTrace());
			
		} finally {
			
			System.out.println("Number of bytes sent from the server: " + messageSize);
		}			
	}
	
	
	// ******************************************** END *********************************************************************
	
	
	// ********************************************* Thread Related *********************************************************
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
	
	// ******************************************** END **********************************************************************
}
