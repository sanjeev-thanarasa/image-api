package com.nmr.image_api.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HashUtilTest {

    @Test
    void sha256_producesExpectedLengthAndHex() {
        byte[] data = "hello".getBytes();
        String hash = HashUtil.sha256(data);
        assertNotNull(hash);
        assertEquals(64, hash.length()); // 256 bits -> 64 hex chars
        assertTrue(hash.matches("[0-9a-f]{64}"));
    }

}
