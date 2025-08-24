package util;

import client.SpotifyAuthClient;
import base.Config;

/**
 * TokenManager:
 * - Centralized cache for the app (client-credentials) token.
 * - Avoids hitting the token endpoint for every test.
 * - Refreshes the token slightly before actual expiry to be safe.
 *
 * Thread-safety:
 * - getAppToken() is synchronized so parallel tests don’t race on refresh.
 */
public class TokenManager {
    // Cached token and its expiry (epoch millis)
    private static String appToken;
    private static long   expiresAtMs = 0;

    /**
     * Returns a valid app token from cache or refreshes if missing/expired.
     *
     * Expiry handling:
     * - Spotify returns expires_in (seconds). We subtract a 30s buffer.
     * - This minimizes the chance of using a token that's about to expire mid-request.
     *
     * @return String bearer token suitable for Authorization: Bearer <token>
     */
    public static synchronized String getAppToken() {
        long now = System.currentTimeMillis();

        // If no token yet OR current time is past the stored expiry → refresh
        if (appToken == null || now >= expiresAtMs) {
            // Pull clientId/secret from Config (system props or env)
            String clientId     = Config.clientId();
            String clientSecret = Config.clientSecret();

            if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
                throw new IllegalStateException(
                        "Spotify client credentials are missing. " +
                                "Provide -Dclient_id / -Dclient_secret or set SPOTIFY_CLIENT_ID / SPOTIFY_CLIENT_SECRET env vars."
                );
            }

            // Request a fresh token
            SpotifyAuthClient.TokenResponse tr = SpotifyAuthClient.getAppToken(clientId, clientSecret);

            // Cache it
            appToken = tr.access_token;

            // Compute a "refresh slightly early" expiry time (expires_in is in seconds)
            long ttlMs = Math.max(0, (tr.expires_in - 30)) * 1000L; // minus 30s buffer
            expiresAtMs = now + ttlMs;
        }

        return appToken;
    }
}