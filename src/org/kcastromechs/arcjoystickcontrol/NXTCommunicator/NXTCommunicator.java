/**
 *   Copyright 2013 Ken Pugsley, Matt Pugsley
 *
 *	 Provides an abstraction layer between the UI code and the back end for communication to
 *   the NXT.  
 *
 **/

package org.kcastromechs.arcjoystickcontrol.NXTCommunicator;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class NXTCommunicator {
	
	// Public Constants (possibly needed by the UI)
	public static final int USE_USB = 100;
	public static final int  USE_BLUETOOTH= 101;
	
	// Return Message Types
	public static final int INFO_MESSAGE = 1000;
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
	
	
	
	private Handler uiHandler;
	private Resources mResources;
	private NXTCommunicationAdapter mAdapter;

	public NXTCommunicator(Handler uiHandler, Resources resources, int communicationIntent) {
		this.uiHandler = uiHandler;
		this.mResources = resources;
		
		switch (communicationIntent) {
			
		case USE_USB:
			break;
			
		case USE_BLUETOOTH:
			// TODO should initialize the btAdapter here...
//			this.btAdapter = btAdapter;

		
		}	
		
	}

	public void beep(int frequency, int duration) {

    	sendMessageToAdapter(NXTBluetoothCommunicationAdapter.NO_DELAY,NXTBluetoothCommunicationAdapter.DO_BEEP,440,500);
		
	}
	
	
    /**
     * Sends the message via the BTCommuncator to the robot.
     * @param delay time to wait before sending the message.
     * @param message the message type (as defined in BTCommucator)
     * @param value1 first parameter
     * @param value2 second parameter
     */   
    void sendMessageToAdapter(int delay, int message, int value1, int value2) {
        Bundle myBundle = new Bundle();
        myBundle.putInt("message", message);
        myBundle.putInt("value1", value1);
        myBundle.putInt("value2", value2);
        Message myMessage = mAdapter.getHandler().obtainMessage();
        myMessage.setData(myBundle);

        mAdapter.getHandler().sendMessage(myMessage);

    }
	
}
