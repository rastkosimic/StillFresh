package com.stillfresh.app.sharedentities.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Comparison helper for shared secrets that arrive on a request, such as the
 * {@code X-Gateway-Secret} trust header.
 *
 * <p>{@link String#equals} returns as soon as two bytes differ, so the time it takes leaks how
 * many leading characters an attacker guessed correctly. {@link MessageDigest#isEqual} compares
 * the full length regardless, removing that signal.
 */
public final class SharedSecret {

    private SharedSecret() {
    }

    /**
     * Compares a configured secret against one supplied by a caller in constant time.
     *
     * @param expected the value this service was configured with
     * @param provided the value taken from the incoming request; may be {@code null}
     * @return {@code true} only when both are non-blank and equal
     */
    public static boolean matches(String expected, String provided) {
        if (expected == null || expected.isBlank() || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }
}
