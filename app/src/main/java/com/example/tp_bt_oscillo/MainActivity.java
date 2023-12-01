package com.example.tp_bt_oscillo;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.Manifest;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setTitle("BT Oscilloscope App");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
       getMenuInflater().inflate(R.menu.menu, menu);
       return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Log.i("MENU", "Pressed the bluetooth button");
        verifyBtPermissions();
        return super.onOptionsItemSelected(item);
    }

    private void verifyBtPermissions() {
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            Toast.makeText(this, "This app requires a bluetooth adapter", Toast.LENGTH_LONG).show();
            return;
        }

        if (!hasBTPermissions()) {
            Log.i("PERMS", "Not granted yet");
            btPermissionLauncher.launch(new String[]{ Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT });
        } else {
            Intent intent = new Intent(MainActivity.this, BluetoothConnectActivity.class);
            btConnectLauncher.launch(intent);
        }
    }

    private boolean hasBTPermissions() {
        String[] permissions = new String[]{ Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT };
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

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

    private final ActivityResultLauncher<String[]> btPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
        result -> {
            if (result.containsValue(false)) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN)) {
                    showDialog("Permission Needed", "We need your permission to use the bluetooth",
                        "Request again", (dialog, i) -> {
                            requestAgain();
                        },
                        "Don't allow", (dialog, i) -> {
                                Toast.makeText(this, "Permissions for bluetooth were refused", Toast.LENGTH_LONG).show();
                        }, false);
                } else {
                    Toast.makeText(this, "Permission was refused too many times, please enable them in the settings", Toast.LENGTH_LONG).show();
                }
            }
        });

    private final ActivityResultLauncher<Intent> btConnectLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            o -> {

            }
    );

    private void requestAgain() {
        btPermissionLauncher.launch(new String[]{ Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT });
    }
}