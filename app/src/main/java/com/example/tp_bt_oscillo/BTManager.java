package com.example.tp_bt_oscillo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Implémentation de Transciever pour la connexion à un appareil bluetooth
 * @author Guillaume DEREX
 */
public class BTManager extends Transceiver {

    // Unique UUID for this application (set the SPP UUID because expected incomming connection are of this type)
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // reference vers l'adaptateur
    private final BluetoothAdapter mAdapter;

    // réferences vers les Threads
    private BluetoothSocket mSocket = null;
    private ConnectThread mConnectThread = null;
    private ReadingThread mReadingThread = null;
    private WritingThread mWritingThread = null;

    // Constructeur par défaut
    public BTManager() {
        // Obtention de l'adapter
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        // Création du frame processor (todo: faire autrement)
        frameProcessor = new FrameProcessor();
    }

    /**
     * Implémentation de la fonction connect (démarre un thread de connexion)
     * @param id: addresse de l'appareil bluetooth à joindre
     */
    @Override
    public void connect(String id) {

        // TODO: Annuler les éventuelles demandes de connexion en cours

        // Création et démarrage du thread
        mConnectThread = new ConnectThread(id);
        mConnectThread.start();
    }

    /**
     * Implémentation de la déconnexion
     * Tue les threads associés et ferme le socket
     */
    @Override
    public void disconnect() {
        // Suppression des threads (les threads vérifient leur propre référence donc s'arrêteront)
        mConnectThread = null;
        mReadingThread = null;
        mWritingThread = null;

        // Fermeture du socket
        if (mSocket != null) {
            try {
                mSocket.close();
                mSocket = null;
            } catch (IOException e) {}
        }
    }


    /**
     * Implémentation de l'envoi de données pour l'appareil bluetooth associé
     * L'envoi utilise un buffer circulaire et est asynchrone
     * Les données à envoyer sont traitées par le frame processor
     * @param data: les octets à envoyer
     */
    @Override
    public void send(byte[] data) {
        if (mWritingThread != null) {
            try {
                // Envoi des données au thread d'envoi
                mWritingThread.write(frameProcessor.toFrame(data));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Thread de connexion
    private class ConnectThread extends Thread {

        // Adresse bluetooth à joindre
        private String mId;

        /**
         * Constructeur du thread de connexion
         * @param id: adresse bluetooth à joindre
         */
        public ConnectThread(String id) {
            super();
            mId = id;
        }

        /**
         * Code du thread de connexion
         */
        @Override
        public void run() {
            BluetoothDevice bluetoothDevice = mAdapter.getRemoteDevice(mId);

            // Connexion au socket
            try {
                Log.i("BTConnect", "Trying to connect to " + bluetoothDevice.getName());
                mSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
                Log.i("BTConnect", "Got socket");
                mSocket.connect();
                Log.i("BTConnect", "Connected");
            } catch (IOException e) {
                // Signalisation au listener de l'échec
                connectionFailed();
                return;
            }

            // TODO: stopper éventuelle découverte lancée

            // Création des threads d'envoi et de réception
            mWritingThread = new WritingThread();
            mReadingThread = new ReadingThread();

            mWritingThread.start();
            mReadingThread.start();

            // Signalisation au listener du succes
            connectionSucceed();

            // Fin du thread de connexion
        }
    }



    /************************************************************************************
     /
     /                          THREADS de COMMUNICATION
     /
     /***********************************************************************************/


    // Thread d'envoi des données
    private class WritingThread extends Thread {
        // buffer circulaire d'envoi
        private ByteRingBuffer mBuffer;
        // Stream (du socket bluetooth)
        private OutputStream mStream;

        /**
         * Construteur du writing thread
         */
        public WritingThread() {
            super();

            // Création du buffer circulaire
            mBuffer = new ByteRingBuffer(1024);
            try {
                // Obtention du stream
                mStream = mSocket.getOutputStream();
            } catch (IOException e) {
                // Signalisation au listener de l'échec
                connectionLost();
            }
        }

        /**
         * Boucle d'envoi de données
         */
        @Override
        public void run() {
            while (mWritingThread == this) {
                // Obtention de toutes les données du buffer circulaire
                byte[] bytes = mBuffer.getAll();

                if (bytes.length != 0) {
                    try {
                        // Envoi
                        mStream.write(bytes);
                    } catch (IOException e) {
                        // Signalisation au listener de l'échec
                        connectionLost();
                    }
                }
            }
        }

        /**
         * Ajout de données au buffer circulaire
         * Utilisé par send du Transciever
         * @param data: octets à envoyer
         */
        public void write(byte[] data) {
            mBuffer.put(data);
        }
    }

    // Thread de réception des données
    // Classe non finie, non testée
    private class ReadingThread extends Thread {
        // Buffer circulaire de réception
        private ByteRingBuffer mBuffer;
        // Stream (du socket bluetooth)
        private InputStream mStream;
        public ReadingThread() {
            super();

            // Création du buffer circulaire
            mBuffer = new ByteRingBuffer(1024);
            try {
                // Obtention du stream
                mStream = mSocket.getInputStream();
            } catch (IOException e) {
                // Signalisation au listener de l'échec
                connectionLost();
            }
        }

        /**
         * Boucle de réception de données
         * NON IMPLEMENTE
         * Cette boucle doit recevoir des données du socket, décoder les trames, et les présenter
         * à une fonction de lecture
         */
        @Override
        public void run() {
            // TODO
            while (mReadingThread == this) {
                /*
                int free = mBuffer.freeSpace();
                byte[] bytes = mStream.readNBytes(free)
                try {
                    mStream.write(bytes);
                } catch (IOException e) {
                    connectionLost();
                }
                */
            }
        }
    }
}
