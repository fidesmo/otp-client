package com.fidesmo.oath.hardware;

import java.io.IOException;
import android.content.SharedPreferences;
import android.nfc.tech.IsoDep;
import com.yubico.yubioath.model.*;


/** Wrapper to communicate with the ykneo-oath applet (https://github.com/Yubico/ykneo-oath) from an
 * Android phone. The code wrapped has been provided by yubico in thier android authenticator
 * android app (https://github.com/Yubico/yubioath-android).
 *
 * This class provides is HardwareToken complient wrapper working, which also works for different
 * AIDs. The aim is to make the YubicoCode more accesible for other android applications.
 *
 * In the long run it should be removed and replaced by a modified YubiKeyNeo class.
 */
public class YubicoCardlet extends YubiKeyNeo implements HardwareToken {

    public YubicoCardlet(SharedPreferences preferences, IsoDep card) throws IOException {
        super(new KeyManager(preferences), card);
    }

    @Override
    public void open() {
    }

    @Override
    public void storeCode(String label, byte[] secret, Algorithm algorithm, int digits, int counterOrPeriod) throws IOException {

        int usedDigits = 6;
        if (digits > 0) {
            usedDigits = digits;
        }

        storeCode(label, secret, algorithm == Algorithm.HOTP ? HOTP_TYPE : TOTP_TYPE, usedDigits, counterOrPeriod);
    }

}
