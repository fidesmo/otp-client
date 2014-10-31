/*
 * Copyright (c) 2014, Fidesmo AB.  All rights reserved.
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

package com.fidesmo.oath.hardware;

import java.io.IOException;
import java.util.List;

/** High-level interface an external secure element should comply to in order to implement an oath
 *  application for HOTP and TOTP.
 */
public interface HardwareToken {

    /** Open must be called before any operation is executed, returns the AID selected
     *  This attempts to select the default AIDs.
     */
    public byte[] open() throws IOException;

    /** Open must be called before any operation is executed, returns the AID selected
     *  @param aids A list of AIDs to be attempted
     */
    public byte[] open(List<byte[]> aids) throws IOException;

    /** Close must be called before the object is destroyed
     *  After closing no fruther operations shall be executed
     */
    public void close() throws IOException;

    /** Return ID of cardlet
     */
    public byte[] getId();

    /** Store a secret key for TOTP or HOTP on the hardware token
     *
     *  @param digits the number of digits (default is 6)
     *  @param counterOrInterval if algorithm is HOTP must be the initial
     *                           counter if algorithm is TOTP  might be lifeperiod
     *                           of a single code in seconds (default is 30)
     */
    public void storeCode(TokenMeta token, byte[] key, int counterOrPeriod) throws IOException;

    /** Delete the secret key of the specified entry
     */
    public void deleteCode(String label) throws IOException;

    /** Get a hotpCode for the specified entry
     */
    public String readHotpCode(String label) throws IOException;

    /** Get a totp code for the specified entry
     */
    public String readTotpCode(String label, long timestamp) throws IOException;

    /** Get a list of identifiers for all available secrets. It will also return the current codes
     *  for TOTP entries.
     */
    public List<TokenMeta> getTokens(long timestamp) throws IOException;

}
