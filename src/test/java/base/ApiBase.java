package base;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.specification.RequestSpecification;

/**
 * ApiBase class:
 * - Provides reusable RequestSpecification builders for RestAssured.
 * - One spec for "app token" flows (read-only API).
 * - Another spec for "user token" flows (playlist management, /me endpoints).
 *
 * Benefits:
 *   - Keeps headers and base URI consistent across tests.
 *   - Centralized logging setup.
 *   - Reduces duplication in test classes.
 */
public class ApiBase {

    /**
     * Build a RequestSpecification using an "app token" (client credentials).
     * Typically used for read-only endpoints:
     *   - /search
     *   - /artists/{id}
     *   - /audio-features
     *
     * @param appToken the bearer token obtained via client credentials.
     * @return reusable RestAssured RequestSpecification.
     */
    protected RequestSpecification appSpec(String appToken) {
        return new RequestSpecBuilder()
                .setBaseUri(Config.baseUrl())                 // Spotify API base URL
                .addHeader("Authorization", "Bearer " + appToken) // Inject Bearer token
                .log(LogDetail.URI)                           // Log only the request URI
                .build();
    }

    /**
     * Build a RequestSpecification using a "user token".
     * Typically used for user-scoped endpoints:
     *   - /me
     *   - /users/{id}/playlists
     *   - /playlists/{id}/tracks
     *
     * Reads user_token from Config (system property or env variable).
     *
     * @return reusable RestAssured RequestSpecification for user flows.
     */
    protected RequestSpecification userSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(Config.baseUrl())                      // Spotify API base URL
                .addHeader("Authorization", "Bearer " + Config.userToken()) // User's OAuth token
                .addHeader("Content-Type", "application/json")     // Most user APIs expect JSON body
                .log(LogDetail.URI)                                // Log request URI for visibility
                .build();
    }
}