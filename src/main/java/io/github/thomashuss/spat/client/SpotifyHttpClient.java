package io.github.thomashuss.spat.client;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements communication between the Spotify client and server.
 */
abstract class SpotifyHttpClient
{
    private static final Pattern QUERY_PATTERN = Pattern.compile("([^=?&]+)=([^&]+)");
    private static final String API_SCOPE = "playlist-read-private playlist-read-collaborative playlist-modify-private playlist-modify-public user-library-modify user-library-read";
    private static final Set<String> SCOPE_SET = new HashSet<>(Arrays.asList(API_SCOPE.split(" ")));
    private static final String PKCE_POSSIBLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final URL SPOTIFY_TOKEN_URL;

    static {
        try {
            SPOTIFY_TOKEN_URL = new URI("https://accounts.spotify.com/api/token").toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static final int PKCE_CODE_LENGTH = 64;
    private final Base64.Encoder b64Encoder;
    private final MessageDigest digest;
    private final Token token;
    private String clientId;
    private String loginState;
    private String pkceCodeVerifier;
    private URI redirectUri;

    public SpotifyHttpClient()
    {
        b64Encoder = Base64.getEncoder();
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        token = new Token();
    }

    private static Map<String, String> decodeQuery(URI uri)
    {
        Map<String, String> decodedQuery = new HashMap<>();
        Matcher matcher = QUERY_PATTERN.matcher(uri.getQuery());
        while (matcher.find()) {
            decodedQuery.put(matcher.group(1), URLDecoder.decode(matcher.group(2), StandardCharsets.UTF_8));
        }
        return decodedQuery;
    }

    public void setClientId(String clientId)
    {
        this.clientId = clientId;
    }

    public void setRedirectUri(URI redirectUri)
    {
        this.redirectUri = redirectUri;
    }

    public boolean isTokenValid()
    {
        return token.isValid();
    }

    public Token getToken()
    {
        return token;
    }

    private BufferedReader getConnectionReader(URL target)
    throws IOException, SpotifyClientException
    {
        HttpsURLConnection con;
        int code;

        refreshAccessToken();
        con = (HttpsURLConnection) target.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", token.getAccessAuthorization());

        code = con.getResponseCode();
        if (code == HttpsURLConnection.HTTP_OK) {
            return new BufferedReader(new InputStreamReader(con.getInputStream()));
        } else {
            throw new SpotifyClientHttpException(code);
        }
    }

    private BufferedReader getConnectionReader(URL target, String method, String data,
                                               String type, boolean shouldAuthenticate)
    throws IOException, SpotifyClientHttpException
    {
        int responseCode;
        HttpsURLConnection con = (HttpsURLConnection) target.openConnection();

        con.setRequestMethod(method);
        if (shouldAuthenticate) {
            con.setRequestProperty("Authorization", token.getAccessAuthorization());
        }
        con.setRequestProperty("Content-Length", Integer.toString(data.getBytes().length));
        con.setRequestProperty("Content-Type", type);
        con.setDoOutput(true);
        try (OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream())) {
            writer.write(data);
        }

        responseCode = con.getResponseCode();
        if (responseCode == HttpsURLConnection.HTTP_OK || responseCode == HttpsURLConnection.HTTP_CREATED
                || responseCode == HttpsURLConnection.HTTP_ACCEPTED) {
            return new BufferedReader(new InputStreamReader(con.getInputStream()));
        } else {
            throw new SpotifyClientHttpException(responseCode);
        }
    }

    /**
     * Sends a GET request to Spotify and creates a BufferedReader from the response.
     *
     * @param target URL of Spotify object
     * @return BufferedReader of JSON output from Spotify
     * @throws IOException                on I/O errors
     * @throws SpotifyClientHttpException if there is an unexpected HTTP error when communicating with Spotify
     */
    BufferedReader getAPIReader(URI target)
    throws IOException, SpotifyClientException
    {
        refreshAccessToken();
        return getConnectionReader(target.toURL());
    }

    BufferedReader getAPIReader(URI target, String method, String data)
    throws IOException, SpotifyClientException
    {
        refreshAccessToken();
        return getConnectionReader(target.toURL(), method, data, "application/json", true);
    }

    BufferedReader getAPIReader(URL target, String method, String data)
    throws IOException, SpotifyClientException
    {
        refreshAccessToken();
        return getConnectionReader(target, method, data, "application/json", true);
    }

    /**
     * Sends a POST request to Spotify and creates a BufferedReader from the response.
     *
     * @param target URL of Spotify object
     * @return BufferedReader of JSON output from Spotify
     * @throws IOException                on I/O errors
     * @throws SpotifyClientHttpException if there is an unexpected HTTP error when communicating with Spotify
     */
    BufferedReader getAPIReader(URI target, String data)
    throws IOException, SpotifyClientException
    {
        return getAPIReader(target, "POST", data);
    }

    /**
     * Sends a POST request to Spotify and creates a BufferedReader from the response.
     *
     * @param target URL of Spotify object
     * @return BufferedReader of JSON output from Spotify
     * @throws IOException                on I/O errors
     * @throws SpotifyClientHttpException if there is an unexpected HTTP error when communicating with Spotify
     */
    BufferedReader getAPIReader(URL target, String data)
    throws IOException, SpotifyClientException
    {
        return getAPIReader(target, "POST", data);
    }

    private String pkceCodeGenerate()
    {
        SecureRandom random = new SecureRandom();
        StringBuilder ret = new StringBuilder(PKCE_CODE_LENGTH);

        for (int i = 0; i < PKCE_CODE_LENGTH; i++) {
            ret.append(PKCE_POSSIBLE.charAt(random.nextInt(PKCE_POSSIBLE.length())));
        }

        return ret.toString();
    }

    public URI getLoginRedirect()
    {
        pkceCodeVerifier = pkceCodeGenerate();
        loginState = UUID.randomUUID().toString();
        try {
            return new URI("https://accounts.spotify.com/authorize?response_type=code&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                    + "&scope=" + URLEncoder.encode(API_SCOPE, StandardCharsets.UTF_8)
                    + "&state=" + URLEncoder.encode(loginState, StandardCharsets.UTF_8)
                    + "&code_challenge_method=S256&code_challenge=" + URLEncoder.encode(
                    b64Encoder.encodeToString(digest.digest(pkceCodeVerifier.getBytes(StandardCharsets.UTF_8)))
                            .replace("=", "").replace('+', '-').replace('/', '_'), StandardCharsets.UTF_8)
                    + "&redirect_uri=" + URLEncoder.encode(redirectUri.toString(), StandardCharsets.UTF_8));
        } catch (URISyntaxException e) {
            return null;
        }
    }

    abstract SpotifyToken parseToken(BufferedReader reader)
    throws IOException;

    private void refreshAccessToken(String out)
    throws IOException, SpotifyClientHttpException, SpotifyAuthenticationException
    {
        SpotifyToken spotifyToken;
        try (BufferedReader reader = getConnectionReader(SPOTIFY_TOKEN_URL, "POST", out,
                "application/x-www-form-urlencoded", false)) {
            spotifyToken = parseToken(reader);
        }

        if (!(new HashSet<>(Arrays.asList(spotifyToken.scope.split(" ")))).equals(SCOPE_SET)) {
            throw new SpotifyAuthenticationException("Scope mismatch.");
        }
        token.setAccessAuthorization(spotifyToken.tokenType + ' ' + spotifyToken.accessToken);
        token.setExpires(Instant.now().plusSeconds(spotifyToken.expiresIn));
        token.setRefreshToken(spotifyToken.refreshToken);
    }

    public void loginCallback(URI callback)
    throws IOException, SpotifyClientException
    {
        Map<String, String> req = decodeQuery(callback);
        if (!Objects.equals(req.get("state"), loginState)) {
            throw new SpotifyAuthenticationException("Authentication state mismatch.");
        }
        refreshAccessToken("client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&code=" + URLEncoder.encode(req.get("code"), StandardCharsets.UTF_8)
                + "&code_verifier=" + URLEncoder.encode(pkceCodeVerifier, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri.toString(), StandardCharsets.UTF_8)
                + "&grant_type=authorization_code");
    }

    private void refreshAccessToken()
    throws IOException, SpotifyClientException
    {
        if (token == null) {
            throw new SpotifyClientStateException("No Spotify access token exists.");
        }
        Instant expires = token.getExpires();
        if (expires != null && Instant.now().isBefore(expires)) {
            return;
        }
        refreshAccessToken("grant_type=refresh_token&refresh_token="
                + URLEncoder.encode(token.getRefreshToken(), StandardCharsets.UTF_8)
                + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8));
    }
}
