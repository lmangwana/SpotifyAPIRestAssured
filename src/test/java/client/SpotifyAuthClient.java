package client;

import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;

/**
 * SpotifyAuthClient:
 * - Responsible for obtaining an "app token" via the Client Credentials flow.
 * - This token grants READ-ONLY access (no user context) to public endpoints.
 *
 * OAuth endpoint:
 *   POST https://accounts.spotify.com/api/token
 *   Auth: Basic <base64(clientId:clientSecret)>
 *   Body: grant_type=client_credentials (x-www-form-urlencoded)
 *
 * Typical uses with app token:
 *   - /search, /artists/{id}, /audio-features, etc.
 */
public class SpotifyAuthClient {

    /** Simple DTO for the token endpoint response. */
    public static class TokenResponse {
        public String access_token;  // The actual bearer token to use
        public String token_type;    // "Bearer"
        public int    expires_in;    // Lifetime in seconds (usually 3600)
    }

    /**
     * Obtain an application token using client credentials.
     *
     * @param clientId     Spotify client ID
     * @param clientSecret Spotify client secret
     * @return TokenResponse {access_token, token_type, expires_in}
     */
    public static TokenResponse getAppToken(String clientId, String clientSecret) {
        return given()
                // Basic auth header with clientId:clientSecret (RestAssured encodes it for you)
                .auth().preemptive().basic(clientId, clientSecret)
                // Content type MUST be application/x-www-form-urlencoded
                .contentType(ContentType.URLENC)
                // grant_type form param per OAuth spec
                .formParam("grant_type", "client_credentials")
                // Token endpoint (Accounts service, not Web API base)
                .post("https://accounts.spotify.com/api/token")
                // Expect 200 OK with token payload
                .then().statusCode(200)
                .extract().as(TokenResponse.class);
    }
}