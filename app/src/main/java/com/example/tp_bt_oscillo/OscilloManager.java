package com.example.tp_bt_oscillo;

/**
 * Surcharge du transciever bluetooth pour l'oscilloscope
 * @author Guillaume DEREX
 */
public class OscilloManager extends BTManager {

    // Listener spécialisé
    private OscilloEventsListener listener;
    public interface OscilloEventsListener
    {
        void onOscilloConnected();
        void onOscilloDisconnected();

        void onOscilloLostConnection();
    }

    // Adaptation du transciever listener
    TransceiverListener transceiverListener = new TransceiverListener() {
        @Override
        public void onTransceiverDataReceived() {

        }

        @Override
        public void onTransceiverConnectionLost() {
            setState(STATE_NOT_CONNECTED);
            if (listener != null)
                listener.onOscilloLostConnection();
        }

        @Override
        public void onTransceiverUnableToConnect() {
            setState(STATE_NOT_CONNECTED);
        }

        @Override
        public void onTranscieverConnected() {
            setState(STATE_CONNECTED);
            if (listener != null)
                listener.onOscilloConnected();
        }
    };

    // Etats possible du transciever
    public static final int STATE_NOT_CONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    // Implémentation du singleton pattern
    private static OscilloManager instance;

    /**
     * Obtention du singleton
     * @return l'instance du singleton (dernière instance construite)
     */
    public static OscilloManager getInstance() {
        return instance;
    }

    /**
     * Constructeur
     * à appeler uniquemenet (singleton pattern)
     */
    public OscilloManager() {
        super();

        instance = this;
        setState(STATE_NOT_CONNECTED);
        setTransceiverListener(transceiverListener);
    }

    /**
     * Surcharge du connect pour gérer l'état interne
     * @param id: addresse de l'appareil bluetooth à joindre
     */
    @Override
    public void connect(String id) {
        setState(STATE_CONNECTING);
        super.connect(id);
    }

    /**
     * Surcharge du disconnect pour gérer l'état interne
     * Signale la déconnexion au listener
     */
    @Override
    public void disconnect() {
        setState(STATE_NOT_CONNECTED);
        if (listener != null)
            listener.onOscilloDisconnected();
        super.disconnect();
    }

    public void setListener(OscilloEventsListener listener) {
        this.listener = listener;
    }


    // Fonctions de contrôle de l'oscilloscope

    /**
     * Gestion de la luminiosité de la led frontale
     * (Du rapport cyclique de calibration)
     * @param brightness: intensité entre 0 et 100 (%)
     */
    public void setLedBrightness(int brightness) {
        if (getState() != STATE_CONNECTED)
            return;
        if (brightness > 100)
            brightness = 100;
        if (brightness < 0)
            brightness = 0;
        send(new byte[] { 0x0A, (byte)brightness });
    }

    // TODO: d'autres fonctions ici
}
