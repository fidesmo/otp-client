/*
 * Copyright (c) 2013, Yubico AB.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

package com.yubico.yubioath.model;

import android.nfc.tech.IsoDep;
import android.util.Log;
import com.yubico.yubioath.exc.*;
import com.fidesmo.oath.hardware.HardwareToken;
import com.fidesmo.oath.hardware.TokenMeta;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

public class YkneoOath implements HardwareToken {
    private static final byte[] APDU_OK = {(byte) 0x90, 0x00};
    private static final byte[] APDU_FILE_FULL = {(byte) 0x6a, (byte) 0x84};

    public static final byte NAME_TAG = 0x71;
    public static final byte NAME_LIST_TAG = 0x72;
    public static final byte KEY_TAG = 0x73;
    public static final byte CHALLENGE_TAG = 0x74;
    public static final byte RESPONSE_TAG = 0x75;
    public static final byte T_RESPONSE_TAG = 0x76;
    public static final byte NO_RESPONSE_TAG = 0x77;
    public static final byte PROPERTY_TAG = 0x78;
    public static final byte VERSION_TAG = 0x79;
    public static final byte IMF_TAG = 0x7a;

    public static final byte PUT_INS = 0x01;
    public static final byte DELETE_INS = 0x02;
    public static final byte SET_CODE_INS = 0x03;
    public static final byte RESET_INS = 0x04;

    public static final byte LIST_INS = (byte) 0xa1;
    public static final byte CALCULATE_INS = (byte) 0xa2;
    public static final byte VALIDATE_INS = (byte) 0xa3;
    public static final byte CALCULATE_ALL_INS = (byte) 0xa4;
    public static final byte SEND_REMAINING_INS = (byte) 0xa5;

    public static final byte HMAC_MASK = 0x0f;
    public static final byte HMAC_SHA1 = 0x01;
    public static final byte HMAC_SHA256 = 0x02;

    public static final byte OATH_MASK = (byte) 0xf0;
    public static final byte HOTP_TYPE = 0x10;
    public static final byte TOTP_TYPE = 0x20;
    public static final byte[] FIDESMO_AID = {(byte)0xA0, 0x00, 0x00, 0x06, 0x17, 0x00, (byte)0x94, (byte)0xC3, (byte)0xCD, 0x60, 0x01, 0x01};
    public static final byte[] YUBICO_AID = {(byte) 0xa0, 0x00, 0x00, 0x05, 0x27, 0x21, 0x01, 0x01};
    public static final List<byte[]> DEFAULT_AIDS = new ArrayList() {{add(FIDESMO_AID); add(YUBICO_AID);}};

    //APDU CL INS P1 P2 L ...
    //DATA 00  00 00 00 00 ...
    private static final byte[] SELECT_COMMAND = {0x00, (byte) 0xa4, 0x04, 0x00};
    private static final byte[] CALCULATE_ALL_COMMAND = {0x00, CALCULATE_ALL_INS, 0x00, 0x01, 0x0a, CHALLENGE_TAG, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static final byte[] CALCULATE_COMMAND = {0x00, CALCULATE_INS, 0x00, 0x01, 0x00};
    private static final byte[] PUT_COMMAND = {0x00, PUT_INS, 0x00, 0x00, 0x00};
    private static final byte[] DELETE_COMMAND = {0x00, DELETE_INS, 0x00, 0x00, 0x00};
    private static final byte[] SEND_REMAINING_COMMAND = {0x00, SEND_REMAINING_INS, 0x00, 0x00, 0x00};

    private static final int[] MOD = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000};


    private final IsoDep isoTag;
    private byte[] id;

    public YkneoOath(IsoDep isoTag) {
        this.isoTag = isoTag;
    }

    private byte[] select(byte[] aid) {
        byte[] apdu = new byte[SELECT_COMMAND.length + 1 + aid.length];
        System.arraycopy(SELECT_COMMAND, 0, apdu, 0, SELECT_COMMAND.length);
        apdu[SELECT_COMMAND.length] = (byte)aid.length;
        System.arraycopy(aid, 0, apdu, SELECT_COMMAND.length + 1, aid.length);
        return apdu;
    }

    public byte[] open() throws IOException, AppletSelectException {
        return open(DEFAULT_AIDS);
    }

    public byte[] open(List<byte[]> aids) throws IOException, AppletSelectException {
        isoTag.connect();
        isoTag.setTimeout(3000);

        byte[] resp = null;
        byte[] selected = null;
        for(byte[] aid: aids) {
            resp = isoTag.transceive(select(aid));
            if(compareStatus(resp, APDU_OK)) {
                selected = aid;
                break;
            }
        }

        if(resp == null || !compareStatus(resp, APDU_OK))
            throw new AppletMissingException();

        int offset = 0;
        byte[] version = parseBlock(resp, offset, VERSION_TAG);
        offset += version.length + 2;

        checkVersion(version);

        id = parseBlock(resp, offset, NAME_TAG);
        return selected;
    }

    public byte[] getId() {
        return id;
    }

    private byte generateType(TokenMeta meta) {
        byte type = 0;
        switch(meta.getType()) {
        case TOTP:
            type = TOTP_TYPE;
            break;
        case HOTP:
            type = HOTP_TYPE;
            break;
        }

        byte algo = 0;
        switch(meta.getAlgorithm()) {
        case SHA1:
            algo = HMAC_SHA1;
            break;
        case SHA256:
            algo = HMAC_SHA256;
            break;
        }
        return (byte)((int)type | (int)algo);
    }


    public void storeCode(TokenMeta token, byte[] key, int counter) throws IOException {
        byte[] nameBytes = token.getLabel().getBytes();
        byte[] counterBytes = null;
        int length = PUT_COMMAND.length + 2 + nameBytes.length + 4 + key.length;
        if (counter > 0) {
            length += 6;
            counterBytes = new byte[4];
            counterBytes[0] = (byte) (counter >>> 24);
            counterBytes[1] = (byte) (counter >>> 16);
            counterBytes[2] = (byte) (counter >>> 8);
            counterBytes[3] = (byte) counter;
        }
        byte[] data = new byte[length];
        System.arraycopy(PUT_COMMAND, 0, data, 0, PUT_COMMAND.length);
        int offset = 4;
        data[offset++] = (byte) (data.length - 5);

        data[offset++] = NAME_TAG;
        data[offset++] = (byte) nameBytes.length;
        System.arraycopy(nameBytes, 0, data, offset, nameBytes.length);
        offset += nameBytes.length;

        data[offset++] = KEY_TAG;
        data[offset++] = (byte) (key.length + 2);
        data[offset++] = generateType(token);
        data[offset++] = (byte) token.getDigits();
        System.arraycopy(key, 0, data, offset, key.length);
        offset += key.length;

        if (counterBytes != null) {
            data[offset++] = IMF_TAG;
            data[offset++] = (byte) counterBytes.length;
            System.arraycopy(counterBytes, 0, data, offset, counterBytes.length);
            offset += counterBytes.length;
        }

        byte[] resp = isoTag.transceive(data);
        if (compareStatus(resp, APDU_FILE_FULL)) {
            throw new StorageFullException("No more room for OATH credentials!");
        } else {
            requireStatus(resp, APDU_OK);
        }
    }

    public void deleteCode(String name) throws IOException {
        byte[] nameBytes = name.getBytes();
        byte[] data = new byte[DELETE_COMMAND.length + 2 + nameBytes.length + 2];
        System.arraycopy(DELETE_COMMAND, 0, data, 0, DELETE_COMMAND.length);
        int offset = 4;
        data[offset++] = (byte) (data.length - 5);

        data[offset++] = NAME_TAG;
        data[offset++] = (byte) nameBytes.length;
        System.arraycopy(nameBytes, 0, data, offset, nameBytes.length);
        requireStatus(isoTag.transceive(data), APDU_OK);
    }

    public String readHotpCode(String name) throws IOException {
        byte[] nameBytes = name.getBytes();
        byte[] data = new byte[CALCULATE_COMMAND.length + 2 + nameBytes.length + 2];
        System.arraycopy(CALCULATE_COMMAND, 0, data, 0, CALCULATE_COMMAND.length);
        int offset = 4;
        data[offset++] = (byte) (data.length - 5);

        data[offset++] = NAME_TAG;
        data[offset++] = (byte) nameBytes.length;
        System.arraycopy(nameBytes, 0, data, offset, nameBytes.length);
        offset += nameBytes.length;

        data[offset++] = CHALLENGE_TAG;
        data[offset++] = 0;

        byte[] resp = requireStatus(send(data), APDU_OK);
        return codeFromTruncated(parseBlock(resp, 0, T_RESPONSE_TAG));
    }

    public String readTotpCode(String name, long timestamp) throws IOException {
        byte[] nameBytes = name.getBytes();
        byte[] data = new byte[CALCULATE_COMMAND.length + 2 + nameBytes.length + 10];
        System.arraycopy(CALCULATE_COMMAND, 0, data, 0, CALCULATE_COMMAND.length);
        int offset = 4;
        data[offset++] = (byte) (data.length - 5);
        data[offset++] = NAME_TAG;
        data[offset++] = (byte) nameBytes.length;
        System.arraycopy(nameBytes, 0, data, offset, nameBytes.length);
        offset += nameBytes.length;

        data[offset++] = CHALLENGE_TAG;
        data[offset++] = 8;
        data[offset++] = 0;
        data[offset++] = 0;
        data[offset++] = 0;
        data[offset++] = 0;
        data[offset++] = (byte) (timestamp >> 24);
        data[offset++] = (byte) (timestamp >> 16);
        data[offset++] = (byte) (timestamp >> 8);
        data[offset++] = (byte) timestamp;

        byte[] resp = requireStatus(send(data), APDU_OK);
        return codeFromTruncated(parseBlock(resp, 0, T_RESPONSE_TAG));
    }

    public List<TokenMeta> getTokens(long timestamp) throws IOException {
        List<TokenMeta> tokens = new ArrayList<TokenMeta>();

        byte[] command = new byte[CALCULATE_ALL_COMMAND.length];
        System.arraycopy(CALCULATE_ALL_COMMAND, 0, command, 0, CALCULATE_ALL_COMMAND.length);
        int offset = CALCULATE_ALL_COMMAND.length - 4;
        command[offset++] = (byte) (timestamp >> 24);
        command[offset++] = (byte) (timestamp >> 16);
        command[offset++] = (byte) (timestamp >> 8);
        command[offset++] = (byte) timestamp;

        byte[] resp = requireStatus(send(command), APDU_OK);

        offset = 0;
        while (resp[offset] == NAME_TAG) {
            byte[] name = parseBlock(resp, offset, NAME_TAG);
            offset += name.length + 2;
            byte responseType = resp[offset];
            byte[] hashBytes = parseBlock(resp, offset, responseType);
            offset += hashBytes.length + 2;

            String nameStr = new String(name);
            switch (responseType) {
                case T_RESPONSE_TAG:
                    tokens.add(new TokenMeta(new String(name), codeFromTruncated(hashBytes).length(), TokenMeta.Type.TOTP));
                    break;
                case NO_RESPONSE_TAG:
                    tokens.add(new TokenMeta(new String(name), 6, TokenMeta.Type.HOTP));
                    break;
            }
        }

        return tokens;
    }

    private byte[] send(byte[] command) throws IOException {
        byte[] resp = isoTag.transceive(command);
        byte[] buf = new byte[2048];
        int offset = 0;

        while (resp[resp.length - 2] == 0x61) {
            System.arraycopy(resp, 0, buf, offset, resp.length - 2);
            offset += resp.length - 2;
            resp = isoTag.transceive(SEND_REMAINING_COMMAND);
        }

        System.arraycopy(resp, 0, buf, offset, resp.length);
        byte[] properlySized = new byte[offset + resp.length];
        System.arraycopy(buf, 0, properlySized, 0, properlySized.length);

        return properlySized;
    }

    public void close() throws IOException {
        isoTag.close();
    }

    private static void checkVersion(byte[] version) throws UnsupportedAppletException {
        byte major = version[0];

        if (major > 0) {
            throw new UnsupportedAppletException(version);
        }
    }

    private static boolean compareStatus(byte[] apdu, byte[] status) {
        return apdu[apdu.length - 2] == status[0] && apdu[apdu.length - 1] == status[1];
    }

    private static byte[] requireStatus(byte[] apdu, byte[] status) throws IOException {
        if (!compareStatus(apdu, status)) {
            String expected = String.format("%02x%02x", 0xff & status[0], 0xff & status[1]).toUpperCase();
            String actual = String.format("%02x%02x", 0xff & apdu[apdu.length - 2], 0xff & apdu[apdu.length - 1]).toUpperCase();
            throw new IOException("Require APDU status: " + expected + ", got " + actual);
        }
        return apdu;
    }

    private static byte[] parseBlock(byte[] data, int offset, byte identifier) throws IOException {
        if (data[offset] == identifier) {
            int length = data[offset + 1];
            byte[] block = new byte[length];
            System.arraycopy(data, offset + 2, block, 0, length);
            return block;
        } else {
            throw new IOException("Require block type: " + identifier + ", got: " + data[offset]);
        }
    }

    private static String codeFromTruncated(byte[] data) {
        int num_digits = data[0];
        int code = (data[1] << 24) | ((data[2] & 0xff) << 16) | ((data[3] & 0xff) << 8) | (data[4] & 0xff);
        return String.format("%0" + num_digits + "d", code % MOD[num_digits]);
    }
}
