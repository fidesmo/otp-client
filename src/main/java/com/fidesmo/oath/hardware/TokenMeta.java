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

public class TokenMeta {
    public static enum Type {
        TOTP,
        HOTP
    }

    public static enum Algorithm {
        SHA1,
        SHA256
    }

    private final String label;
    private final int digits;
    private final Type type;
    private final Algorithm algorithm;

    public TokenMeta(String label, int digits, Type type) {
        this.label = label;
        this.digits = digits;
        this.type = type;
        this.algorithm = Algorithm.SHA1;
    }

    public TokenMeta(String label, int digits, Type type, Algorithm algorithm) {
        this.label = label;
        this.digits = digits;
        this.type = type;
        this.algorithm = algorithm;
    }

    public String getLabel() {
        return label;
    }

    public int getDigits() {
        return digits;
    }

    public Type getType() {
        return type;
    }

    public Algorithm getAlgorithm() {
        return algorithm;
    }

}
