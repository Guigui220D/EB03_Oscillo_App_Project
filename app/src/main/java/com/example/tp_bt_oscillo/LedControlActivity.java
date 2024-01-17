package com.example.tp_bt_oscillo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Activité de contrôle de la led
 */
public class LedControlActivity extends AppCompatActivity {

    // Slider circulaire pour le contrôle de la led
    private CircularDial dial;

    /**
     * Création de la télécommande
     * Mise en place de la mécanique de changement de luminiosité
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_led_control);

        OscilloManager oscilloManager = OscilloManager.getInstance();

        oscilloManager.setListener(new OscilloManager.OscilloEventsListener() {
            @Override
            public void onOscilloConnected() {

            }

            @Override
            public void onOscilloDisconnected() {

            }

            @Override
            public void onOscilloLostConnection() {
                runOnUiThread(() -> {
                    // On quitte cet état si on perd la connexion
                    // TODO: le toast ne s'affiche pas
                    Toast.makeText(LedControlActivity.this, "Lost connection", Toast.LENGTH_LONG);
                    LedControlActivity.super.onBackPressed();
                });
            }
        });

        dial = findViewById(R.id.circularDial);
        dial.setSliderChangeListener((val) -> {
            // Lorsque le bouton circulaire est changé, on appelle la fonction du oscillo manager
            oscilloManager.setLedBrightness((int)(val * 100.f));
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        OscilloManager.getInstance().disconnect();
    }
}