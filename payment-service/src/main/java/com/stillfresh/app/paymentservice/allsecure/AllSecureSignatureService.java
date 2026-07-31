package com.stillfresh.app.paymentservice.allsecure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Implements AllSecure's request signing and password hashing.
 *
 * <ul>
 *   <li>Password: SHA-1 of the plaintext, lower-case hex (placed inside the XML body).</li>
 *   <li>Request signature: HMAC-SHA512 over
 *       {@code method\nSHA512(body)\ncontentType\nDate\n\nrequestUri}, Base64 encoded
 *       (sent in the Authorization header). The same mechanism verifies inbound callbacks.</li>
 * </ul>
 */
@Service
public class AllSecureSignatureService {

    private static final Logger logger = LoggerFactory.getLogger(AllSecureSignatureService.class);

    private static final String HMAC_ALGO = "HmacSHA512";
    /** RFC-1123 style date as produced by PHP's "D, d M Y H:i:s T", e.g. "Mon, 27 Nov 2018 10:33:41 UTC". */
    private static final String DATE_PATTERN = "EEE, dd MMM yyyy HH:mm:ss zzz";

    /** SHA-1 hashes the plaintext password and returns it lower-case hex (as the gateway expects). */
    public String sha1Password(String plaintext) {
        return hashHex("SHA-1", plaintext);
    }

    /** Builds the current UTC timestamp string used in both the Date header and the signature. */
    public String currentTimestamp() {
        return formatTimestamp(new Date());
    }

    public String formatTimestamp(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN, Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    /**
     * Generates the Base64 HMAC-SHA512 signature for a request.
     *
     * @param method      HTTP method (e.g. "POST")
     * @param body        the exact request body that will be sent
     * @param contentType the Content-Type header value
     * @param timestamp   the Date header value
     * @param requestUri  the request URI path (e.g. "/transaction")
     * @param sharedSecret the connector shared secret (HMAC key)
     */
    public String sign(String method, String body, String contentType, String timestamp,
                       String requestUri, String sharedSecret) {
        String bodyHash = hashHex("SHA-512", body);
        String message = String.join("\n", method, bodyHash, contentType, timestamp, "", requestUri);
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate AllSecure signature", e);
        }
    }

    /**
     * Verifies an inbound callback signature using a constant-time comparison.
     *
     * @param providedSignature the signature extracted from the inbound Authorization header
     */
    public boolean verify(String method, String body, String contentType, String timestamp,
                          String requestUri, String sharedSecret, String providedSignature) {
        if (providedSignature == null || providedSignature.isBlank()) {
            logger.warn("AllSecure callback verification failed: no signature provided");
            return false;
        }
        String expected = sign(method, body, contentType, timestamp, requestUri, sharedSecret);
        boolean ok = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8));
        if (!ok) {
            logger.warn("AllSecure callback signature mismatch (uri={}, contentType={}, date={})",
                    requestUri, contentType, timestamp);
        }
        return ok;
    }

    private static String hashHex(String algorithm, String input) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute " + algorithm + " hash", e);
        }
    }
}
