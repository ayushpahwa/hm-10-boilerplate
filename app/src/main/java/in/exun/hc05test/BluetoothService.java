package in.exun.hc05test;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import co.lujun.lmbluetoothsdk.BluetoothLEController;
import co.lujun.lmbluetoothsdk.base.BluetoothLEListener;
import co.lujun.lmbluetoothsdk.base.State;

public class BluetoothService extends Service {

    private static final String TAG = "SocketService";

    private int writeCounter = 0, readCounter = 0, preState = -1;
    private int workingState = 0; // 0 - not required, 1 - called, 2 - working

    public static boolean SERVICE_CONNECTED = false;

    private String deviceAddress = "xasd";

    private Handler connectCheckHandler = new Handler();

    public BluetoothLEController mBLEController;

    private BluetoothAdapter mBluetoothAdapter;
    private String lastString = "~";

    private char[] dataBits;
    private int dataBitsCounter = 0;
    private Boolean exchangeOn = false;

    public BluetoothLEListener mBluetoothLEListener = new BluetoothLEListener() {

        @Override
        public void onReadData(final BluetoothGattCharacteristic characteristic) {

            Log.d(TAG, "run: Read " + parseData(characteristic) + " " + workingState);
            if (workingState == 1) {
                // Device is now connected. Check for laser alignment
                prepareDataToSend("T");
            }
        }

        @Override
        public void onWriteData(final BluetoothGattCharacteristic characteristic) {


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

                rebuild();
                attemptConnection();
            }

        }

        @Override
        public void onDataChanged(final BluetoothGattCharacteristic characteristic) {

            readCounter++;
            Log.d(TAG, "run: readCounter incremented " + writeCounter + " " + readCounter);

            String message = parseData(characteristic);

            if (readCounter == writeCounter) {
                Log.d(TAG, "run: Read return " + message);


                broadcastText("Read: " + message + "\n");

                connectCheckHandler.removeCallbacksAndMessages(null);

                if (message.equals("0")) {
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

            Log.d(TAG, "onBluetoothServiceStateChanged: " + state);

            writeCounter = 0;
            readCounter = 0;

            if (state == State.STATE_CONNECTING) {

                // Show loading
                Log.d(TAG, "onBluetoothServiceStateChanged: " + preState + " connecting now");
                broadcastText("\nConnecting\n");

            } else if (state == State.STATE_CONNECTED || state == State.STATE_GOT_CHARACTERISTICS) {
                Log.d(TAG, "onBluetoothServiceStateChanged: Connected");
                broadcastText("\nConnected\n");

                // Remove any shown errors

            } else if (state == State.STATE_DISCONNECTED) {
                Log.d(TAG, "onBluetoothServiceStateChanged: Disconnected");
                broadcastText("\nDisconnected\nRetrying\n");
                attemptConnection();
                //Show errors

            }

        }

        @Override
        public void onActionDeviceFound(BluetoothDevice device, short rssi) {
        }

    };

    private void broadcastText(String data) {
        Intent intent = new Intent().setAction(GC.ACTION_ADD_TEXT);
        intent.putExtra("data",data);
        sendBroadcast(intent);
    }

    public void attemptConnection() {

        // SHOW LOADING ANIMATION

        if (btCheck()) { // Returns true if bt was off and it just turned it on

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    if (mBLEController == null) {
                        mBLEController = BluetoothLEController.getInstance().build(BluetoothService.this);
                        mBLEController.setBluetoothListener(mBluetoothLEListener);
                    }
                    workingState = 1;
                    mBLEController.connect(deviceAddress);

                }
            }, 5000);

        } else {

            if (mBLEController == null) {
                mBLEController = BluetoothLEController.getInstance().build(this);
                mBLEController.setBluetoothListener(mBluetoothLEListener);
            }
            workingState = 1;
            mBLEController.connect(deviceAddress);
        }

    }

    //    Checks if  bluetooth is turned on.
    public boolean btCheck() {

        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
            return true;
        }

        return false;
    }

    private void rebuild() {

        mBLEController.disconnect();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mBLEController.release();
                mBLEController = BluetoothLEController.getInstance().build(BluetoothService.this);
                mBLEController.setBluetoothListener(mBluetoothLEListener);

            }
        }, 500);

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
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    private final IBinder myBinder = new LocalBinder();

    public void IsBoundable() {}

    public void setAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public class LocalBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate: ");
        // Setting up bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mBLEController = BluetoothLEController.getInstance().build(this);
        mBLEController.setBluetoothListener(mBluetoothLEListener);
    }

    public void prepareDataToSend(String data) {
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
            broadcastText("\nWrite: " + data + "\n");
        }
    }

    private void reconnect() {

        if (mBLEController == null) {
            mBLEController = BluetoothLEController.getInstance().build(this);
            mBLEController.setBluetoothListener(mBluetoothLEListener);
        }
        mBLEController.reConnect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBLEController.disconnect();
    }
}
