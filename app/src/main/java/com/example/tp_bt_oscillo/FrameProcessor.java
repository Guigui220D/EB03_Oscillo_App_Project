package com.example.tp_bt_oscillo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class FrameProcessor {
    //private byte[] txFrame;
    //private byte[] rxData;

    /*
    private byte[] toFrame(byte[] data) {

    }
    */

    public static byte[] fromFrame(byte[] data) throws IOException {
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
