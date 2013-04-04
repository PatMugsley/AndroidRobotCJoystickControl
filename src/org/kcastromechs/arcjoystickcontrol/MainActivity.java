package org.kcastromechs.arcjoystickcontrol;

import java.io.IOException;

import org.kcastromechs.arcjoystickcontrol.bt.NXTBTCommunicator;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {

    private boolean connected = false;
    private boolean CommErrorPending = false;
    private boolean pairing;
    private static boolean btOnByUs = false;
	
	private NXTBTCommunicator myNXTBTCommunicator = null;
    private Handler NXTCommHandler;
    
    private Activity thisActivity;
    private ProgressDialog connectingProgressDialog;
    private Toast reusableToast;
    
    private static final int REQUEST_CONNECT_DEVICE = 1000;
    private static final int REQUEST_ENABLE_BT = 2000;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        thisActivity = this;
        
        // create the reusable toast
        reusableToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
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
        if (BluetoothAdapter.getDefaultAdapter()==null) {
            showToast(R.string.bt_initialization_failure, Toast.LENGTH_LONG);
            destroyNXTBTCommunicator();
            finish();
            return;
        }            

        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            selectNXT();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyNXTBTCommunicator();
    }

    @Override
    public void onPause() {
        //mView.unregisterListener();
        destroyNXTBTCommunicator();
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);
        //mView.unregisterListener();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:

                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address and start a new bt communicator thread
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    pairing = data.getExtras().getBoolean(DeviceListActivity.PAIRING);
                    startNXTBTCommunicator(address);
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
    
    /*
     * TODO
     * This is definitely not quite right.  This part of the code shouldn't have to know the
     * details of connecting via BT.  Also is not very clean that we're having to know (with no
     * compiler help) the format of the beep command.
     *   
     */   
    public void doBeep(View view) {
    	/*
    	 * YUCK!!
    	 */
    	sendBTCmessage(NXTBTCommunicator.NO_DELAY,NXTBTCommunicator.DO_BEEP,440,500);
    	
    }
    
    
    
    /**
     * Creates a new object for communication to the NXT robot via bluetooth and fetches the corresponding handler.
     */
    private void createBTCommunicator() {
        // interestingly BT adapter needs to be obtained by the UI thread - so we pass it in in the constructor
        myNXTBTCommunicator = new NXTBTCommunicator(myHandler, BluetoothAdapter.getDefaultAdapter(), getResources());
        NXTCommHandler = myNXTBTCommunicator.getHandler();
    }


    /**
     * Creates and starts the a thread for communication via bluetooth to the NXT robot.
     * @param mac_address The MAC address of the NXT robot.
     */
    private void startNXTBTCommunicator(String mac_address) {
        connected = false;        
        connectingProgressDialog = ProgressDialog.show(this, "", getResources().getString(R.string.connecting_please_wait), true);

        if (myNXTBTCommunicator != null) {
            try {
                myNXTBTCommunicator.destroyNXTconnection();
            }
            catch (IOException e) { }
        }
        createBTCommunicator();
        myNXTBTCommunicator.setMACAddress(mac_address);
        myNXTBTCommunicator.start();
        /*
         * TODO
         * should probably be implemented...
        updateButtonsAndMenu();
        */
    }

    /**
     * Sends a message for disconnecting to the communcation thread.
     */
    public void destroyNXTBTCommunicator() {

        if (myNXTBTCommunicator != null) {
            sendBTCmessage(NXTBTCommunicator.NO_DELAY, NXTBTCommunicator.DISCONNECT, 0, 0);
            myNXTBTCommunicator = null;
        }

        connected = false;
        /*
         * TODO
         * should be implemented...
        updateButtonsAndMenu();
        */
    }


    /**
     * Sends the message via the BTCommuncator to the robot.
     * @param delay time to wait before sending the message.
     * @param message the message type (as defined in BTCommucator)
     * @param value1 first parameter
     * @param value2 second parameter
     */   
    void sendBTCmessage(int delay, int message, int value1, int value2) {
        Bundle myBundle = new Bundle();
        myBundle.putInt("message", message);
        myBundle.putInt("value1", value1);
        myBundle.putInt("value2", value2);
        Message myMessage = myHandler.obtainMessage();
        myMessage.setData(myBundle);

        if (delay == 0)
            NXTCommHandler.sendMessage(myMessage);

        else
            NXTCommHandler.sendMessageDelayed(myMessage, delay);
    }

    /**
     * Sends the message via the BTCommuncator to the robot.
     * @param delay time to wait before sending the message.
     * @param message the message type (as defined in BTCommucator)
     * @param String a String parameter
     */       
    void sendBTCmessage(int delay, int message, String name) {
        Bundle myBundle = new Bundle();
        myBundle.putInt("message", message);
        myBundle.putString("name", name);
        Message myMessage = myHandler.obtainMessage();
        myMessage.setData(myBundle);

        if (delay == 0)
            NXTCommHandler.sendMessage(myMessage);
        else
            NXTCommHandler.sendMessageDelayed(myMessage, delay);
    }

    
    /**
     * Displays a message as a toast
     * @param textToShow the message
     * @param length the length of the toast to display
     */
    private void showToast(String textToShow, int length) {
        reusableToast.setText(textToShow);
        reusableToast.setDuration(length);
        reusableToast.show();
    }

    /**
     * Displays a message as a toast
     * @param resID the ressource ID to display
     * @param length the length of the toast to display
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
        @Override
        public void handleMessage(Message myMessage) {
            switch (myMessage.getData().getInt("message")) {
                case NXTBTCommunicator.DISPLAY_TOAST:
                    showToast(myMessage.getData().getString("toastText"), Toast.LENGTH_SHORT);
                    break;
                    
                
                case NXTBTCommunicator.STATE_CONNECTED:
                    connected = true;
                    /*
                     * We don't want to automatically get the program list
                     * 
                     */

                    //programList = new ArrayList<String>();
                    connectingProgressDialog.dismiss();
                    //updateButtonsAndMenu();
                    //sendBTCmessage(NXTBTCommunicator.NO_DELAY, NXTBTCommunicator.GET_FIRMWARE_VERSION, 0, 0);
                    
                    break;
                 /*
                case NXTBTCommunicator.MOTOR_STATE:

                    if (myNXTBTCommunicator != null) {
                        byte[] motorMessage = myNXTBTCommunicator.getReturnMessage();
                        int position = byteToInt(motorMessage[21]) + (byteToInt(motorMessage[22]) << 8) + (byteToInt(motorMessage[23]) << 16)
                                       + (byteToInt(motorMessage[24]) << 24);
                        showToast(getResources().getString(R.string.current_position) + position, Toast.LENGTH_SHORT);
                    }

                    break;
				*/
                case NXTBTCommunicator.STATE_CONNECTERROR_PAIRING:
                    connectingProgressDialog.dismiss();
                    destroyNXTBTCommunicator();
                    break;

                case NXTBTCommunicator.STATE_CONNECTERROR:
                    connectingProgressDialog.dismiss();
                case NXTBTCommunicator.STATE_RECEIVEERROR:
                case NXTBTCommunicator.STATE_SENDERROR:

                    destroyNXTBTCommunicator();
                    if (CommErrorPending == false) {
                        CommErrorPending = true;
                        // inform the user of the error with an AlertDialog
                        AlertDialog.Builder builder = new AlertDialog.Builder(thisActivity);
                        builder.setTitle(getResources().getString(R.string.bt_error_dialog_title))
                        .setMessage(getResources().getString(R.string.bt_error_dialog_message)).setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                CommErrorPending = false;
                                dialog.cancel();
                                selectNXT();
                            }
                        });
                        builder.create().show();
                    }

                    break;

                /*
                case NXTBTCommunicator.FIRMWARE_VERSION:

                    if (myNXTBTCommunicator != null) {
                        byte[] firmwareMessage = myNXTBTCommunicator.getReturnMessage();
                        // check if we know the firmware
                        boolean isLejosMindDroid = true;
                        for (int pos=0; pos<4; pos++) {
                            if (firmwareMessage[pos + 3] != LCPMessage.FIRMWARE_VERSION_LEJOSMINDDROID[pos]) {
                                isLejosMindDroid = false;
                                break;
                            }
                        }
                        if (isLejosMindDroid) {
                            mRobotType = R.id.robot_type_lejos;
                            setUpByType();
                        }
                        // afterwards we search for all files on the robot
                        sendBTCmessage(NXTBTCommunicator.NO_DELAY, NXTBTCommunicator.FIND_FILES, 0, 0);
                    }

                    break;
				*/
                /*
                case NXTBTCommunicator.FIND_FILES:

                    if (myNXTBTCommunicator != null) {
                        byte[] fileMessage = myNXTBTCommunicator.getReturnMessage();
                        String fileName = new String(fileMessage, 4, 20);
                        fileName = fileName.replaceAll("\0","");

                        if (mRobotType == R.id.robot_type_lejos || fileName.endsWith(".nxj") || fileName.endsWith(".rxe")) {
                            programList.add(fileName);
                        }

                        // find next entry with appropriate handle, 
                        // limit number of programs (in case of error (endless loop))
                        if (programList.size() <= MAX_PROGRAMS)
                            sendBTCmessage(NXTBTCommunicator.NO_DELAY, NXTBTCommunicator.FIND_FILES,
                                           1, byteToInt(fileMessage[3]));
                    }

                    break;
                    
                case NXTBTCommunicator.PROGRAM_NAME:
                    if (myNXTBTCommunicator != null) {
                        byte[] returnMessage = myNXTBTCommunicator.getReturnMessage();
                        startRXEprogram(returnMessage[2]);
                    }
                    
                    break;
                 */                     
                    
            }
        }
    };
    

    void selectNXT() {
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }

}
