package com.example.tp_bt_oscillo;

import static java.lang.Math.min;

import androidx.annotation.NonNull;

import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;

/**
 * ByteRingBuffer: buffer circulaire d'octets
 * Guillaume DEREX
 */
public class ByteRingBuffer {

    private byte [] buff;
    private int wPos;  // position d'écriture
    private int rPos; // position de lecture
    private boolean full = false; //indique un buffer full car wPos = rPos est insuffisant pour cela


    /**
     *   Constructeur
     *   @param size : taille du buffer circulaire en octet
     */
    public ByteRingBuffer(int size) {
        buff = new byte[size];
        wPos = 0;
        rPos = 0;
        full = false;
    }


    /**
     *   Ecriture des octets d'un tableau dans le buffer
     *   L'overflow est détecté avant l'écriture et celle si ne se fait pas s'il n'y a pas la place
     *   @param bArray : tableau d'octets à écrire
     */
    public synchronized void put(byte[] bArray) throws BufferOverflowException {

        final int length = bArray.length;
        final int free = freeSpace();

        // Cas trivial
        if (length == 0)
            return;
        // Pas assez de place
        if (length > free)
            throw new BufferOverflowException();

        if (wPos < rPos) {
            // Cas mémoire continue simple
            System.arraycopy(bArray, 0, buff, wPos, length);
            wPos += length;
        } else if (wPos > rPos) {
            // Cas mémoire en 2 morceaux
            final int tailSpace = buff.length - wPos;

            if (length <= tailSpace) {
                // Une seule partie utilisée
                System.arraycopy(bArray, 0, buff, wPos, length);
                wPos += length;
            } else {
                // Deux partie, deux copies
                System.arraycopy(bArray, 0, buff, wPos, tailSpace);
                System.arraycopy(bArray, tailSpace, buff, 0, length - tailSpace);
                wPos = length - tailSpace;
            }
        } else {
            // Buffer intégralement vide et place dispo (sinon on aurait levé une erreur avant)
            // Remise à 0 des curses pour la simplicité
            rPos = 0;
            wPos = length;
            System.arraycopy(bArray, 0, buff, 0, length);
        }

        // Bouclage du wPos
        if (wPos == buff.length)
            wPos = 0;

        // Actualisation du flag full
        if (free == length)
            full = true;
    }


    /**
     *   Nombre d'octets libres dans le buffer
     *   @return nombre d'octets libres dans le buffer
     */
    public synchronized int freeSpace() {
        if (full) return 0;

        if (wPos < rPos) {
            return rPos - wPos;
        } else if (wPos > rPos) {
            return buff.length - wPos + rPos;
        } else {
            return buff.length;
        }
    }

    /**
     *   Nombre d'octets disponibles dans le buffer
     *   @return nombre d'octets présents dans le buffer
     */
    public synchronized int bytesToRead() {
        return buff.length - freeSpace();
    }


    /**
     *   Ajout d'un octet dans le buffer
     *   @param b : octet à ajouter
     */
    public synchronized void put(byte b) throws BufferOverflowException {
        if (full)
            throw new BufferOverflowException();

        buff[wPos] = b;
        wPos++;
        if (wPos == buff.length)
            wPos = 0;

        if (wPos == rPos)
            full = true;
    }


    /**
     *   Lecture de tous les octets présents dans le buffer
     *   @return tableau d'octets lu
     */
    public synchronized byte[] getAll(){
        final int toRead = bytesToRead();
        byte[] ret = new byte[toRead];

        if (wPos <= rPos) {
            // Mémoire segmentée, deux lectures
            final int tailData = buff.length - rPos;

            if (toRead <= tailData) {
                // Une seule partie utilisée
                System.arraycopy(buff, rPos, ret, 0, toRead);
            } else {
                // Deux partie, deux copies
                System.arraycopy(buff, rPos, ret, 0, tailData);
                System.arraycopy(buff, 0, ret, tailData, toRead - tailData);
            }

        } else {
            // wPos > rPos, mémoire continue, lecture en un coup
            System.arraycopy(buff, rPos, ret, 0, toRead);
        }

        // Remise à 0 du buffer pour la simplicité
        full = false;
        wPos = 0;
        rPos = 0;

        return ret;
    }


    /**
     *   Lecture d'un octet du buffer
     *   @return octet lu
     */
    public synchronized byte get() throws BufferUnderflowException {
		if (bytesToRead() == 0)
            throw new BufferUnderflowException();

        full = false;

        byte ret = buff[rPos];
        rPos++;
        if (rPos == buff.length)
            rPos = 0;

        return ret;
    }


    /**
     *   Indication d'information sur le buffer (utilisé principalement pour le débuggage)
     *   @return Chaine contenant des informations d'état du buffer (taille, nombre d'éléments présents, position des pointeurs ...)
     */
    @NonNull
    @Override
    public synchronized String toString() {
        return "{circular buffer of " + buff.length + " bytes, " + freeSpace() + " free, " + bytesToRead() + " used ; reading at " + rPos + ", writing at " + wPos + " ; full?: " + full + "}";
    }

    /**
     *   Obtention des données du buffer en string (utilisé principalement pour le débuggage)
     *   @return Le contenu complet du buffer interprété comme chaîne UTF-8
     */
    @NonNull
    public synchronized String dumpAsString() {
        byte[] contents = getAll();
        put(contents);

        return new String(contents, StandardCharsets.UTF_8);
    }
}