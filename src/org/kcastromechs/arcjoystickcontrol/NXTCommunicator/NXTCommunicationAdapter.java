package org.kcastromechs.arcjoystickcontrol.NXTCommunicator;

import java.io.IOException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public abstract class NXTCommunicationAdapter extends Thread {

	// We'll wait up to 100 ms before deciding that we have a timeout situation
	int responseMessageTimeout = 100;
	
	// Messages for communicating to the NXT
	protected static final int MOTOR_A = 0;
	protected static final int MOTOR_B = 1;
	protected static final int MOTOR_C = 2;
	protected static final int MOTOR_B_ACTION = 40;
	protected static final int MOTOR_RESET = 10;
	protected static final int DO_BEEP = 51;
	protected static final int DO_ACTION = 52;
	protected static final int READ_MOTOR_STATE = 60;
	protected static final int GET_FIRMWARE_VERSION = 70;
	protected static final int DISCONNECT = 99;
	protected static final int NO_DELAY = 0;
	protected static final byte GET_BATTERY_LEVEL = 0x0B;
	
	//private static final UUID SERIAL_PORT_SERVICE_CLASS_UUID = UUID
	//		.fromString("00001101-0000-1000-8000-00805F9B34FB");
	// this is the only OUI registered by LEGO, see
	// http://standards.ieee.org/regauth/oui/index.shtml
	public static final String OUI_LEGO = "00:16:53";

	// Member variables
	protected Handler owningHandler;
	private boolean mMotorHasBeenStarted;
	protected boolean connected = false;
	private byte[] returnMessage;
	
	private boolean waitingForResponseMessage = false;
	private byte expectedResponseCommandType = 0x00;
	long mResponseMessageTimeoutMillis = 0;


	/* **********************************************************************
	 * BEGIN Abstract Methods
	 * *********************************************************************/

	/**
	 * Sends a message to the stream connected to the NXT
	 * 
	 * @param message
	 *            message to send
	 * @throws IOException
	 */
	protected abstract void sendMessageToStream(byte[] message)
			throws IOException;

	/**
	 * Receive a message from the stream connected to the NXT
	 * 
	 * @return message received from the NXT
	 * @throws IOException
	 */
	protected abstract byte[] receiveMessageFromStream() throws IOException;

	/**
	 * Create the connection to the NXT. Any setup should have already been
	 * handled (presumably via the constructor)
	 * 
	 * @throws IOException
	 */
	protected abstract void createNXTconnection() throws IOException;

	/**
	 * Destroy the connection stream with the NXT
	 * 
	 * @throws IOException
	 */
	protected abstract void destroyNXTconnection() throws IOException;

	/* **********************************************************************
	 * END section of methods that should be overwritten
	 * **********************************************************************/

	/**
	 * Creates the connection, waits for incoming messages and dispatches them.
	 * The thread will be terminated on closing of the connection.
	 */
	@Override
	public void run() {

		try {

			createNXTconnection();
		} catch (IOException e) {
		}

		while (connected) {
			try {

				/*
				 * TODO THIS MAY STILL NEED IMPROVEMENT
				 * 
				 * WHAT THIS IS TRYING TO DO IS TO KNOW IF WE'RE EXPECTING A REPONSE,
				 * AND IF WE ARE KNOW WHAT REPONSE WE SHOULD BE EXPECTING, AND FILTER
				 * TO ONLY THOSE RESPONSES TO SEND BACK TO THE UI.  ADDITIONALLY WE'RE
				 * ONLY WANT TO TRY FOR A FAIRLY SHORT PERIOD BEFORE GIVING UP.
				 * 
				 * AS A SIDE NOTE, TECHNICALLY WE SHOULD SYNCRONIZE THE SENDING
				 * (AND RECEIVING) OF MESSAGES SO THAT WE DON'T HAVE THE CHANCE
				 * OF HAVING MULTIPLE MESSAGES CLOBBER ONE ANOTHER. MIGHT NOT BE
				 * MUCH OF AN ISSUE IF THE UI IS RUNNING IN A SINGLE THREAD AND
				 * ALL COMMANDS COME FROM THE UI - BUT WOULD BE SAFER
				 * NONETHELESS.
				 */
				if (waitingForResponseMessage) {
					returnMessage = receiveMessageFromStream();
					if (returnMessage.length >= 2 
							&& returnMessage[0] == NXTMessageUtil.getReplyCommand()
							&& returnMessage[1] == expectedResponseCommandType) {
						
						dispatchMessageToOwner(returnMessage);
						waitingForResponseMessage=false;
						
					} else {
						
						// OK... so we didn't get a message, or it wasn't the correct type
						// check to see if we've hit the timeout (and stop looking if we have)
						if (System.currentTimeMillis()>mResponseMessageTimeoutMillis)
							waitingForResponseMessage=false;
						
					}
				} else {
					
					try {
						Thread.sleep(5);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}
					
				}
				
				// Check to see 

			} catch (IOException e) {
				// don't inform the user when connection is already closed
				if (connected)
					sendState(NXTCommunicator.STATE_RECEIVEERROR);
				return;
			}
		}
	}

	/**
	 * Sends a message on the opened OutputStream. In case of an error the state
	 * is sent to the handler.
	 * 
	 * @param message
	 *            , the message as a byte array
	 */

	private synchronized void sendMessage(byte[] message) {

		// if we're waiting for a response message, just return without
		// sending the message.  It's up to the implementations to
		// make sure they're not sending messages while waiting on
		// responses.
		if (waitingForResponseMessage) return;

		try {
			sendMessageToStream(message);
			
			// If we should be expecting a response, set the flag to 
			//  expect the response, the command type we're expecting,
			//  and set the timeout time.
			if (message[0]==NXTMessageUtil.DIRECT_COMMAND_REPLY ||
					message[0]==NXTMessageUtil.SYSTEM_COMMAND_REPLY) {
				waitingForResponseMessage=true;
				expectedResponseCommandType = message[1];
				mResponseMessageTimeoutMillis = System.currentTimeMillis()+responseMessageTimeout;
			}
			
		} catch (IOException e) {
			sendState(NXTCommunicator.STATE_SENDERROR);
		}
	}

	/*
	 * Returns a message to the owning instance via the handler.
	 */
	protected void dispatchMessageToOwner(byte[] message) {

		switch (message[1]) {

		case NXTMessageUtil.GET_OUTPUT_STATE:

			if (message.length >= 25)
				sendState(NXTCommunicator.MOTOR_STATE, message);

			break;

		case NXTMessageUtil.GET_FIRMWARE_VERSION:

			if (message.length >= 7)
				sendState(NXTCommunicator.FIRMWARE_VERSION, message);

			break;
			
		case NXTMessageUtil.GET_BATTERY_LEVEL:

			if (message.length >= 5)
				sendState(NXTCommunicator.BATTERY_LEVEL, message);

			break;
			
		case NXTMessageUtil.FIND_FIRST:
		case NXTMessageUtil.FIND_NEXT:

			if (message.length >= 28) {
				// Success
				if (message[2] == 0)
					sendState(NXTCommunicator.FIND_FILES, message);
			}

			break;

		case NXTMessageUtil.GET_CURRENT_PROGRAM_NAME:

			if (message.length >= 23) {
				sendState(NXTCommunicator.PROGRAM_NAME, message);
			}

			break;

		}
	}
	
	/* ************************************************************
	 * BEGIN SECTION OF HELPER METHODS FOR THE MESSAGE HANDLER
	 * These methods are called by the handler when we get a request
	 *   to send a specific message.  
	 ************************************************************ */
	
	private void doBeep(int frequency, int duration) {
		byte[] message = NXTMessageUtil.getBeepMessage(frequency, duration);
		sendMessage(message);
	}

	private void startProgram(String programName) {
		byte[] message = NXTMessageUtil.getStartProgramMessage(programName);
		sendMessage(message);
	}

	private void stopProgram() {
		byte[] message = NXTMessageUtil.getStopProgramMessage();
		sendMessage(message);
	}

	private void getProgramName() {
		byte[] message = NXTMessageUtil.getProgramNameMessage();
		sendMessage(message);
	}

	private void changeMotorSpeed(int motor, int speed) {
		if (speed > 100)
			speed = 100;

		else if (speed < -100)
			speed = -100;

		byte[] message = NXTMessageUtil.getMotorMessage(motor, speed);
		sendMessage(message);
		mMotorHasBeenStarted = true;
	}

	private void rotateTo(int motor, int end) {
		byte[] message = NXTMessageUtil.getMotorMessage(motor, -80, end);
		sendMessage(message);
		mMotorHasBeenStarted = true;
	}

	private void reset(int motor) {
		byte[] message = NXTMessageUtil.getResetMessage(motor);
		sendMessage(message);
	}

	private void readMotorState(int motor) {
		byte[] message = NXTMessageUtil.getOutputStateMessage(motor);
		sendMessage(message);
	}

	private void getFirmwareVersion() {
		byte[] message = NXTMessageUtil.getFirmwareVersionMessage();
		sendMessage(message);
	}
	
	private void getBatteryLevel() {
		byte[] message = NXTMessageUtil.getBatteryLevel();
		sendMessage(message);
		
	}

	private void findFiles(boolean findFirst, int handle) {
		byte[] message = NXTMessageUtil.getFindFilesMessage(findFirst, handle,
				"*.*");
		sendMessage(message);
	}
	
	/* ************************************************************
	 * BEGIN SECTION OF HELPER METHODS FOR THE MESSAGE HANDLER
	 * These methods are called by the handler when we get a request
	 *   to send a specific message.  
	 ************************************************************ */

	/**
	 * Returns the handler for communication to the adapter thread.
	 * 
	 * @return
	 */
	protected Handler getHandler() {
		return myHandler;
	}

	/* ************************************************************
	 * MAIN MESSAGE HANDLER
	 * This handler receives messages (usually from the UI) and starts
	 *  the process of constructing the correct message to send to
	 *  the NXT.
	 *  
	 *  This is done via a handler to help with thread safety.
	 ************************************************************ */
	final Handler myHandler = new Handler() {

		@Override
		public void handleMessage(Message myMessage) {

			int message = myMessage.getData().getInt("message");

			switch (message) {
			case MOTOR_A:
				changeMotorSpeed(0, myMessage.getData().getInt("value1"));
				break;
			case MOTOR_B:
				changeMotorSpeed(1, myMessage.getData().getInt("value1"));
				break;
			case MOTOR_C:
				changeMotorSpeed(2, myMessage.getData().getInt("value1"));
				break;
			case MOTOR_B_ACTION:
				rotateTo(MOTOR_B, myMessage.getData().getInt("value1"));
				break;
			case MOTOR_RESET:
				reset(myMessage.getData().getInt("value1"));
				break;
			case NXTCommunicator.START_PROGRAM:
				startProgram(myMessage.getData().getString("name"));
				break;
			case NXTCommunicator.STOP_PROGRAM:
				stopProgram();
				break;
			case NXTCommunicator.GET_PROGRAM_NAME:
				getProgramName();
				break;
			case DO_BEEP:
				doBeep(myMessage.getData().getInt("value1"), myMessage
						.getData().getInt("value2"));
				break;
			case READ_MOTOR_STATE:
				readMotorState(myMessage.getData().getInt("value1"));
				break;
			case GET_FIRMWARE_VERSION:
				getFirmwareVersion();
				break;
			case GET_BATTERY_LEVEL:
				getBatteryLevel();
				break;
			case NXTCommunicator.FIND_FILES:
				findFiles(myMessage.getData().getInt("value1") == 0, myMessage
						.getData().getInt("value2"));
				break;
			case DISCONNECT:
				if (mMotorHasBeenStarted) {
					// send stop messages before closing
					changeMotorSpeed(0, 0);
					changeMotorSpeed(1, 0);
					changeMotorSpeed(2, 0);
					// try to wait long enough for the motor messages to get
					// through
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
					}
				}
				// now disconnect, which should terminate the main run loop
				try {
					destroyNXTconnection();
				} catch (IOException e) {
				}
				break;
			}
		}

		
	};
	
	
	/* ************************************************************
	 * BEGIN SECTION OF HELPER METHODS FOR SENDING MESSAGES BACK TO THE OWNER (USUALLY THE UI) 
	 ************************************************************ */

	protected void sendInfo(String toastText) {
		Bundle myBundle = new Bundle();
		myBundle.putInt("message", NXTCommunicator.INFO_MESSAGE);
		myBundle.putString("toastText", toastText);
		sendBundle(myBundle);
	}

	protected void sendState(int message) {
		Bundle myBundle = new Bundle();
		myBundle.putInt("message", message);
		sendBundle(myBundle);
	}

	protected void sendState(int message, byte[] detail) {
		Bundle myBundle = new Bundle();
		myBundle.putInt("message", message);
		myBundle.putByteArray("detail", detail);
		sendBundle(myBundle);
	}

	private void sendBundle(Bundle myBundle) {
		Message myMessage = owningHandler.obtainMessage();
		myMessage.setData(myBundle);
		owningHandler.sendMessage(myMessage);
	}
	
	/* ************************************************************
	 * END SECTION OF HELPER METHODS FOR SENDING MESSAGES BACK TO THE OWNER (USUALLY THE UI) 
	 ************************************************************ */

}
