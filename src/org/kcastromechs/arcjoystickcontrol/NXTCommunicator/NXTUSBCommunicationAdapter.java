package org.kcastromechs.arcjoystickcontrol.NXTCommunicator;

import java.io.IOException;
import android.content.res.Resources;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;

public class NXTUSBCommunicationAdapter extends NXTCommunicationAdapter {

	// TODO We probably don't need the Resources instance here.  Look at removing it.
	private Resources mResources;
	private UsbDevice mDevice = null;
	private UsbDeviceConnection mConnection;
	private UsbManager mUsbManager;
	private UsbEndpoint mEndpointOut;
	private UsbEndpoint mEndpointIn;
	
	// The NXT uses a 64 byte buffer
	private byte[] readbuffer = new byte[64];



	protected NXTUSBCommunicationAdapter(UsbManager UsbManager,UsbDevice device, Handler owningHandler,
			Resources resources) {
		mUsbManager = UsbManager;
		mDevice = device;
		this.owningHandler = owningHandler;
		this.mResources = resources;
	}

	/**
	 * Create a connection to the NXT, claiming usb interface 0
	 * 
	 * @see <a href=
	 *      "http://lejos.sourceforge.net/forum/viewtopic.php?t=1991&highlight=android"
	 *      /> On error the method either sends a message to it's owner or
	 *      creates an exception in the case of no message handler.
	 */
	protected void createNXTconnection() throws IOException {

		// the NXT uses only 1 interface (index=0), and it has two endpoints
        //   ... endpoint 0 is for xfer_out
        //   ... endpoint 1 is for xfer_in
		
		UsbInterface intf = mDevice.getInterface(0);
		mEndpointOut = intf.getEndpoint(0);
		mEndpointIn = intf.getEndpoint(1);
		mConnection = mUsbManager.openDevice(mDevice); 
		mConnection.claimInterface(intf, true);
		connected=true;
		
	}

	/**
	 * Closes the bluetooth connection. On error the method either sends a
	 * message to it's owner or creates an exception in the case of no message
	 * handler.
	 */
	protected void destroyNXTconnection() throws IOException {
		
		mConnection.releaseInterface(mDevice.getInterface(0));
		connected=false;
		
	}

	/*
	 * Sends a message on the opened USB connection/endpoint
	 * 
	 * @param message, the message as a byte array
	 */
	protected void sendMessageToStream(byte[] message) throws IOException {
		if (mConnection == null)
			throw new IOException();
		
		int ret = mConnection.bulkTransfer(mEndpointOut, message, message.length, 0); 
		if (ret<0) throw new IOException();
	}


	/**
	 * Receives a message on the opened InputStream
	 * 
	 * @return the message
	 */
	protected byte[] receiveMessageFromStream() throws IOException {
		if (mConnection == null) {
			throw new IOException();
		}
		
		// This next call can cause a delay if the nxt isn't sending a message
		//   It's important that the superclass doesn't look for messages all the time, only
		//   when we've requested a message response.
		int length = mConnection.bulkTransfer(mEndpointIn, readbuffer, readbuffer.length, 100);
		if (length<0) {
			throw new IOException();
		}
		byte[] returnMessage = new byte[length];
		if (length>0) System.arraycopy(readbuffer, 0, returnMessage, 0, length);
		return returnMessage;
		
		
	}
	
}
