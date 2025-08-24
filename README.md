# Spotify API — Rest Assured + TestNG

REST Assured + TestNG automation for the Spotify Web API.  
Read-only tests run with **client credentials**; user-scoped playlist tests run only when a **user token** is provided.  
Config is hybrid: **System props > Env vars > `config.properties` > defaults**.

---

## Project layout

src/test/java/
base/
Config.java          # hybrid config loader (env/-D/config.properties)
ApiBase.java         # common RequestSpecs (app vs user)
client/
SpotifyAuthClient.java  # OAuth: client_credentials → app token
util/
TokenManager.java    # caches app token until near-expiry
tests/
ReadOnlySpotifyIT.java  # search → artist → top-tracks → tracks
UserPlaylistIT.java     # /me → create → add → update → get → remove

---

## Prereqs
- Java 17, Maven
- Spotify **Client ID/Secret** (App)
- (Optional) **User token** with scopes:
    - `playlist-modify-private playlist-modify-public user-read-email`

---

## Configure secrets (pick one)

### A) `config.properties` (local dev)
1. Copy the example:
   ```bash
   cp config.example.properties config.properties
   ```
2. Fill in `client_id`, `client_secret`, and (optionally) `user_token`.

> **Note:** `config.properties` is in `.gitignore` — don’t commit real keys.

### B) Environment variables
```bash
export SPOTIFY_CLIENT_ID=xxx
export SPOTIFY_CLIENT_SECRET=yyy
export SPOTIFY_USER_TOKEN=zzz   # optional (only for user tests)
```

### C) Maven `-D` properties
Pass at runtime:
```bash
mvn -q -Dclient_id=xxx -Dclient_secret=yyy -Duser_token=zzz test -Dtest=tests.ReadOnlySpotifyIT
```
*(Adjust the `-Dtest=...` target as needed.)*

---

## Run

### Read-only suite (no user token required)
```bash
mvn -q test -Dtest=tests.ReadOnlySpotifyIT
```

### User playlist flow (requires user token + scopes)
```bash
mvn -q test -Dtest=tests.UserPlaylistIT
```

---

## Notes & troubleshooting
- **401 / 403:** Check credentials and required scopes; ensure the user token isn’t expired.
- **SLF4J NOP logger** (harmless). To enable simple logs in tests, add to `pom.xml` (test scope):
  ```xml
  <dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>2.0.13</version>
    <scope>test</scope>
  </dependency>
  
