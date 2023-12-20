package com.example.tp_bt_oscillo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

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
        mAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void connect(String id) {

        // TODO: Annuler les éventuelles demandes de connexion en cours

        // Création du thread
        mConnectThread = new ConnectThread(id);

        mConnectThread.start();
    }

    @Override
    public void disconnect() {
        mConnectThread = null;
        // TODO: mutex?
        mReadingThread = null;
        mWritingThread = null;

        // Fermeture du socket
        if (mSocket != null) {
            try {
                mSocket.close();
                mSocket = null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * TODO 7 : Conversion des commandes en trames puis transmission par buffer circulaire
     *		Réimplémenter la méthode send pour qu'elle réalise la conversion des commandes en trames puis qu'elle
     les insère dans le buffer circulaire à l'aide de la méthode write du thread d'écriture.
     */
    @Override
    public void send(byte[] data) {
        // TODO: conversion en trame
        if (mWritingThread != null) {
            try {
                mWritingThread.write(frameProcessor.fromFrame(data));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class ConnectThread extends Thread {

        private String mId;

        public ConnectThread(String id) {
            super();
            mId = id;
        }

        @Override
        public void run() {
            BluetoothDevice bluetoothDevice = mAdapter.getRemoteDevice(mId);

            try {
                Log.i("BTConnect", "Trying to connect to " + bluetoothDevice.getName());
                mSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
                Log.i("BTConnect", "Got socket");
                mSocket.connect();
                Log.i("BTConnect", "Connected");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // TODO: stopper éventuelle découverte lancée

            mWritingThread = new WritingThread();
            mReadingThread = new ReadingThread();

            mWritingThread.start();
            mReadingThread.start();

            // Extinction
        }
    }



    /************************************************************************************
     /
     /                          THREADS de COMMUNICATION
     /
     /***********************************************************************************/


    private class WritingThread extends Thread {
        private ByteRingBuffer mBuffer;
        private OutputStream mStream;
        public WritingThread() {
            super();

            mBuffer = new ByteRingBuffer(1024);
            try {
                mStream = mSocket.getOutputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            while (mWritingThread == this) {
                byte[] bytes = mBuffer.getAll();

                if (bytes.length != 0) {
                    try {
                        mStream.write(bytes);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        public void write(byte[] data) {
            mBuffer.put(data);
        }
    }

     /* TODO 6 : Créez ici une classe héritée de Thread permettant de gérer de manière non
                  bloquante la lecture des datas de l'oscilloscope.
                   1 - Dans le constructeur récupérer la référence sur le flux entrant du socket via
                   getInputStream (Attention : getInputStream lance une exeption Checkée)
                   2 - écrire la méthode run qui lit un octet du flux (peu importe si c'est bloquant),
                       et l'envoi dans l'interpreteur de trame jusqu'à ce qu'une trame soit constituée et
                       décodée.
                   3 - lorsque c'est le cas, le listener du transceiver doit traiter la trame.
         */


    private class ReadingThread extends Thread {
        private ByteRingBuffer mBuffer;
        private InputStream mStream;
        public ReadingThread() {
            super();

            mBuffer = new ByteRingBuffer(1024);
            try {
                mStream = mSocket.getInputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            while (mReadingThread == this) {
                /*
                int free = mBuffer.freeSpace();
                byte[] bytes = mStream.readNBytes(free)
                try {
                    mStream.write(bytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                */
            }
        }
    }
}
