package base;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Config (hybrid):
 * Precedence for every key:
 *   1) Java system property  (-Dkey=value)
 *   2) Environment variable  (mapped names, e.g., SPOTIFY_CLIENT_ID)
 *   3) config.properties     (local file, ignored by git)
 *   4) Hardcoded default     (when applicable)
 *
 * Why this order?
 * - System/Env are CI-friendly and safer for secrets.
 * - config.properties makes local dev easy (no need to export vars).
 */
public class Config {

    // Properties loaded from ./config.properties (if present)
    private static final Properties FILE_PROPS = new Properties();

    static {
        // Lazily load config.properties from project root if it exists.
        try {
            Path p = Path.of("config.properties");
            if (Files.exists(p)) {
                try (InputStream in = new FileInputStream(p.toFile())) {
                    FILE_PROPS.load(in);
                    System.err.println("[Config] Loaded config.properties");
                }
            } else {
                // Not an error—many users will rely on env vars or -D props.
                System.err.println("[Config] config.properties not found; using env/-D/defaults");
            }
        } catch (Exception e) {
            // Do not fail the test run just because the file couldn't be read.
            System.err.println("[Config] Failed to load config.properties: " + e.getMessage());
        }
    }

    /** Public getters used across the framework. */
    public static String baseUrl()      { return resolve("base_url",  "https://api.spotify.com/v1"); }
    public static String clientId()     { return resolve("client_id",  null); }
    public static String clientSecret() { return resolve("client_secret", null); }
    public static String userToken()    { return resolve("user_token", null); }

    /**
     * Resolve a key using the precedence:
     *   -Dkey       → ENV (mapped) → config.properties → default
     */
    private static String resolve(String key, String defVal) {
        // 1) Java system property (e.g., -Dclient_id=xxx)
        String sys = System.getProperty(key);
        if (!isBlank(sys)) return sys;

        // 2) Environment variable (mapped names)
        String env = System.getenv(mapToEnvName(key));
        if (!isBlank(env)) return env;

        // 3) config.properties file
        String fileVal = FILE_PROPS.getProperty(key);
        if (!isBlank(fileVal)) return fileVal;

        // 4) Default
        return defVal == null ? "" : defVal;
    }

    /**
     * Map our logical keys to conventional env var names.
     * (Keeps backward compatibility with what we used earlier.)
     */
    private static String mapToEnvName(String key) {
        return switch (key) {
            case "client_id"     -> "SPOTIFY_CLIENT_ID";
            case "client_secret" -> "SPOTIFY_CLIENT_SECRET";
            case "user_token"    -> "SPOTIFY_USER_TOKEN";
            case "base_url"      -> "SPOTIFY_BASE_URL";
            default              -> key.toUpperCase();
        };
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}