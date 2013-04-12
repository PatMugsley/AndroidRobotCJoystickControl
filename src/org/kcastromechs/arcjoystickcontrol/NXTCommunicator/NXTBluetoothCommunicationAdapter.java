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

package org.kcastromechs.arcjoystickcontrol.NXTCommunicator;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.res.Resources;
import android.os.Handler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;
import org.kcastromechs.arcjoystickcontrol.*;

/**
 * This class is for talking to a LEGO NXT robot via bluetooth. The
 * communication to the robot is done via LCP (LEGO communication protocol).
 * Objects of this class can either be run as standalone thread or controlled by
 * the owners, i.e. calling the send/receive methods by themselves.
 */
public class NXTBluetoothCommunicationAdapter extends NXTCommunicationAdapter {

	private static final UUID SERIAL_PORT_SERVICE_CLASS_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private Resources mResources;
	private BluetoothAdapter btAdapter;
	private BluetoothSocket nxtBTsocket = null;
	private OutputStream nxtOutputStream = null;
	private InputStream nxtInputStream = null;
	private String mMACaddress;



	protected NXTBluetoothCommunicationAdapter(String mac_address,
			BluetoothAdapter btAdapter, Handler owningHandler,
			Resources resources) {
		this.owningHandler = owningHandler;
		this.btAdapter = btAdapter;
		this.mResources = resources;
		this.mMACaddress = mac_address;
	}

	/**
	 * Create a bluetooth connection with SerialPortServiceClass_UUID
	 * 
	 * @see <a href=
	 *      "http://lejos.sourceforge.net/forum/viewtopic.php?t=1991&highlight=android"
	 *      /> On error the method either sends a message to it's owner or
	 *      creates an exception in the case of no message handler.
	 */
	protected void createNXTconnection() throws IOException {

		BluetoothSocket nxtBTSocketTemporary;
		BluetoothDevice nxtDevice = null;
		nxtDevice = btAdapter.getRemoteDevice(mMACaddress);
		if (nxtDevice == null) {
			if (owningHandler == null)
				throw new IOException();
			else {
				sendInfo(mResources.getString(R.string.no_paired_nxt));
				sendState(NXTCommunicator.STATE_CONNECTERROR);
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
				if (owningHandler == null)
					throw new IOException();
				else
					sendState(NXTCommunicator.STATE_CONNECTERROR);
				return;
			}
		}
		nxtBTsocket = nxtBTSocketTemporary;
		nxtInputStream = nxtBTsocket.getInputStream();
		nxtOutputStream = nxtBTsocket.getOutputStream();
		connected = true;

		// everything was OK
		if (owningHandler != null)
			sendState(NXTCommunicator.STATE_CONNECTED);
	}

	/**
	 * Closes the bluetooth connection. On error the method either sends a
	 * message to it's owner or creates an exception in the case of no message
	 * handler.
	 */
	protected void destroyNXTconnection() throws IOException {
		try {
			if (nxtBTsocket != null) {
				connected = false;
				nxtBTsocket.close();
				nxtBTsocket = null;
			}

			nxtInputStream = null;
			nxtOutputStream = null;

		} catch (IOException e) {
			if (owningHandler == null)
				throw e;
			else
				sendInfo(mResources.getString(R.string.problem_at_closing));
		}
	}

	/**
	 * Sends a message on the opened OutputStream Should only be used internally
	 * by sendMessage()
	 * 
	 * @param message, the message as a byte array
	 */
	protected void sendMessageToStream(byte[] message) throws IOException {
		if (nxtOutputStream == null)
			throw new IOException();

		// send message length
		int messageLength = message.length;
		nxtOutputStream.write(messageLength);
		nxtOutputStream.write(messageLength >> 8);
		nxtOutputStream.write(message, 0, message.length);
	}


	/**
	 * Receives a message on the opened InputStream
	 * 
	 * @return the message
	 */
	protected byte[] receiveMessageFromStream() throws IOException {
		if (nxtInputStream == null)
			throw new IOException();

		int length = nxtInputStream.read();
		length = (nxtInputStream.read() << 8) + length;
		byte[] returnMessage = new byte[length];
		nxtInputStream.read(returnMessage);
		return returnMessage;
	}


}
