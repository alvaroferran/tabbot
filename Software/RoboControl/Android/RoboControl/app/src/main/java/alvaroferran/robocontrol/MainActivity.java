package alvaroferran.robocontrol;

import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder sb = new StringBuilder();
    private static String address = "00:12:02:10:00:78"; // MAC-address of Bluetooth module
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String TAG = "bluetooth"; //solo para log
    final int RECEIVE_MESSAGE = 1;
    private ConnectedThread myConnectedThread;
    private boolean bluetoothConnected=false;

    Button bluetoothButton, Up, Down, Left, Right,Save;
    static Handler btInputHandler;
    private String inMessage;



    /********ON CREATE**************************************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

        Up=(Button) findViewById(R.id.btnUp);
        Down=(Button) findViewById(R.id.btnDown);
        Left=(Button) findViewById(R.id.btnLeft);
        Right=(Button) findViewById(R.id.btnRight);
        Save=(Button) findViewById(R.id.btnSave);

        bluetoothButton= (Button) findViewById(R.id.bluetoothButton);

        btInputHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECEIVE_MESSAGE:                                      // if receive massage
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1);    // create string from bytes array
                        sb.append(strIncom);                                   // append string
                        int endOfLineIndex = sb.indexOf("\r\n");               // determine the end-of-line
                        if (endOfLineIndex > 0) {                              // if end-of-line,
                            String sbprint = sb.substring(0, endOfLineIndex);  // extract string
                            sb.delete(0, sb.length());                         // and clear
                            inMessage=sbprint;
                            if(inMessage.contains("Connected") )
                                sendBT("Control");
                        }
                        break;
                }}};






    }


    /********MAIN FUNCTION**********************************************************************************/
    private void mainFunction(){

        Up.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(bluetoothConnected) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN)
                        sendBT("0");
                    else if (event.getAction() == MotionEvent.ACTION_UP)
                        sendBT("4");
                }
                return false;
            }
        });

        Down.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(bluetoothConnected) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN)
                        sendBT("1");
                    else if (event.getAction() == MotionEvent.ACTION_UP)
                        sendBT("4");
                }
                return false;
            }
        });

        Left.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(bluetoothConnected) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN)
                        sendBT("2");
                    else if (event.getAction() == MotionEvent.ACTION_UP)
                        sendBT("4");
                }
                return false;
            }
        });

        Right.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(bluetoothConnected) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN)
                        sendBT("3");
                    else if (event.getAction() == MotionEvent.ACTION_UP)
                        sendBT("4");
                }
                return false;
            }
        });


        Save.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v) {
                if(bluetoothConnected)
                    sendBT("5");
            }
        });

    }








    /********ON RESUME**************************************************************************************/
    @Override
    public void onResume() {
        super.onResume();

        bluetoothButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v) {
                bluetoothButton.setVisibility(View.INVISIBLE);
                connectBT();
            }
        });

        mainFunction();
    }



    /********ON PAUSE***************************************************************************************/
    @Override
    public void onPause() {
        super.onPause();
        disconnectBT();
    }


    /********CHECK BT STATE*********************************************************************************/
    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            errorExit("Fatal Error", "Bluetooth not support");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }


    /********CREATE BT SOCKET*******************************************************************************/
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if(Build.VERSION.SDK_INT >= 10){
            try {
                BluetoothSocket mBSocket;
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
//                mBSocket=device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
//                mBSocket.connect();
//                return mBSocket;
                return (BluetoothSocket) m.invoke(MY_UUID,device);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection", e);
                //Toast.makeText(getBaseContext(),"Could not create socket connection", Toast.LENGTH_LONG).show();
            }
        }
        return  device.createRfcommSocketToServiceRecord(MY_UUID);
    }


    /********CONNECT TO BT**********************************************************************************/
    public void connectBT(){
        BluetoothDevice device = btAdapter.getRemoteDevice(address);    //Pointer to BT in Robot

        try {
            btSocket = createBluetoothSocket(device);   //Create Socket to Device
        } catch (IOException e1) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e1.getMessage() + ".");
        }

        btAdapter.cancelDiscovery();    //Discovery consumes resources -> Cancel before connecting

        try {
            btSocket.connect();     //Connect to Robot
        } catch (IOException e) {
            try {
                btSocket.close();   //If unable to connect, close socket
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        bluetoothConnected=true;

        myConnectedThread = new ConnectedThread(btSocket);
        myConnectedThread.start();


    }


    /********DISCONNECT BT**********************************************************************************/
    public void disconnectBT(){
        if (bluetoothConnected) {
            if (myConnectedThread.outStream != null) {
                try {
                    myConnectedThread.outStream.flush();  //If output stream is not empty, send data
                } catch (IOException e) {
                    errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
                }
            }

            try {
                btSocket.close();   //Close socket
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
            }
            bluetoothConnected=false;
        }
    }

    /********SEND DATA**************************************************************************************/
    public void sendBT(String message) {
        byte[] msgBuffer = message.getBytes();

        try {
            myConnectedThread.outStream.write(msgBuffer);
            myConnectedThread.outStream.flush();

        } catch (IOException e) {
            String msg= "Phone not connected to client's Bluetooth";
            errorExit("Fatal Error", msg);
        }
    }



    /********ERROR EXIT*************************************************************************************/
    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }




    ///******THREAD CLASS*********************************************************************************///
    private class ConnectedThread extends Thread {
        private final InputStream inStream;
        private final OutputStream outStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            inStream = tmpIn;
            outStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = inStream.read(buffer);        // Get number of bytes and message in "buffer"
                    btInputHandler.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget();     // Send to message queue Handler
                } catch (IOException e) {
                    break;
                }
            }
        }

    }


}
