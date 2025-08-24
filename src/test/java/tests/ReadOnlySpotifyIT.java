package tests;

import base.ApiBase;
import io.restassured.path.json.JsonPath;
import org.testng.annotations.Test;
import util.TokenManager;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * ReadOnlySpotifyIT:
 * - Uses the "app token" (client credentials) for READ-ONLY endpoints.
 * - Mirrors a simple Postman collection: search → artist → top-tracks → audio-features.
 * - Also includes a negative test for invalid artist IDs.
 *
 * Run tip:
 *   mvn -q -Dclient_id=... -Dclient_secret=... test -Dtest=tests.ReadOnlySpotifyIT
 */
public class ReadOnlySpotifyIT extends ApiBase {

    @Test
    public void search_artist_then_get_by_id_then_toptracks_then_audio_features() {
        // Obtain a valid app token from the cache (auto-refresh if expired)
        String appToken = TokenManager.getAppToken();

        // 1) Search for an artist by name (use "Radiohead" as a stable example)
        String artistId =
                given().spec(appSpec(appToken))
                        .queryParam("q", "Radiohead")   // query string
                        .queryParam("type", "artist")   // search type
                        .queryParam("limit", 1)         // only need the first hit
                        .when().get("/search")
                        .then().statusCode(200)
                        .body("artists.items[0].id", notNullValue())
                        .extract().path("artists.items[0].id");

        // 2) Get the artist by ID (sanity: response id matches the one we extracted)
        given().spec(appSpec(appToken))
                .when().get("/artists/{id}", artistId)
                .then().statusCode(200)
                .body("id", equalTo(artistId))
                .body("name", not(isEmptyOrNullString()));

        // 3) Get artist top tracks (market required; use ZA for local context)
        JsonPath top = given().spec(appSpec(appToken))
                .queryParam("market", "ZA")
                .when().get("/artists/{id}/top-tracks", artistId)
                .then().statusCode(200)
                .body("tracks.size()", greaterThan(0))
                .extract().jsonPath();


    }

    @Test
    public void negative_invalid_artist_returns_404_or_400() {
        // Some invalid IDs return 400 (bad format), others 404 (not found).
        String appToken = TokenManager.getAppToken();

        given().spec(appSpec(appToken))
                .when().get("/artists/{id}", "not-a-real-id")
                .then().statusCode(anyOf(is(400), is(404)));
    }
}