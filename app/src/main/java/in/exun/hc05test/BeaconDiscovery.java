package in.exun.hc05test;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class BeaconDiscovery extends AppCompatActivity {

    // LOG TAG
    private static final String TAG = "BeaconDiscovery";
    private static final int PROCESS_RESET = 0;
    private static final int PROCESS_SUCCESS = 1;
    private static final int PROCESS_FAILURE = 2;
    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1;

    // Bluetooth connection variables
    private List<BluetoothDevice> bleList;
    private ArrayList<String> foundDeviceId = new ArrayList<String>();
    private Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;

    // UI elements
    private TextView infoText;
    private ImageView btnRetry;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    // Device scan callback. Executed when scan is started and a device is found
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

            if (device.getName() != null && device.getName().length() > 0) {
                addToArray(device);
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon_discovery);



        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        init();

        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUI(PROCESS_RESET);
                btCheck();
            }
        });
    }

    private void init() {

        // Handler to run delayed code
        mHandler = new Handler(Looper.getMainLooper());

        // Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Linking to UI elements
        infoText = (TextView) findViewById(R.id.info_text);
        mRecyclerView = (RecyclerView) findViewById(R.id.rv_ble);
        btnRetry = (ImageView) findViewById(R.id.btn_retry);
    }

    // Add new found device to bleList
    private void addToArray(BluetoothDevice device) {

        Log.d(TAG, "addToArray: " + device.getName());

        // foundDeviceID array contains the addresses of already found devices so that they are not repeated
        if (!foundDeviceId.contains(device.getAddress())) {


            Log.d(TAG, "Adding " + device.getName());

            updateUI(PROCESS_SUCCESS);

            // Update lists with the new unique device
            foundDeviceId.add(device.getAddress());
            bleList.add(device);

            // Hide the loading view and show the recyclerView
            infoText.setVisibility(View.GONE);

            // Notify adapter of data changes
            mAdapter.notifyDataSetChanged();

        }

    }

    // Function to handle the UI updates
    private void updateUI(int requestCode) {

        switch (requestCode) {
            case PROCESS_RESET:
                infoText.setVisibility(View.VISIBLE);
                infoText.setText("Scanning for devices...");
                btnRetry.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.GONE);
                break;
            case PROCESS_SUCCESS:
                infoText.setVisibility(View.GONE);
                btnRetry.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
                break;
            case PROCESS_FAILURE:
                infoText.setVisibility(View.VISIBLE);
                infoText.setText("No device found!");
                btnRetry.setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);
                break;
        }
    }

    // Function to initialize and reset RecyclerView and it's adapter
    private void populateBle() {

        bleList = new ArrayList<BluetoothDevice>();
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new RVBLE(this, bleList);
        mRecyclerView.setAdapter(mAdapter);

    }

    // Function responsible for finding BLE nearby
    public void findBLE(final boolean enable) {

        // Enable variable to start/stop mHandler callbacks

        if (enable) {

            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    // Executed after SCAN_PERIOD time of scanning for BLE

                    // Stop BLE scan
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);

                    // If loader is not already hidden by the earlier devices found, hide it here
                    infoText.setVisibility(View.GONE);

                    // If list size is greater than zero then reset adapter for the final list data
                    if (bleList.size() > 0)
                        mAdapter.notifyDataSetChanged();
                    else    // Else update UI to show no BLE was found
                        updateUI(PROCESS_FAILURE);

                }
            }, SCAN_PERIOD);

            // Start BLE scan
            mBluetoothAdapter.startLeScan(mLeScanCallback);

        } else {

            // Executed if the scan has to be stopped

            // Stop BLE scan
            mBluetoothAdapter.stopLeScan(mLeScanCallback);

            // Remove the handler callback
            mHandler.removeCallbacksAndMessages(null);

        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Initialize recyclerView
        populateBle();


        // Reset UI
        updateUI(PROCESS_RESET);

            btCheck();



        // Capture RecyclerView custom onItemClickListener event from RecyclerView.Adapter
        ((RVBLE) mAdapter).setOnItemClickListener(new RVBLE.MyClickListener() {
            @Override
            public void onItemClick(int position, View v) {

                Intent i;

                i = new Intent(BeaconDiscovery.this, MainActivity.class);
                i.putExtra("name", bleList.get(position).getName());
                i.putExtra("address", bleList.get(position).getAddress());

                startActivity(i);
                finish();

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    btCheck();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }


    private void btCheck() {

        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    // Start BLE discovery
                    findBLE(true);

                }
            }, 5000);
        } else
            // Start BLE discovery
            findBLE(true);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop BLE Discovery and callbacks
        findBLE(false);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();
    }
}
