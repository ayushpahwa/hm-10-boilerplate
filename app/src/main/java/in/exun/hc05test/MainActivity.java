package in.exun.hc05test;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private EditText input;
    private TextView textReceive;

    private String deviceAddress = "xasd";

    private String receiveTextHistory;
    private boolean isBTServiceBound;

    private BluetoothService btService;

    private ServiceConnection btConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            btService = ((BluetoothService.LocalBinder) service).getService();
            btService.setAddress(deviceAddress);
            btService.attemptConnection();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            btService = null;
        }
    };

    /*
     * Notifications from SocketService will be received here.
     */
    private final BroadcastReceiver mBTReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case GC.ACTION_ADD_TEXT:
                    String data = intent.getStringExtra("data");
                    receiveTextHistory = receiveTextHistory.concat(data);
                    textReceive.setText(receiveTextHistory);
                    break;
            }
        }
    };

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

                                btService.attemptConnection();

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        receiveTextHistory = "Start\n";
        input = findViewById(R.id.input_text);

        Button btnSend = findViewById(R.id.btn_send);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String data = input.getText().toString();
                if (data.length() > 0) {
                    btService.prepareDataToSend(data);
                    input.setText("");
                }
            }
        });

        textReceive = findViewById(R.id.text_received);
        textReceive.setText(receiveTextHistory);

        startBTService(BluetoothService.class,btConnection);
        setBTServiceFilters();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            deviceAddress = extras.getString("address");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBTReceiver);
        if (isBTServiceBound) {
            // Detach our existing connection.
            unbindService(btConnection);
            isBTServiceBound = false;
        }
    }

    private void setBTServiceFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(GC.ACTION_ADD_TEXT);
        registerReceiver(mBTReceiver, filter);
    }

    /**
     * Connects to Socket service
     */
    private void startBTService(Class<?> service, ServiceConnection serviceConnection) {

        if (!BluetoothService.SERVICE_CONNECTED) {
            startService(new Intent(this, service));
        }

        bindService(new Intent(this, service), serviceConnection, Context.BIND_AUTO_CREATE);
        isBTServiceBound = true;
        if (btService != null) {
            btService.IsBoundable();
        }
    }
}
