/**
 *   Copyright 2013 Ken Pugsley, Matt Pugsley
 *
 *	 Provides an abstraction layer between the UI code and the back end for communication to
 *   the NXT.  
 *
 **/

package org.kcastromechs.arcjoystickcontrol.NXTCommunicator;

import java.io.IOException;

import android.bluetooth.BluetoothAdapter;
import android.content.res.Resources;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class NXTCommunicator {

	// Return Message Types
	public static final int INFO_MESSAGE = 1000;
	public static final int STATE_CONNECTED = 1001;
	public static final int STATE_CONNECTERROR = 1002;
	public static final int STATE_CONNECTERROR_PAIRING = 1022;
	public static final int MOTOR_STATE = 1003;
	public static final int STATE_RECEIVEERROR = 1004;
	public static final int STATE_SENDERROR = 1005;
	public static final int FIRMWARE_VERSION = 1006;
	public static final int BATTERY_LEVEL = 1030;
	public static final int FIND_FILES = 1007;
	public static final int START_PROGRAM = 1008;
	public static final int STOP_PROGRAM = 1009;
	public static final int GET_PROGRAM_NAME = 1010;
	public static final int PROGRAM_NAME = 1011;


	private NXTCommunicationAdapter mNXTCommunicationAdapter;
	private Handler mNXTCommunicationHandler;

	/*
	 * Constructor for use with bluetooth
	 */
	public NXTCommunicator(BluetoothAdapter BTAdapter, String mac_address,
			Handler uiHandler, Resources resources) {

		// Create the communication adapter to the NXT
		mNXTCommunicationAdapter = (NXTCommunicationAdapter) new NXTBluetoothCommunicationAdapter(
				mac_address, BluetoothAdapter.getDefaultAdapter(), uiHandler,
				resources);
		mNXTCommunicationHandler = mNXTCommunicationAdapter.getHandler();

		mNXTCommunicationAdapter.start();

	}
	
	/*
	 * Constructor to use with usb
	 * 
	 */
	public NXTCommunicator(UsbManager UsbManager,UsbDevice device, Handler owningHandler,
			Resources resources) {
		
		mNXTCommunicationAdapter = (NXTCommunicationAdapter) new NXTUSBCommunicationAdapter(UsbManager, device, owningHandler, resources);
		mNXTCommunicationHandler = mNXTCommunicationAdapter.getHandler();

		mNXTCommunicationAdapter.start();
		
	}
	
	/**
	 * Tells the connected NXT to play a tone with the specified
	 * frequency and duration.
	 * @param frequency
	 * 			frequency of the tone to play in Hz
	 * @param duration
	 * 			duration of the tone to play in ms
	 */	
	public void playTone(int frequency, int duration) {

		sendMessageToAdapter(NXTCommunicationAdapter.NO_DELAY,
				NXTCommunicationAdapter.DO_BEEP, frequency, duration);

	}
	
	public void getFirmwareVersion() {
		sendMessageToAdapter(NXTCommunicationAdapter.NO_DELAY,NXTCommunicationAdapter.GET_FIRMWARE_VERSION,"");
	}
	
	public void checkBatteryLevel() {
		sendMessageToAdapter(NXTCommunicationAdapter.NO_DELAY,NXTCommunicationAdapter.GET_BATTERY_LEVEL,"");
		
	}

	/**
	 * Sends a raw message to the robot. Generally this should only be used when
	 * constructing a message unsupported by any other defined method for this
	 * class.
	 * 
	 * @param delay
	 *            time to wait before sending the message.
	 * @param message
	 *            the message type (as defined in BTCommucator)
	 * @param value1
	 *            first parameter
	 * @param value2
	 *            second parameter
	 */
	void sendMessageToAdapter(int delay, int message, int value1, int value2) {
		Bundle myBundle = new Bundle();
		myBundle.putInt("message", message);
		myBundle.putInt("value1", value1);
		myBundle.putInt("value2", value2);
		Message myMessage = mNXTCommunicationHandler.obtainMessage();
		myMessage.setData(myBundle);

		mNXTCommunicationHandler.sendMessage(myMessage);

	}

	/**
	 * Sends a raw message to the robot. Generally this should only be used when
	 * constructing a message unsupported by any other defined method for this
	 * class.
	 * 
	 * @param delay
	 *            time to wait before sending the message.
	 * @param message
	 *            the message type (as defined in BTCommucator)
	 * @param String
	 *            a String parameter
	 */

	void sendMessageToAdapter(int delay, int message, String name) {
		Bundle myBundle = new Bundle();
		myBundle.putInt("message", message);
		myBundle.putString("name", name);
		Message myMessage = mNXTCommunicationAdapter.getHandler()
				.obtainMessage();
		myMessage.setData(myBundle);

		if (delay == 0)
			mNXTCommunicationHandler.sendMessage(myMessage);
		else
			mNXTCommunicationHandler.sendMessageDelayed(myMessage,
					delay);
	}

	/**
	 * Disconnects from the NXT (if connected).  Once disconnected, a new
	 * NXTCommunicator instance should be constructed to reconnect.
	 */
	public void disconnect() {
		// TODO Auto-generated method stub
		sendMessageToAdapter(NXTCommunicationAdapter.NO_DELAY,
				NXTCommunicationAdapter.DISCONNECT, 0, 0);	
	}

	

}
