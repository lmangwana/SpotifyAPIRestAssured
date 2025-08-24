package tests;

import base.ApiBase;
import base.Config;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import util.TokenManager;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * UserPlaylistIT:
 * - Requires a valid USER TOKEN (OAuth Authorization Code flow) with scopes:
 *     playlist-modify-private playlist-modify-public user-read-email
 * - Flow: /me → create playlist → add track → update playlist → verify → remove track
 * - Skips the entire class if user_token is not provided.
 *
 * Run tip:
 *   mvn -q -Dclient_id=... -Dclient_secret=... -Duser_token=... test -Dtest=tests.UserPlaylistIT
 *
 * Notes:
 * - We reuse the app token solely to SEARCH for a track, then use the user token
 *   to operate on the user’s playlist resources.
 */
public class UserPlaylistIT extends ApiBase {

    @BeforeClass
    public void guard() {
        // Skip gracefully if user_token is missing; read-only pipeline can still pass
        if (Config.userToken() == null || Config.userToken().isBlank()) {
            throw new SkipException("user_token not provided; skipping user-scoped tests.");
        }
    }

    @Test
    public void create_add_update_get_remove_playlist_flow() {
        // 1) Who am I? (/me)
        String userId =
                given().spec(userSpec())
                        .when().get("/me")
                        .then().statusCode(200)
                        .extract().path("id");

        // 2) Create a new private playlist under this user
        String name = "QA – RA " + System.currentTimeMillis();
        String playlistId =
                given().spec(userSpec())
                        .body("{\"name\":\"" + name + "\",\"public\":false}")
                        .when().post("/users/{uid}/playlists", userId)
                        .then().statusCode(201)
                        .extract().path("id");

        // 3) Find a track ID using the app token (search endpoint is read-only)
        String appToken = TokenManager.getAppToken();
        String trackId =
                given().spec(appSpec(appToken))
                        .queryParam("q", "Radiohead")
                        .queryParam("type", "track")
                        .queryParam("limit", 1)
                        .when().get("/search")
                        .then().statusCode(200)
                        .extract().path("tracks.items[0].id");

        // 4) Add the track to the new playlist
        given().spec(userSpec())
                .body("{\"uris\":[\"spotify:track:" + trackId + "\"]}")
                .when().post("/playlists/{pid}/tracks", playlistId)
                .then().statusCode(anyOf(is(200), is(201)));

        // 5) Update playlist name/description (PUT /playlists/{id})
        given().spec(userSpec())
                .body("{\"name\":\"" + name + " (Updated)\",\"description\":\"Updated via RA\",\"public\":false}")
                .when().put("/playlists/{pid}", playlistId)
                .then().statusCode(200);

        // 6) Verify update took effect
        given().spec(userSpec())
                .when().get("/playlists/{pid}", playlistId)
                .then().statusCode(200)
                .body("name", equalTo(name + " (Updated)"));

        // 7) Remove the track we added (DELETE /playlists/{id}/tracks)
        given().spec(userSpec())
                .body("{\"tracks\":[{\"uri\":\"spotify:track:" + trackId + "\"}]}")
                .when().delete("/playlists/{pid}/tracks", playlistId)
                .then().statusCode(200);
    }
}