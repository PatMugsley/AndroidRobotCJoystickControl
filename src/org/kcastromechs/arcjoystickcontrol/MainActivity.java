package org.kcastromechs.arcjoystickcontrol;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.kcastromechs.arcjoystickcontrol.NXTCommunicator.NXTCommunicator;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	// Set whether or not to use bluetooth
	// USB will be used instead when false
	private boolean use_bluetooth = true;

	private boolean connected = false;
	private boolean CommErrorPending = false;
	private boolean pairing;
	private static boolean btOnByUs = false;

	// private NXTBluetoothCommunicationAdapter myNXTBluetoothCommunicator =
	// null;
	// private Handler NXTCommHandler;

	private Activity thisActivity;
	private ProgressDialog connectingProgressDialog;
	private Toast reusableToast;
	private NXTCommunicator mNXTCommunicator;

	private static final int REQUEST_CONNECT_DEVICE = 1000;
	private static final int REQUEST_ENABLE_BT = 2000;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		thisActivity = this;

		// create the reusable toast
		reusableToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

		UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
		HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		if (deviceIterator.hasNext()) {
			UsbDevice device = deviceIterator.next();
			mNXTCommunicator = new NXTCommunicator(manager, device, myHandler,
					getResources());
			connected = true;
			showToast("USB CONNECTED!", 3000);
		}

		/*
		 * Code that was used to inspect the usb connection with the NXT.
		 * Keeping it here right now for reference, should be either moved to a
		 * separate test class or activity, or deleted at some point.
		 * 
		 * HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
		 * Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		 * while(deviceIterator.hasNext()){ UsbDevice device =
		 * deviceIterator.next();
		 * 
		 * StringBuffer sb = new StringBuffer();
		 * sb.append("onCreate\n").append(device.getDeviceName())
		 * .append(" ").append(device.getVendorId())
		 * .append(" ").append(device.getProductId())
		 * .append(" ").append(device.getDeviceClass())
		 * .append(" ").append(device.getDeviceSubclass())
		 * .append(" ").append(device.getDeviceProtocol());
		 * 
		 * //showToast("onCreate\n"+device.getDeviceName()+" "+device.getVendorId
		 * ()+" "+device.getProductId() //
		 * +" "+device.getDeviceClass()+" "+device
		 * .getDeviceSubclass()+" "+device.getDeviceProtocol(),3000);
		 * 
		 * 
		 * int icount = device.getInterfaceCount();
		 * sb.append("\n\nInterface Count = ").append(icount);
		 * 
		 * 
		 * for (int i=0; i<icount;i++) { UsbInterface intf =
		 * device.getInterface(i); int ifcount = intf.getEndpointCount();
		 * sb.append
		 * ("\nInterface ").append(i).append(" endpoints = ").append(ifcount);
		 * 
		 * for (int j=0; j<ifcount; j++) { UsbEndpoint mEndpoint =
		 * intf.getEndpoint(j); sb.append("\n .. if ").append(j); switch
		 * (mEndpoint.getType()) { case UsbConstants.USB_ENDPOINT_XFER_BULK:
		 * sb.append(" XFER_BULK"); break; case
		 * UsbConstants.USB_ENDPOINT_XFER_CONTROL: sb.append(" XFER_CONTROL");
		 * break; case UsbConstants.USB_ENDPOINT_XFER_INT:
		 * sb.append(" XFER_INT"); break; case
		 * UsbConstants.USB_ENDPOINT_XFER_ISOC: sb.append(" XFER_ISOC"); } if
		 * (mEndpoint.getDirection()==UsbConstants.USB_DIR_IN)
		 * sb.append(" DIR_IN"); if
		 * (mEndpoint.getDirection()==UsbConstants.USB_DIR_OUT) {
		 * sb.append(" DIR_OUT");
		 * 
		 * 
		 * 
		 * }
		 * 
		 * 
		 * }
		 * 
		 * }
		 * 
		 * showToast(sb.toString(),10000);
		 * 
		 * // let's see if we can make it beep // the NXT uses only 1 interface
		 * (index=0), and it has two endpoints // ... endpoint 0 is for xfer_out
		 * // ... endpoint 1 is for xfer_in UsbDeviceConnection mConnection =
		 * manager.openDevice(device); UsbInterface intf =
		 * device.getInterface(0); UsbEndpoint mEndpoint = intf.getEndpoint(0);
		 * mConnection.claimInterface(intf, true); byte[] message =
		 * NXTMessageUtil.getBeepMessage(440, 2500);
		 * mConnection.bulkTransfer(mEndpoint, message, message.length, 0); //
		 * really really bad to hold up the UI thread... but just for now... try
		 * { Thread.sleep(250); } catch (InterruptedException e) {
		 * 
		 * } message = NXTMessageUtil.getBeepMessage(880, 500);
		 * mConnection.bulkTransfer(mEndpoint, message, message.length, 0);
		 * 
		 * //UsbInterface intf = mDevice.getInterface(0); //mEndpoint =
		 * intf.getEndpoint(0); //mConnection = mUsbManager.openDevice(mDevice);
		 * //mConnection.claimInterface(intf, true);
		 * 
		 * 
		 * 
		 * }
		 */

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onStart() {
		super.onStart();

		// no bluetooth available
		if (BluetoothAdapter.getDefaultAdapter() == null) {
			showToast(R.string.bt_initialization_failure, Toast.LENGTH_LONG);
			destroyNXTCommunicator();
			finish();
			return;
		}

		if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		} else {
			selectNXT();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		destroyNXTCommunicator();
	}

	@Override
	public void onPause() {
		// mView.unregisterListener();
		destroyNXTCommunicator();
		super.onStop();
	}

	@Override
	public void onSaveInstanceState(Bundle icicle) {
		super.onSaveInstanceState(icicle);
		// mView.unregisterListener();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:

			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address and start a new bt communicator
				// thread
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				pairing = data.getExtras().getBoolean(
						DeviceListActivity.PAIRING);
				startNXTCommunicator(address);
			}

			break;

		case REQUEST_ENABLE_BT:

			// When the request to enable Bluetooth returns
			switch (resultCode) {
			case Activity.RESULT_OK:
				btOnByUs = true;
				selectNXT();
				break;
			case Activity.RESULT_CANCELED:
				showToast(R.string.bt_needs_to_be_enabled, Toast.LENGTH_SHORT);
				finish();
				break;
			default:
				showToast(R.string.problem_at_connecting, Toast.LENGTH_SHORT);
				finish();
				break;
			}

			break;
		}
	}

	/* *******************************************************
	 * 
	 * UI ACTION HANDLERS
	 * 
	 * ******************************************************
	 */

	/*
	 * Handle the beep request form the UI
	 */
	public void doBeep(View view) {

		TextView tv = (TextView) findViewById(R.id.activitylog);
		tv.append("Beep!\n");
		mNXTCommunicator.playTone(880, 500);
	}
	public void checkBattery(View view) {

		TextView tv = (TextView) findViewById(R.id.activitylog);
		tv.append("Checking Battery...\n");
		mNXTCommunicator.checkBatteryLevel();
	}

	public void playLowC(View view) {

		TextView tv = (TextView) findViewById(R.id.activitylog);
		tv.append("C\n");
		mNXTCommunicator.playTone(262, 500);
	}

	public void playLowD(View view) {

		TextView tv = (TextView) findViewById(R.id.activitylog);
		tv.append("D\n");
		mNXTCommunicator.playTone(294, 500);
	}

	public void playLowE(View view) {

		TextView tv = (TextView) findViewById(R.id.activitylog);
		tv.append("E\n");
		mNXTCommunicator.playTone(330, 500);
	}

	public void playLowF(View view) {

		TextView tv = (TextView) findViewById(R.id.activitylog);
		tv.append("F\n");
		mNXTCommunicator.playTone(349, 500);
	}

	public void playLowG(View view) {

		TextView tv = (TextView) findViewById(R.id.activitylog);
		tv.append("G\n");
		mNXTCommunicator.playTone(392, 500);
	}

	public void playLowA(View view) {

		TextView tv = (TextView) findViewById(R.id.activitylog);
		tv.append("A\n");
		mNXTCommunicator.playTone(440, 500);
	}

	public void playLowB(View view) {

		TextView tv = (TextView) findViewById(R.id.activitylog);
		tv.append("B\n");
		mNXTCommunicator.playTone(494, 500);
	}

	public void playHighC(View view) {

		TextView tv = (TextView) findViewById(R.id.activitylog);
		tv.append("C\n");
		mNXTCommunicator.playTone(523, 500);
	}

	/*
	 * Handle the getFW request from the UI
	 */
	public void getFirmware(View view) {

		TextView tv = (TextView) findViewById(R.id.activitylog);
		tv.append("Get Firmware Version\n");
		mNXTCommunicator.getFirmwareVersion();
	}

	/* *******************************************************
	 * 
	 * END UI ACTION HANDLERS
	 * 
	 * ******************************************************
	 */

	/**
	 * Creates and starts the a thread for communication via bluetooth to the
	 * NXT robot.
	 * 
	 * @param mac_address
	 *            The MAC address of the NXT robot.
	 */
	private void startNXTCommunicator(String mac_address) {

		connected = false;
		connectingProgressDialog = ProgressDialog.show(this, "", getResources()
				.getString(R.string.connecting_please_wait), true);

		mNXTCommunicator = new NXTCommunicator(
				BluetoothAdapter.getDefaultAdapter(), mac_address, myHandler,
				getResources());

		/*
		 * TODO should probably be implemented... updateButtonsAndMenu();
		 */
	}

	/**
	 * Sends a message for disconnecting to the communication thread.
	 */
	public void destroyNXTCommunicator() {

		if (mNXTCommunicator != null) {
			mNXTCommunicator.disconnect();
			mNXTCommunicator = null;
		}

		connected = false;
		/*
		 * TODO should be implemented... updateButtonsAndMenu();
		 */
	}

	/**
	 * Displays a message as a toast
	 * 
	 * @param textToShow
	 *            the message
	 * @param length
	 *            the length of the toast to display
	 */
	private void showToast(String textToShow, int length) {
		reusableToast.setText(textToShow);
		reusableToast.setDuration(length);
		reusableToast.show();
	}

	/**
	 * Displays a message as a toast
	 * 
	 * @param resID
	 *            the ressource ID to display
	 * @param length
	 *            the length of the toast to display
	 */
	private void showToast(int resID, int length) {
		reusableToast.setText(resID);
		reusableToast.setDuration(length);
		reusableToast.show();
	}

	/**
	 * Receive messages from the BTCommunicator
	 */
	final Handler myHandler = new Handler() {
		@SuppressLint("HandlerLeak")
		@Override
		public void handleMessage(Message myMessage) {

			switch (myMessage.getData().getInt("message")) {
			case NXTCommunicator.INFO_MESSAGE:
				showToast(myMessage.getData().getString("toastText"),
						Toast.LENGTH_SHORT);
				break;

			case NXTCommunicator.STATE_CONNECTED:
				connected = true;
				connectingProgressDialog.dismiss();
				showToast("connected!", 3000);
				// TODO Should do this!
				// updateButtonsAndMenu();
				break;
			/*
			 * case NXTBluetoothCommunicator.MOTOR_STATE:
			 * 
			 * if (myNXTBluetoothCommunicator != null) { byte[] motorMessage =
			 * myNXTBluetoothCommunicator.getReturnMessage(); int position =
			 * byteToInt(motorMessage[21]) + (byteToInt(motorMessage[22]) << 8)
			 * + (byteToInt(motorMessage[23]) << 16) +
			 * (byteToInt(motorMessage[24]) << 24);
			 * showToast(getResources().getString(R.string.current_position) +
			 * position, Toast.LENGTH_SHORT); }
			 * 
			 * break;
			 */
			case NXTCommunicator.STATE_CONNECTERROR_PAIRING:
				connectingProgressDialog.dismiss();
				destroyNXTCommunicator();
				break;

			case NXTCommunicator.STATE_CONNECTERROR:
				connectingProgressDialog.dismiss();
			case NXTCommunicator.STATE_RECEIVEERROR:
			case NXTCommunicator.STATE_SENDERROR:

				showToast("Send or Receive Error!", 3000);

				destroyNXTCommunicator();
				if (CommErrorPending == false) {
					CommErrorPending = true;
					// inform the user of the error with an AlertDialog
					AlertDialog.Builder builder = new AlertDialog.Builder(
							thisActivity);
					builder.setTitle(
							getResources().getString(
									R.string.bt_error_dialog_title))
							.setMessage(
									getResources().getString(
											R.string.bt_error_dialog_message))
							.setCancelable(false)
							.setPositiveButton("OK",
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog, int id) {
											CommErrorPending = false;
											dialog.cancel();
											selectNXT();
										}
									});
					builder.create().show();
				}

				break;

			case NXTCommunicator.FIRMWARE_VERSION:

				byte[] detail = myMessage.getData().getByteArray("detail");
				int pmin = detail[3] & 0xFF;
				int pmaj = detail[4] & 0xFF;
				int fmin = detail[5] & 0xFF;
				int fmaj = detail[6] & 0xFF;
				TextView tv = (TextView) findViewById(R.id.activitylog);
				tv.append("Firmware\nprotocol: " + pmaj + "." + pmin
						+ " | firmware:" + fmaj + "." + fmin + "\n");
				break;
				
			case NXTCommunicator.BATTERY_LEVEL:

				byte[] detail1 = myMessage.getData().getByteArray("detail");
				int lowByte = detail1[3] & 0xFF;
				int highByte = detail1[4] & 0xFF;
				int batteryLevel = (highByte*256)+lowByte;
				TextView tv1 = (TextView) findViewById(R.id.activitylog);
				tv1.append("Battery Level: " + batteryLevel +"mv \n");
				break;

			/*
			 * case NXTBluetoothCommunicator.FIND_FILES:
			 * 
			 * if (myNXTBluetoothCommunicator != null) { byte[] fileMessage =
			 * myNXTBluetoothCommunicator.getReturnMessage(); String fileName =
			 * new String(fileMessage, 4, 20); fileName =
			 * fileName.replaceAll("\0","");
			 * 
			 * if (mRobotType == R.id.robot_type_lejos ||
			 * fileName.endsWith(".nxj") || fileName.endsWith(".rxe")) {
			 * programList.add(fileName); }
			 * 
			 * // find next entry with appropriate handle, // limit number of
			 * programs (in case of error (endless loop)) if (programList.size()
			 * <= MAX_PROGRAMS)
			 * sendBTCmessage(NXTBluetoothCommunicator.NO_DELAY,
			 * NXTBluetoothCommunicator.FIND_FILES, 1,
			 * byteToInt(fileMessage[3])); }
			 * 
			 * break;
			 * 
			 * case NXTBluetoothCommunicator.PROGRAM_NAME: if
			 * (myNXTBluetoothCommunicator != null) { byte[] returnMessage =
			 * myNXTBluetoothCommunicator.getReturnMessage();
			 * startRXEprogram(returnMessage[2]); }
			 * 
			 * break;
			 */

			}
		}
	};

	void selectNXT() {
		if (use_bluetooth) {
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
		}
	}

}
