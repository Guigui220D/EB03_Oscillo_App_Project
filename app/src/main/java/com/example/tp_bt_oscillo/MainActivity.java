package com.example.tp_bt_oscillo;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;
import android.Manifest;

/**
 * Activité principale (présente le bouton de connexion et les instructions)
 * @author Guillaume DEREX
 */
public class MainActivity extends AppCompatActivity {
    /**
     * Création de cette activité
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialization du oscillo manager
        // TODO: mettre ailleurs
        new OscilloManager();

        setContentView(R.layout.activity_main);

        getSupportActionBar().setTitle("BT Oscilloscope App");
    }

    /**
     * Retour à cette activité
     * On récupère le listener du oscillomanager
     */
    @Override
    public void onResume() {
        super.onResume();
        OscilloManager.getInstance().setListener(new OscilloManager.OscilloEventsListener() {
            @Override
            public void onOscilloConnected() {
                // Lorsque le oscillo manager est connecté, on peut passr à l'activité télécommande
                runOnUiThread(() -> {
                    Intent intent = new Intent(MainActivity.this, LedControlActivity.class);
                    btRemoteLauncher.launch(intent);
                });
            }

            @Override
            public void onOscilloDisconnected() {

            }

            @Override
            public void onOscilloLostConnection() {

            }
        });
    }

    /**
     * Création du menu
     * @param menu The options menu in which you place your items
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
       getMenuInflater().inflate(R.menu.menu, menu);
       return true;
    }

    /**
     * Gestion des évènements du menu
     * @param item The menu item that was selected.
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Log.i("MENU", "Pressed the bluetooth button");
        verifyBtPermissions();
        return super.onOptionsItemSelected(item);
    }

    /**
     * Vérification des permissions bluetooth et passage au choix de connexions si c'est possible
     */
    private void verifyBtPermissions() {
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            // Alerte si abscence d'adapter bluetooth
            Toast.makeText(this, "This app requires a bluetooth adapter", Toast.LENGTH_LONG).show();
            return;
        }

        if (!hasBTPermissions()) {
            // Demande des permissions
            Log.i("PERMS", "Not granted yet");
            btPermissionLauncher.launch(new String[]{ Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT });
        } else {
            // Ouverture de l'activité de connexion
            Intent intent = new Intent(MainActivity.this, BluetoothConnectActivity.class);

            // On déconnecte le transciever actuel (si nécessaire)
            OscilloManager.getInstance().disconnect();
            btConnectLauncher.launch(intent);
        }
    }

    /**
     * Vérification des permissions nécessaires
     * @return true si on a les permissions bluetooth nécessaires
     */
    private boolean hasBTPermissions() {
        String[] permissions = new String[]{ Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT };
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    /**
     * Présentation d'un menu générique à deux choix
     * @param title Titre affiché
     * @param message Message affiché
     * @param positivelabel Texte du bouton positif
     * @param positiveOnClick Callback du bouton positif cliqué
     * @param negativelabel Texte du bouton négatif
     * @param negativeOnClick Callback du bouton négatif cliqué
     * @param isCancelable Vrai si peut être annulé
     * @return Référence au dialogue
     */
    private AlertDialog showDialog(String title,
                                   String message,
                                   String positivelabel,
                                   DialogInterface.OnClickListener positiveOnClick,
                                   String negativelabel,
                                   DialogInterface.OnClickListener negativeOnClick,
                                   boolean isCancelable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(isCancelable);
        builder.setPositiveButton(positivelabel,positiveOnClick);
        builder.setNegativeButton(negativelabel,negativeOnClick);
        AlertDialog alert = builder.create();
        alert.show();
        return alert;
    }

    /**
     * Demande de permissions bluetooth (et gestion du résultat)
     */
    private final ActivityResultLauncher<String[]> btPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
        result -> {
            if (result.containsValue(false)) {
                // En cas de refus
                if (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN)) {
                    // Proposition de 2eme tentative
                    showDialog("Permission Needed", "We need your permission to use the bluetooth",
                        "Request again", (dialog, i) -> {
                            requestAgain();
                        },
                        "Don't allow", (dialog, i) -> {
                                // Refus catégorique
                                Toast.makeText(this, "Permissions for bluetooth were refused", Toast.LENGTH_LONG).show();
                        }, false);
                } else {
                    // On ne peut plus proposer les permissions
                    Toast.makeText(this, "Permission was refused too many times, please enable them in the settings", Toast.LENGTH_LONG).show();
                }
            }
        });

    /**
     * Launcher du menu de connexion bluetooth
     * Et gestion du résultat
     */
    private final ActivityResultLauncher<Intent> btConnectLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            o -> {
                // Obtention du choix de l'utilisateur
                String chosen = o.getData().getStringExtra("chosen");
                // On tronque la deuxième ligne car la première est le nom de l'appareil (ne sert pas)
                String id = chosen.split("\n")[1];
                Log.i("BTConnect", "Got: \"" + id + "\"");

                // On vérifie les permsisions
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.i("BTConnect", "No permission");
                    return;
                }

                // En cas de succès, on tente la connexion depuis l'oscillo manager
                // Le listener de celui ci prendra en compte la réussite de connexion
                OscilloManager.getInstance().connect(id);
            }
    );

    /**
     * Launcher de la télécommande
     */
    public final ActivityResultLauncher<Intent> btRemoteLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            o -> {

            }
    );

    /**
     * Demande à nouveau des permissions
     */
    private void requestAgain() {
        btPermissionLauncher.launch(new String[]{ Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT });
    }
}