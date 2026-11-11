package com.cleanroom.remotedesktop.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityTest {

    @Test
    public void testHmacCalculationAndVerification() {
        String password = "testPassword123";
        String nonce = AuthCryptoHelper.generateRandomHex(16);

        String hmac = AuthCryptoHelper.calculateHmac(password, nonce);
        assertNotNull(hmac);
        assertFalse(hmac.isEmpty());

        assertTrue(AuthCryptoHelper.verifyHmac(password, nonce, hmac));
        assertFalse(AuthCryptoHelper.verifyHmac("wrongPassword", nonce, hmac));
        assertFalse(AuthCryptoHelper.verifyHmac(password, "differentNonce", hmac));
    }

    @Test
    public void testRandomPasswordAndHostId() {
        String pwd4 = RandomPasswordGenerator.generatePassword(4);
        assertNotNull(pwd4);
        assertEquals(4, pwd4.length());

        String pwd6 = RandomPasswordGenerator.generatePassword(6);
        assertEquals(6, pwd6.length());

        String hostId = RandomPasswordGenerator.generateRandomHostId();
        assertNotNull(hostId);
        assertEquals(9, hostId.length());

        String formatted = RandomPasswordGenerator.formatHostId(hostId);
        assertEquals(11, formatted.length()); // "xxx xxx xxx"
        assertTrue(formatted.contains(" "));
    }

    @Test
    public void testRateLimiter() {
        RateLimiter limiter = new RateLimiter(3, 500); // 3 attempts within 500ms
        String ip = "192.168.1.10";

        assertTrue(limiter.isAllowed(ip));
        limiter.recordFailure(ip);
        assertTrue(limiter.isAllowed(ip));
        limiter.recordFailure(ip);
        assertTrue(limiter.isAllowed(ip));
        limiter.recordFailure(ip);

        // 3 failures reached
        assertFalse(limiter.isAllowed(ip));

        // Other IP is still allowed
        assertTrue(limiter.isAllowed("192.168.1.20"));

        // Reset works
        limiter.reset(ip);
        assertTrue(limiter.isAllowed(ip));
    }
}
