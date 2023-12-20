package com.example.tp_bt_oscillo;

import org.junit.Test;

import static org.junit.Assert.*;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.Arrays;
import java.util.Random;

/**
 * Tests unitaires pour le byteRingBuffer
 * Guillaume DEREX
 */
public class ByteRingBufferTest {

    // Test d'initialization d'un buffer circulaire
    @Test
    public void ringbuffer_inizialization() {
        ByteRingBuffer b = new ByteRingBuffer(50);
        assertEquals(50, b.freeSpace());
        assertEquals(0, b.bytesToRead());
    }

    // Test de put et get octet par octet
    @Test
    public void ringbuffer_getAndPutSingle() {
        ByteRingBuffer b = new ByteRingBuffer(50);

        b.put((byte)1);
        b.put((byte)2);
        b.put((byte)3);

        assertEquals(47, b.freeSpace());
        assertEquals(3, b.bytesToRead());

        assertEquals((byte)1, b.get());
        assertEquals((byte)2, b.get());
        assertEquals((byte)3, b.get());

        assertEquals(50, b.freeSpace());
        assertEquals(0, b.bytesToRead());
    }

    // test de put et getall pour les arrrays
    @Test
    public void ringbuffer_getAndPutArray() {
        ByteRingBuffer b = new ByteRingBuffer(50);

        b.put(new byte[]{4, 5, 6, 7});

        assertEquals(46, b.freeSpace());
        assertEquals(4, b.bytesToRead());

        byte[] contents = b.getAll();
        assertEquals(4, contents.length);
        assertEquals((byte)4, contents[0]);
        assertEquals((byte)5, contents[1]);
        assertEquals((byte)6, contents[2]);
        assertEquals((byte)7, contents[3]);

        assertEquals(50, b.freeSpace());
        assertEquals(0, b.bytesToRead());
    }

    // Test de détection d'overflow dans put
    @Test
    public void ringbuffer_overflowPutSingle() {
        ByteRingBuffer b = new ByteRingBuffer(1);

        b.put((byte)1);
        assertThrows(BufferOverflowException.class, () -> b.put((byte)2));
    }

    // Test de détection d'overflow dans put (arrays)
    @Test
    public void ringbuffer_overflowPutArray() {
        ByteRingBuffer b = new ByteRingBuffer(5);

        b.put((byte)1);
        assertThrows(BufferOverflowException.class, () -> b.put(new byte[]{1, 2, 3, 4, 5, 6}));
    }

    // Test de détection de lecture impossible dans get
    @Test
    public void ringbuffer_underflowGet() {
        ByteRingBuffer b = new ByteRingBuffer(5);

        assertThrows(BufferUnderflowException.class, b::get);
    }

    // Test par fuzzing
    @Test
    public void ringbuffer_fuzzingTest() {
        // Test du ring buffer par fuzzing (lecture et écriture de quantités aléatoires de données aléatoires)
        // 2 PRNG avec même graine pour comparer les données
        // Peut durer quelques secondes (au pire, réduire steps)

        final int steps = 10_000_000;

        Random sizes = new Random();
        long seed = sizes.nextLong();
        Random prng1 = new Random(seed);
        Random prng2 = new Random(seed);

        ByteRingBuffer b = new ByteRingBuffer(64);

        for (int i = 0; i < steps; i++) {
            // Ecriture de données aléatoires
            {
                int writeSize = 0;
                if (b.freeSpace() != 0)
                    writeSize = sizes.nextInt(b.freeSpace());
                byte[] writeBuf = new byte[writeSize];
                randomBytesDeterministic(prng1, writeBuf);

                if (sizes.nextBoolean()) {
                    // Ecriture d'un coup
                    b.put(writeBuf);
                } else {
                    // Ecriture de plusieurs octets
                    for (byte n : writeBuf) {
                        b.put(n);
                    }
                }
            }

            // Lecture de données
            // Assertions ici
            {
                if (sizes.nextBoolean()) {
                    // lecture de tout avec getall
                    int readSize = sizes.nextInt();
                    byte[] comparisonBuf = new byte[b.bytesToRead()];
                    randomBytesDeterministic(prng2, comparisonBuf);

                    assertTrue(Arrays.equals(comparisonBuf, b.getAll()));
                } else {
                    // lecture de quelques octets
                    int readSize = 0;
                    if (b.bytesToRead() != 0)
                        readSize = sizes.nextInt(b.bytesToRead());
                    byte[] comparisonBuf = new byte[readSize];
                    randomBytesDeterministic(prng2, comparisonBuf);

                    for (byte n : comparisonBuf) {
                        assertEquals(n, b.get());
                    }
                }
            }
        }
    }

    @Test
    public void ringbuffer_toStringTest() {
        ByteRingBuffer b = new ByteRingBuffer(50);
        b.put((byte)1);

        assertEquals("{circular buffer of 50 bytes, 49 free, 1 used ; reading at 0, writing at 1 ; full?: false}", b.toString());
    }

    @Test
    public void ringbuffer_dumpTest() {
        ByteRingBuffer b = new ByteRingBuffer(5);
        b.put((byte)'y');
        b.put((byte)'e');
        b.put((byte)'s');

        assertEquals("yes", b.dumpAsString());
    }

    // Remplit le buffer d'octets aléatoires
    // Nécessaire car Random.nextBytes peut sauter des octets
    // Utilisé pour le test de fuzzing afin d'avoir de l'aléatoire plus facilement déterminé
    void randomBytesDeterministic(Random r, byte[] buf) {
        for (int i = 0; i < buf.length; i++) {
            buf[i] = (byte)r.nextInt(256);
        }
    }
}