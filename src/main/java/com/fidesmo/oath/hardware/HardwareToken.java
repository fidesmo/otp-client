package com.fidesmo.oath.hardware;

import java.io.IOException;
import java.util.Map;
import java.util.List;

/** High-level interface an external secure element should comply to in order to implement an oath
 *  application for HOTP and TOTP.
 */
public interface HardwareToken {

    public enum Algorithm {
        TOTP,
        HOTP
    }

    /** Open must be called before any operation is executed
     */
    public void open() throws IOException;

    /** Close must be called befor the object is destroyed. After closing no fruther operations shall be executed
     */
    public void close() throws IOException;

    /** Get a unique identfier for this hardware token
     */
    public byte[] getId() throws IOException;

    /** Store a secret key for TOTP or HOTP on the hardware token
     *
     * @param digits maybe the number of digits (default is 6)
     * @param counterOrInterval if algorithm is HOTP must be the initial counter if algorithm is TOTP  might be lifeperiod of a single code in seconds (default is 30)
     */
    public void storeCode(String label, byte[] key, Algorithm algorithm, int digits, int counterOrPeriod) throws IOException;

    /** Delete the secret key of the specified entry
     */
    public void deleteCode(String label) throws IOException;

    /** Get a hotpCode for the specified entry
     */
    public String readHotpCode(String label) throws IOException;

    /** Get a list of identifiers for all available secrets. It will also return the current codes
     *  for TOTP entries.
     */
    public List<Map<String, String>> getCodes(long timestamp) throws IOException;
}
