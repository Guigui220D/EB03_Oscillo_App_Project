package com.example.tp_bt_oscillo;

/**
 * Classe abstraite Transciever pour l'envoi et la réception de données
 * et le traitement de cesdonnées à l'aide d'un frame processor
 * @author Guillaume DEREX
 */
public abstract class Transceiver {

    // Listener pour l'écoute des évènements liés au transciever
    public interface TransceiverListener
    {
        void onTransceiverDataReceived();
        void onTransceiverConnectionLost();
        void onTransceiverUnableToConnect();
        void onTranscieverConnected();
    }

    // Etat interne du transciever, peut être utilisé d'une quelconque manière par le programmeur
    private int state;
    // Instance de listener associée
    private TransceiverListener listener;
    // FrameProcessor associé
    protected FrameProcessor frameProcessor;

    /**
     * Obtention de l'état interne du transciever
     * L'état est abstrait et dépend de l'utilisation qu'en fait le programmeur
     * @return Un entier représentant l'état du transciever
     */
    public int getState() {
        return state;
    }

    /**
     * Définition de l'état interne du transciever
     * L'état est abstrait et dépend de l'utilisation qu'en fait le programmeur
     * @param state: entier qui représente l'état du trasnciever
     */
    protected void setState(int state) {
        this.state = state;
    }

    /**
     * Association d'un frame processor pour le traitement des trames
     * Inutilisé car implémentation unique
     * @param frameProcessor: le nouveau frame processor
     */
    public void attachFrameProcessor(FrameProcessor frameProcessor) {
        this.frameProcessor = frameProcessor;
    }

    /**
     * Détachement du frame processor actuel
     * Inutilisé car implémentation unique
     * (Enlève simplement le frame processor)
     */
    public void detachFrameProcessor() {
        this.frameProcessor = null;
    }

    /**
     * Association d'un transciever listener
     * Le transciever listener recevra les évènements associés à la connexion, la déconnexion et la réception de données
     * @param listener: le nouveau listener à associer
     */
    public void setTransceiverListener(TransceiverListener listener) {
        this.listener = listener;
    }

    /**
     * Fonction à appeler par l'implémentation lors d'une perte de connexion
     * Transmet l'information au listener
     */
    protected void connectionLost() {
        if (listener != null)
            listener.onTransceiverConnectionLost();
    }

    /**
     * Fonction à appeler par l'implémentation lors d'un échec de connexion
     * Transmet l'information au listener
     */
    protected void connectionFailed() {
        if (listener != null)
            listener.onTransceiverUnableToConnect();
    }

    /**
     * Fonction à appeler par l'implémentation lors d'une connexion réussie
     * Transmet l'information au listener
     */
    protected void connectionSucceed() {
        if (listener != null)
            listener.onTranscieverConnected();
    }

    /**
     * Fonction à implémenter pour la connexion de ce transciever
     * id est une adresse et peut être utilisé de la manière souhaitée
     * L'implémentation devrait se charger d'appeler les fonctions associées au succès ou à l'échec
     * @param id: addresse à joindre
     */
    public abstract void connect(String id);

    /**
     * Fonction à implémenter pour la connexion de ce transciever
     */
    public abstract void disconnect();

    /**
     * Fonction à impleménter pour l'envoi de données depuis ce transciever au correspondant associé
     * @param data: les octets à envoyer
     */
    abstract public void send(byte[] data);
}
