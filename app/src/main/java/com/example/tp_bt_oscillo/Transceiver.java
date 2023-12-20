package com.example.tp_bt_oscillo;

public abstract class Transceiver {

    public interface TransceiverListener
    {
        void onTransceiverDataReceived();
        void onTransceiverConnectionLost();
        void onTransceiverUnableToConnect();
    }

    private int state;
    private TransceiverListener listener;
    protected FrameProcessor frameProcessor;

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void attachFrameProcessor(FrameProcessor frameProcessor) {
        this.frameProcessor = frameProcessor;
    }

    public void detachFrameProcessor() {
        this.frameProcessor = null;
    }
    public void setTransceiverListener(TransceiverListener listener) {
        this.listener = listener;
    }

    private void connectionLost() {
        if (listener != null)
            listener.onTransceiverConnectionLost();
    }
    private void connectionFailed() {
        if (listener != null)
            listener.onTransceiverUnableToConnect();
    }

    public abstract void connect(String id);
    public abstract void disconnect();
    abstract public void send(byte[] data);
}
