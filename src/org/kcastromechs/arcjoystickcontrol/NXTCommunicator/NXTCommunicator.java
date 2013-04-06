/**
 *   Copyright 2013 Ken Pugsley, Matt Pugsley
 *
 *	 Provides an abstraction layer between the UI code and the back end for communication to
 *   the NXT.  
 *
 **/

package org.kcastromechs.arcjoystickcontrol.NXTCommunicator;

import android.bluetooth.BluetoothAdapter;
import android.content.res.Resources;
import android.os.Handler;

public class NXTCommunicator {
	
	public static final int USE_USB = 0;
	public static final int  USE_BLUETOOTH= 1;
	
	private Handler uiHandler;
	private Resources mResources;
	private Object btAdapter;

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


	
	
}
