/**
 *   Copyright 2013 Ken Pugsley, Matt Pugsley
 *   Portions Copyright 2010, 2011, 2012 Guenther Hoelzl, Shawn Brown from MINDdroid
 *
 *	 Utility class for generating messages to the NXT over bluetooth (and potentially USB).
 *   
 *   Very large portions of this code came from the MINDdroid project.
 *   
 *   Most of this code was constructed from the Mindstorms communication protocol from the
 *   LEGO MINDSTORMS NXT Bluetooth Developer Kit.
 *
 **/

package org.kcastromechs.arcjoystickcontrol.bt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;
import org.kcastromechs.arcjoystickcontrol.*;

/**
 * This class is for talking to a LEGO NXT robot via bluetooth. The
 * communciation to the robot is done via LCP (LEGO communication protocol).
 * Objects of this class can either be run as standalone thread or controlled by
 * the owners, i.e. calling the send/recive methods by themselves.
 */
public class NXTBTCommunicator extends Thread {
	public static final int MOTOR_A = 0;
	public static final int MOTOR_B = 1;
	public static final int MOTOR_C = 2;
	public static final int MOTOR_B_ACTION = 40;
	public static final int MOTOR_RESET = 10;
	public static final int DO_BEEP = 51;
	public static final int DO_ACTION = 52;
	public static final int READ_MOTOR_STATE = 60;
	public static final int GET_FIRMWARE_VERSION = 70;
	public static final int DISCONNECT = 99;

	public static final int DISPLAY_TOAST = 1000;
	public static final int STATE_CONNECTED = 1001;
	public static final int STATE_CONNECTERROR = 1002;
	public static final int STATE_CONNECTERROR_PAIRING = 1022;
	public static final int MOTOR_STATE = 1003;
	public static final int STATE_RECEIVEERROR = 1004;
	public static final int STATE_SENDERROR = 1005;
	public static final int FIRMWARE_VERSION = 1006;
	public static final int FIND_FILES = 1007;
	public static final int START_PROGRAM = 1008;
	public static final int STOP_PROGRAM = 1009;
	public static final int GET_PROGRAM_NAME = 1010;
	public static final int PROGRAM_NAME = 1011;
	public static final int SAY_TEXT = 1030;
	public static final int VIBRATE_PHONE = 1031;

	public static final int NO_DELAY = 0;

	private static final UUID SERIAL_PORT_SERVICE_CLASS_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	// this is the only OUI registered by LEGO, see
	// http://standards.ieee.org/regauth/oui/index.shtml
	public static final String OUI_LEGO = "00:16:53";

	private Resources mResources;
	private BluetoothAdapter btAdapter;
	private BluetoothSocket nxtBTsocket = null;
	private OutputStream nxtOutputStream = null;
	private InputStream nxtInputStream = null;
	private boolean connected = false;

	private Handler uiHandler;
	private String mMACaddress;

	private byte[] returnMessage;

	public NXTBTCommunicator(Handler uiHandler, BluetoothAdapter btAdapter,
			Resources resources) {
		this.uiHandler = uiHandler;
		this.btAdapter = btAdapter;
		this.mResources = resources;
	}

	public Handler getHandler() {
		return myHandler;
	}

	public byte[] getReturnMessage() {
		return returnMessage;
	}

	public void setMACAddress(String mMACaddress) {
		this.mMACaddress = mMACaddress;
	}

	/**
	 * @return The current status of the connection
	 */
	public boolean isConnected() {
		return connected;
	}

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
				returnMessage = receiveMessage();
				if ((returnMessage.length >= 2)
						&& ((returnMessage[0] == NXTMessageUtil
								.getReplyCommand()) || (returnMessage[0] == NXTMessageUtil
								.getDirectCommandNoReply())))
					dispatchMessageToOwner(returnMessage);

			} catch (IOException e) {
				// don't inform the user when connection is already closed
				if (connected)
					sendState(STATE_RECEIVEERROR);
				return;
			}
		}
	}

	/**
	 * Create a bluetooth connection with SerialPortServiceClass_UUID
	 * 
	 * @see <a href=
	 *      "http://lejos.sourceforge.net/forum/viewtopic.php?t=1991&highlight=android"
	 *      /> On error the method either sends a message to it's owner or
	 *      creates an exception in the case of no message handler.
	 */
	public void createNXTconnection() throws IOException {

		BluetoothSocket nxtBTSocketTemporary;
		BluetoothDevice nxtDevice = null;
		nxtDevice = btAdapter.getRemoteDevice(mMACaddress);
		if (nxtDevice == null) {
			if (uiHandler == null)
				throw new IOException();
			else {
				sendToast(mResources.getString(R.string.no_paired_nxt));
				sendState(STATE_CONNECTERROR);
				return;
			}
		}
		nxtBTSocketTemporary = nxtDevice
				.createRfcommSocketToServiceRecord(SERIAL_PORT_SERVICE_CLASS_UUID);
		try {
			nxtBTSocketTemporary.connect();
		} catch (IOException e) {

			// try another method for connection, this should work on the HTC
			// desire, credits to Michael Biermann
			try {
				Method mMethod = nxtDevice.getClass().getMethod(
						"createRfcommSocket", new Class[] { int.class });
				nxtBTSocketTemporary = (BluetoothSocket) mMethod.invoke(
						nxtDevice, Integer.valueOf(1));
				nxtBTSocketTemporary.connect();
			} catch (Exception e1) {
				if (uiHandler == null)
					throw new IOException();
				else
					sendState(STATE_CONNECTERROR);
				return;
			}
		}
		nxtBTsocket = nxtBTSocketTemporary;
		nxtInputStream = nxtBTsocket.getInputStream();
		nxtOutputStream = nxtBTsocket.getOutputStream();
		connected = true;

		// everything was OK
		if (uiHandler != null)
			sendState(STATE_CONNECTED);
	}

	/**
	 * Closes the bluetooth connection. On error the method either sends a
	 * message to it's owner or creates an exception in the case of no message
	 * handler.
	 */
	public void destroyNXTconnection() throws IOException {
		try {
			if (nxtBTsocket != null) {
				connected = false;
				nxtBTsocket.close();
				nxtBTsocket = null;
			}

			nxtInputStream = null;
			nxtOutputStream = null;

		} catch (IOException e) {
			if (uiHandler == null)
				throw e;
			else
				sendToast(mResources.getString(R.string.problem_at_closing));
		}
	}

	/*
	 * Sends a message on the opened OutputStream Should only be used internally
	 * by sendMessage()
	 * 
	 * @param message, the message as a byte array
	 */
	private void sendMessageToStream(byte[] message) throws IOException {
		if (nxtOutputStream == null)
			throw new IOException();

		// send message length
		int messageLength = message.length;
		nxtOutputStream.write(messageLength);
		nxtOutputStream.write(messageLength >> 8);
		nxtOutputStream.write(message, 0, message.length);
	}

	/**
	 * Sends a message on the opened OutputStream. In case of an error the state
	 * is sent to the handler.
	 * 
	 * @param message
	 *            , the message as a byte array
	 */

	private void sendMessage(byte[] message) {
		if (nxtOutputStream == null)
			return;

		try {
			sendMessageToStream(message);
		} catch (IOException e) {
			sendState(STATE_SENDERROR);
		}
	}

	/**
	 * Receives a message on the opened InputStream
	 * 
	 * @return the message
	 */
	public byte[] receiveMessage() throws IOException {
		if (nxtInputStream == null)
			throw new IOException();

		int length = nxtInputStream.read();
		length = (nxtInputStream.read() << 8) + length;
		byte[] returnMessage = new byte[length];
		nxtInputStream.read(returnMessage);
		return returnMessage;
	}

	private void dispatchMessageToOwner(byte[] message) {
		switch (message[1]) {

		case NXTMessageUtil.GET_OUTPUT_STATE:

			if (message.length >= 25)
				sendState(MOTOR_STATE, message);

			break;

		case NXTMessageUtil.GET_FIRMWARE_VERSION:

			if (message.length >= 7)
				sendState(FIRMWARE_VERSION, message);

			break;

		case NXTMessageUtil.FIND_FIRST:
		case NXTMessageUtil.FIND_NEXT:

			if (message.length >= 28) {
				// Success
				if (message[2] == 0)
					sendState(FIND_FILES, message);
			}

			break;

		case NXTMessageUtil.GET_CURRENT_PROGRAM_NAME:

			if (message.length >= 23) {
				sendState(PROGRAM_NAME, message);
			}

			break;

		}
	}

	private void doBeep(int frequency, int duration) {
		byte[] message = NXTMessageUtil.getBeepMessage(frequency, duration);
		sendMessage(message);
		waitSomeTime(20);
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
	}

	private void rotateTo(int motor, int end) {
		byte[] message = NXTMessageUtil.getMotorMessage(motor, -80, end);
		sendMessage(message);
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

	private void findFiles(boolean findFirst, int handle) {
		byte[] message = NXTMessageUtil.getFindFilesMessage(findFirst, handle,
				"*.*");
		sendMessage(message);
	}

	private void waitSomeTime(int millis) {
		try {
			Thread.sleep(millis);

		} catch (InterruptedException e) {
		}
	}

	private void sendToast(String toastText) {
		Bundle myBundle = new Bundle();
		myBundle.putInt("message", DISPLAY_TOAST);
		myBundle.putString("toastText", toastText);
		sendBundle(myBundle);
	}

	private void sendState(int message) {
		Bundle myBundle = new Bundle();
		myBundle.putInt("message", message);
		sendBundle(myBundle);
	}

	private void sendState(int message, byte[] detail) {
		Bundle myBundle = new Bundle();
		myBundle.putInt("message", message);
		myBundle.putByteArray("detail", detail);
		sendBundle(myBundle);
	}

	private void sendBundle(Bundle myBundle) {
		Message myMessage = myHandler.obtainMessage();
		myMessage.setData(myBundle);
		uiHandler.sendMessage(myMessage);
	}

	// receive messages from the UI
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
			case START_PROGRAM:
				startProgram(myMessage.getData().getString("name"));
				break;
			case STOP_PROGRAM:
				stopProgram();
				break;
			case GET_PROGRAM_NAME:
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
			case FIND_FILES:
				findFiles(myMessage.getData().getInt("value1") == 0, myMessage
						.getData().getInt("value2"));
				break;
			case DISCONNECT:
				// send stop messages before closing
				changeMotorSpeed(0, 0);
				changeMotorSpeed(1, 0);
				changeMotorSpeed(2, 0);
				waitSomeTime(500);
				try {
					destroyNXTconnection();
				} catch (IOException e) {
				}
				break;
			}
		}
	};

}
