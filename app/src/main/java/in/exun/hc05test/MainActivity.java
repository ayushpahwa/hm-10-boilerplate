package in.exun.hc05test;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import co.lujun.lmbluetoothsdk.BluetoothLEController;
import co.lujun.lmbluetoothsdk.base.BluetoothLEListener;
import co.lujun.lmbluetoothsdk.base.State;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private int writeCounter = 0, readCounter = 0, preState = -1;
    private int workingState = 0; // 0 - not required, 1 - called, 2 - working

    private boolean FLAG_DO_NOTHING = false;

    private EditText input;
    private Button btnSend;
    private TextView textReceive;

    private String deviceAddress = "xasd";

    private Handler connectCheckHandler = new Handler();

    public BluetoothLEController mBLEController;

    private BluetoothAdapter mBluetoothAdapter;
    private String lastString = "~";
    public BluetoothLEListener mBluetoothLEListener = new BluetoothLEListener() {

        @Override
        public void onReadData(final BluetoothGattCharacteristic characteristic) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                        }
                    });
                    Log.d(TAG, "run: Read " + parseData(characteristic) + " " + workingState);
                    if (workingState == 1) {

                        // Device is now connected. Check for laser alignment
                        prepareDataToSend("T");

                        connectCheckHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {

                                Log.d(TAG, "run: No reply for 3 secs");
                                FLAG_DO_NOTHING = true;
                                rebuild();
                                showProblemInConnection();
                            }
                        }, 3000);
                    }
                }
            });
        }

        @Override
        public void onWriteData(final BluetoothGattCharacteristic characteristic) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    Log.d(TAG, "run: Write" + ": " + parseData(characteristic) + "\n");

                    if (writeCounter <= readCounter) {

                        readCounter = 0;
                        writeCounter = 1;
                        lastString = parseData(characteristic);
                        if (workingState == 1) {
                            workingState = 2;
                        } else {
                            connectCheckHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(TAG, "run: No response from bluetooth");
                                }
                            }, 5000);
                        }

                    } else {
                        connectCheckHandler.removeCallbacksAndMessages(null);

                        FLAG_DO_NOTHING = true;
                        rebuild();
                        mBLEController.connect(deviceAddress);
                    }
                }
            });
        }

        @Override
        public void onDataChanged(final BluetoothGattCharacteristic characteristic) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {


                    readCounter++;
                    Log.d(TAG, "run: readCounter incremented " + writeCounter + " " + readCounter);

                    String message = parseData(characteristic);

                    if (readCounter == writeCounter) {
                        Log.d(TAG, "run: Read return " + message);

                        receiveTextHistory = receiveTextHistory.concat("Read: " + message + "\n");
                        textReceive.setText(receiveTextHistory);

                        connectCheckHandler.removeCallbacksAndMessages(null);

                        if (message.equals("0")) {
                            lastString = "!";
                            if (exchangeOn) {
                                exchangeOn = false;
                            }
                        }

                        if (exchangeOn) {
                            if (++dataBitsCounter < dataBits.length) {
                                writeToBle(dataBits[dataBitsCounter]);
                            } else {
                                exchangeOn = false;
                            }
                        }
                    }
                }
            });

        }

        @Override
        public void onActionStateChanged(int preState, int state) {

        }

        @Override
        public void onActionDiscoveryStateChanged(String discoveryState) {

        }

        @Override
        public void onActionScanModeChanged(int preScanMode, int scanMode) {

        }

        @Override
        public void onBluetoothServiceStateChanged(final int state) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    Log.d(TAG, "onBluetoothServiceStateChanged: " + state);

                    writeCounter = 0;
                    readCounter = 0;

                    if (state == State.STATE_CONNECTING) {

                        // Show loading
                        Log.d(TAG, "onBluetoothServiceStateChanged: " + preState + " connecting now");
                        receiveTextHistory = receiveTextHistory.concat("\nConnecting\n");

                    } else if (state == State.STATE_CONNECTED || state == State.STATE_GOT_CHARACTERISTICS) {
                        Log.d(TAG, "onBluetoothServiceStateChanged: Connected");
                        receiveTextHistory = receiveTextHistory.concat("\nConnected\n");

                        // Remove any shown errors

                    } else if (state == State.STATE_DISCONNECTED) {
                        Log.d(TAG, "onBluetoothServiceStateChanged: Disconnected");
                        receiveTextHistory = receiveTextHistory.concat("\nDisconnected\nRetrying\n");
                        attemptConnection(deviceAddress);
                        //Show errors

                    }

                }
            });
        }

        @Override
        public void onActionDeviceFound(BluetoothDevice device, short rssi) {

        }
    };
    private char[] dataBits;
    private int dataBitsCounter = 0;
    private Boolean exchangeOn = false;
    private String receiveTextHistory;

    private void showProblemInConnection() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Connection failed");
                builder.setMessage("Your device may be offline or out of range. Please check and retry!");
                builder.setCancelable(false)
                        .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                attemptConnection(deviceAddress);

                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                dialog.dismiss();
                                // Reset everything else
                            }
                        }).show();
            }
        });
    }

    private void attemptConnection(final String address) {


        FLAG_DO_NOTHING = false;

        // SHOW LOADING ANIMATION

        if (btCheck()) { // The fuction btCheck() checks if  bluetooth is turned on. Returns true if bt was off and it just turned it on

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    deviceAddress = address;

                    if (mBLEController == null) {
                        mBLEController = BluetoothLEController.getInstance().build(MainActivity.this);
                        mBLEController.setBluetoothListener(mBluetoothLEListener);
                    }
                    workingState = 1;
                    mBLEController.connect(deviceAddress);

                }
            }, 5000);
        } else {

            deviceAddress = address;

            if (mBLEController == null) {
                mBLEController = BluetoothLEController.getInstance().build(MainActivity.this);
                mBLEController.setBluetoothListener(mBluetoothLEListener);
            }
            workingState = 1;
            mBLEController.connect(deviceAddress);
        }


    }

    public boolean btCheck() {

        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
            return true;
        }

        return false;
    }

    private void rebuild() {

        mBLEController.disconnect();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBLEController.release();
                        mBLEController = BluetoothLEController.getInstance().build(MainActivity.this);
                        mBLEController.setBluetoothListener(mBluetoothLEListener);

                    }
                }, 500);

            }
        });
    }

    private String parseData(BluetoothGattCharacteristic characteristic) {
        String result = "";
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            result = new String(data);
        }
        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setting up bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mBLEController = BluetoothLEController.getInstance().build(this);
        mBLEController.setBluetoothListener(mBluetoothLEListener);

        receiveTextHistory = "Start\n";
        input = findViewById(R.id.input_text);

        btnSend = findViewById(R.id.btn_send);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String data = input.getText().toString();
                if (data.length() > 0) {
                    prepareDataToSend(data);
                    input.setText("");
                }
            }
        });

        textReceive = findViewById(R.id.text_received);
        textReceive.setText(receiveTextHistory);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            deviceAddress = extras.getString("address");
            if (btCheck()) {

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        workingState = 1;
                        mBLEController.connect(deviceAddress);
                    }
                }, 5000);
            } else {
                workingState = 1;
                mBLEController.connect(deviceAddress);
            }
        }
    }

    private void prepareDataToSend(String data) {
        dataBitsCounter = 0;
        dataBits = data.toCharArray();
        exchangeOn = true;
        writeToBle(dataBits[dataBitsCounter]);
    }

    private void writeToBle(char data) {
        if (!(mBLEController.getConnectionState() == State.STATE_GOT_CHARACTERISTICS ||
                mBLEController.getConnectionState() == State.STATE_CONNECTED)) {
            if (btCheck()) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        reconnect();
                    }
                }, 5000);
            } else
                reconnect();
        } else {
            mBLEController.write(new byte[]{(byte) data});
            receiveTextHistory = receiveTextHistory.concat("\nWrite: " + data + "\n");
            textReceive.setText(receiveTextHistory);
        }
    }

    private void reconnect() {

        if (mBLEController == null) {
            mBLEController = BluetoothLEController.getInstance().build(MainActivity.this);
            mBLEController.setBluetoothListener(mBluetoothLEListener);
        }
        mBLEController.reConnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBLEController.disconnect();
    }
}
