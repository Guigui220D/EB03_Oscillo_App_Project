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

/**
 * Activité de choix du device bluetooth à connecter
 */
public class BluetoothConnectActivity extends AppCompatActivity {

    // Enumération des actions de recherche bluetooth
    private enum Action { START, STOP };

    // Adapteur bluetooth
    private BluetoothAdapter m_bluetoothAdapter;

    // Listes
    // Liste des devices appairés
    private ListView m_pairedList;
    // Liste des devices découverts
    private ListView m_discoList;
    // Bouton de scan des devices
    private FloatingActionButton m_scanButton;
    // Icone de chargement circulaire du scan
    private ProgressBar m_progress;
    // Adapteur des devices appairés pour la liste
    private ArrayAdapter<String> m_pairedAdapter;
    // Adapteur des devices scannés pour la liste
    private ArrayAdapter<String> m_discoAdapter;

    // Des devices ont étés trouvés ou non (pour l'affichage d'un message adapté)
    private boolean m_foundAny = false;

    /**
     * Création de l'activité
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connect);

        getSupportActionBar().setTitle("Device connection menu");

        // Création des variables
        m_pairedList = findViewById(R.id.pairedList);
        m_discoList = findViewById(R.id.discoList);
        m_pairedAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        m_discoAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        m_pairedList.setAdapter(m_pairedAdapter);
        m_discoList.setAdapter(m_discoAdapter);

        // Définition des évènements de clic des listes
        m_pairedList.setOnItemClickListener((list, item, c, d) -> {
            TextView t = (TextView)item;
            onItemClick((String)t.getText());
        });
        m_discoList.setOnItemClickListener((list, item, c, d) -> {
            TextView t = (TextView)item;
            onItemClick((String)t.getText());
        });

        // Message par défaut avant le scan
        m_discoAdapter.add("No device found yet");

        // Définition du bouton de scan
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

    /**
     * Fonction de gestion d'un clic sur un device
     * @param device nom et id du device choisi
     */
    private void onItemClick(String device) {
        Log.i("BT", "Chosen " + device);

        // Retour à l'activité précédente avec le choix
        Intent intent = new Intent();
        intent.putExtra("chosen", device);
        setResult(RESULT_OK, intent);

        finish();
    }

    /**
     * Création du menu
     * @param menu The options menu in which you place your items
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu2, menu);
        return true;
    }

    /**
     * Appui sur le bouton retour (sans résultat)
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent intent = new Intent();
        //intent.putExtra -- todo
        setResult(RESULT_OK, intent);
    }

    /**
     * Listener pour l'appui d'un élément de la liste
     * @param item The menu item that was selected
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    /**
     * Routine de scan des devices disponibles (ou d'arrêt)
     * @param startstop
     */
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

    /**
     * Gestion des informations des devices bluetooth découverts
     */
    private BroadcastReceiver m_broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    // Fin de la découverte
                    Log.i("BT", "Discovery finished");
                    btScan(Action.STOP);
                    break;

                case BluetoothDevice.ACTION_FOUND:
                    // Ajout de l'élément à la liste
                    BluetoothDevice dev = intent.getExtras().getParcelable(BluetoothDevice.EXTRA_DEVICE);
                    String id = dev.getAddress();
                    String name = dev.getName();
                    if (name == null)
                        name = "Unknown";
                    String text = name + "\n" + id;

                    Log.i("BT", "Found device " + id);

                    if (!m_foundAny) {
                        // Retrait du message qui dit qu'on a rien trouvé pour le moment
                        m_foundAny = true;
                        m_discoAdapter.clear();
                    }
                    if (m_discoAdapter.getPosition(text) == -1)
                        m_discoAdapter.add(text);

                    break;
            }

            // TODO: interdire la rotation d'écran dans cette activité pour ne pas perdre les variables fragiles
        }
    };

}