package com.example.tp_bt_oscillo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothConnectActivity extends AppCompatActivity {

    private enum Action { START, STOP };

    private BluetoothAdapter m_bluetoothAdapter;
    private boolean m_broadcastRegistered = false;

    private ListView m_pairedList;
    private ListView m_discoList;
    private FloatingActionButton m_scanButton;
    private ProgressBar m_progress;
    private ArrayAdapter<String> m_pairedAdapter;
    private ArrayAdapter<String> m_discoAdapter;
    private boolean m_foundAny = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connect);

        getSupportActionBar().setTitle("Device connection menu");

        m_pairedList = findViewById(R.id.pairedList);
        m_discoList = findViewById(R.id.discoList);
        m_pairedAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        m_discoAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        m_pairedList.setAdapter(m_pairedAdapter);
        m_discoList.setAdapter(m_discoAdapter);

        m_pairedList.setOnItemClickListener((list, item, c, d) -> {
            TextView t = (TextView)item;
            onItemClick((String)t.getText());
        });
        m_discoList.setOnItemClickListener((list, item, c, d) -> {
            TextView t = (TextView)item;
            onItemClick((String)t.getText());
        });

        m_discoAdapter.add("No device found yet");

        m_scanButton = findViewById(R.id.scanButton);
        m_scanButton.setOnClickListener((a) -> btScan(Action.START));

        m_progress = findViewById(R.id.progressBar);

        // création de la liste des périphériques liés
        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(m_bluetoothAdapter != null) {
            Set<BluetoothDevice> pairedDevices = m_bluetoothAdapter.getBondedDevices();

            if (pairedDevices.size() > 0) {
                for (BluetoothDevice pairedDevice : pairedDevices) {
                    m_pairedAdapter.add(pairedDevice.getName() + "\n" + pairedDevice.getAddress());
                }
            } else {
                m_pairedAdapter.add("No paired device available");
            }
        }
    }

    private void onItemClick(String device) {
        Log.i("BT", "Chosen " + device);

        Intent intent = new Intent();
        intent.putExtra("chosen", device);
        setResult(RESULT_OK, intent);

        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu2, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent intent = new Intent();
        //intent.putExtra -- todo
        setResult(RESULT_OK, intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    private void btScan(Action startstop) {
        if (startstop == Action.START) {
            Log.i("BT", "Start scan");

            IntentFilter filter = new IntentFilter();

            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            filter.addAction(BluetoothDevice.ACTION_FOUND);

            registerReceiver(m_broadcastReceiver, filter);

            m_discoAdapter.clear();
            m_discoAdapter.add("No device found yet");
            m_foundAny = false;
            m_bluetoothAdapter.startDiscovery();

            m_scanButton.setVisibility(View.GONE);
            m_progress.setVisibility(View.VISIBLE);
        } else { // Action.STOP
            m_scanButton.setVisibility(View.VISIBLE);
            m_progress.setVisibility(View.GONE);
        }
    }
    private BroadcastReceiver m_broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Log.i("BT", "Discovery finished");
                    btScan(Action.STOP);
                    break;

                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice dev = intent.getExtras().getParcelable(BluetoothDevice.EXTRA_DEVICE);
                    String id = dev.getAddress();
                    String name = dev.getName();
                    if (name == null)
                        name = "Unknown";
                    String text = name + "\n" + id;

                    Log.i("BT", "Found device " + id);

                    if (!m_foundAny) {
                        m_foundAny = true;
                        m_discoAdapter.clear();
                    }
                    if (m_discoAdapter.getPosition(text) == -1)
                        m_discoAdapter.add(text);

                    break;
            }

            // TODO: forbid screen rotation in this activity
        }
    };

}