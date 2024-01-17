package com.example.tp_bt_oscillo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Frame processor (pour le protocole de l'oscilloscope)
 * TODO: en faire une interface et la spécialiser
 */
public class FrameProcessor {
    //private byte[] txFrame;
    //private byte[] rxData;

    // TODO
    /*
    private byte[] toFrame(byte[] data) {

    }
    */

    /**
     * Conversion de données pure en trame prête à envoyer un oscilloscope
     * @param data: octets à envoyer en section données
     * @return trame valide prête à être envoyée
     * @throws IOException
     */
    public static byte[] toFrame(byte[] data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        byte[] size = ByteBuffer.allocate(2).putShort((short)data.length).array();
        byte crtl = calculateChecksum(size, data);

        // Formation de la trame

        outputStream.write(size);
        outputStream.write(data);
        outputStream.write(crtl);

        byte[] beforeEscape = outputStream.toByteArray();
        outputStream.reset();

        // octet de début
        outputStream.write(0x05);
        // Procédure d'échappement
        for (byte b : beforeEscape) {
            if (b == 0x04 || b == 0x05)
                outputStream.write(0x06);
            if (b == 0x06) {
                outputStream.write(new byte[]{ 0x06, 0x0C });
                continue;
            }
            outputStream.write(b);
        }
        // octet de fin
        outputStream.write(0x04);

        return outputStream.toByteArray();
    }

    /**
     * Calcul de la somme de contrôle pour les trames
     * @param size: champ "size" de la trame (2 octets d'un entier short)
     * @param payload: champ données de la trame
     * @return octet de somme de contrôle
     */
    private static byte calculateChecksum(byte[] size, byte[] payload) {
        int sum = 0;

        for (byte b : size) {
            sum += b & 0xFF;
        }
        for (byte b : payload) {
            sum += b & 0xFF;
        }

        byte ctrl = (byte) (~(sum & 0xFF) + 1);
        return ctrl;
    }
}
