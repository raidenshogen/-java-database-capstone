package org.shopwave.smartclinic.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for creating and validating JSON Web Tokens (JWT).
 * <p>
 * Tokens are signed using HMAC-SHA256 with a configured secret key.
 * The subject ({@code sub}) claim carries the user's email address.
 */
@Service
public class TokenService {

    @Value("${jwt.secret:default-secret-key-change-in-production-min-32-chars}")
    private String secret;

    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    /**
     * Generates a JWT token for the given user email.
     * <p>
     * The token contains:
     * <ul>
     *   <li>{@code sub} — the user's email (subject)</li>
     *   <li>{@code iat} — issued-at timestamp</li>
     *   <li>{@code exp} — expiration timestamp</li>
     * </ul>
     *
     * @param email the user's email address to encode as the token subject
     * @return a signed JWT string
     */
    public String generateToken(String email) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", email);
        claims.put("iat", now.toEpochMilli());
        claims.put("exp", expiry.toEpochMilli());

        return sign(claims);
    }

    /**
     * Extracts the subject (user email) from a JWT token.
     *
     * @param token the JWT token string
     * @return the email address encoded in the token, or {@code null} if invalid
     */
    public String extractEmail(String token) {
        Map<String, Object> claims = parse(token);
        if (claims == null) {
            return null;
        }
        return (String) claims.get("sub");
    }

    /**
     * Validates whether a token is still active (not expired).
     *
     * @param token the JWT token string
     * @return {@code true} if the token is valid and not expired
     */
    public boolean isTokenValid(String token) {
        Map<String, Object> claims = parse(token);
        if (claims == null) {
            return false;
        }
        Long exp = (Long) claims.get("exp");
        return exp != null && exp > System.currentTimeMillis();
    }

    /**
     * Returns the signing key derived from the configured secret.
     *
     * @return a {@link SecretKeySpec} using HMAC-SHA256
     */
    public SecretKeySpec getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    /* ---------- internal ---------- */

    private String sign(Map<String, Object> claims) {
        String header = base64UrlEncode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = base64UrlEncode(serializeClaims(claims).getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;
        String signature = base64UrlEncode(hmacSha256(signingInput));
        return signingInput + "." + signature;
    }

    private Map<String, Object> parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String payloadJson = new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8);
            Map<String, Object> claims = deserializeClaims(payloadJson);

            // Verify signature
            String signingInput = parts[0] + "." + parts[1];
            byte[] expectedSignature = hmacSha256(signingInput);
            byte[] actualSignature = base64UrlDecode(parts[2]);
            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                return null;
            }
            return claims;
        } catch (Exception e) {
            return null;
        }
    }

    private String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private byte[] base64UrlDecode(String data) {
        return Base64.getUrlDecoder().decode(data);
    }

    private byte[] hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(getSigningKey());
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Unable to compute HMAC-SHA256", e);
        }
    }

    private String serializeClaims(Map<String, Object> claims) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value);
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deserializeClaims(String json) {
        // Minimal JSON parser for the flat claim map we produce.
        // Handles strings and numbers only — sufficient for our claims.
        Map<String, Object> claims = new HashMap<>();
        if (json == null || !json.startsWith("{") || !json.endsWith("}")) {
            return null;
        }
        String inner = json.substring(1, json.length() - 1).trim();
        if (inner.isEmpty()) {
            return claims;
        }
        String[] pairs = inner.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length != 2) {
                return null;
            }
            String key = kv[0].trim().replace("\"", "");
            String val = kv[1].trim();
            if (val.startsWith("\"") && val.endsWith("\"")) {
                claims.put(key, val.substring(1, val.length() - 1));
            } else {
                try {
                    claims.put(key, Long.parseLong(val));
                } catch (NumberFormatException e) {
                    claims.put(key, val);
                }
            }
        }
        return claims;
    }
}
