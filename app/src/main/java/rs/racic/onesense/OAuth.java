package rs.racic.onesense;

import android.net.Uri;
import android.util.Base64;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/** Google OAuth 2.0 (PKCE flow za Android aplikacije, bez client secret). */
public class OAuth {

    public static final String SCOPE = "https://www.googleapis.com/auth/adsense.readonly";
    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";

    public static String redirectUri() {
        return BuildConfig.REDIRECT_URI;
    }

    public static String randomUrlToken() {
        byte[] b = new byte[32];
        new SecureRandom().nextBytes(b);
        return Base64.encodeToString(b, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    public static String pkceChallenge(String verifier) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.encodeToString(hash, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    public static String authUrl(String state, String verifier) throws Exception {
        return Uri.parse(AUTH_URL).buildUpon()
                .appendQueryParameter("client_id", BuildConfig.OAUTH_CLIENT_ID)
                .appendQueryParameter("redirect_uri", redirectUri())
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("scope", SCOPE)
                .appendQueryParameter("code_challenge", pkceChallenge(verifier))
                .appendQueryParameter("code_challenge_method", "S256")
                .appendQueryParameter("state", state)
                .appendQueryParameter("prompt", "consent") // tražimo refresh token
                .build().toString();
    }

    /** Vraća [accessToken, refreshToken, expiresInSeconds]. */
    public static String[] exchangeCode(String code, String verifier) throws Exception {
        Map<String, String> form = new HashMap<>();
        form.put("code", code);
        form.put("client_id", BuildConfig.OAUTH_CLIENT_ID);
        form.put("redirect_uri", redirectUri());
        form.put("grant_type", "authorization_code");
        form.put("code_verifier", verifier);
        return parseTokens(post(TOKEN_URL, form));
    }

    /** Vraća [accessToken, refreshToken, expiresInSeconds]. */
    public static String[] refresh(String refreshToken) throws Exception {
        Map<String, String> form = new HashMap<>();
        form.put("refresh_token", refreshToken);
        form.put("client_id", BuildConfig.OAUTH_CLIENT_ID);
        form.put("grant_type", "refresh_token");
        return parseTokens(post(TOKEN_URL, form));
    }

    private static String[] parseTokens(String json) throws Exception {
        JSONObject o = new JSONObject(json);
        if (o.has("error")) throw new Exception("OAuth: " + o.optString("error_description", o.getString("error")));
        return new String[]{
                o.getString("access_token"),
                o.optString("refresh_token", null),
                String.valueOf(o.optLong("expires_in", 3600))
        };
    }

    static String post(String url, Map<String, String> form) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> e : form.entrySet()) {
            if (body.length() > 0) body.append('&');
            body.append(Uri.encode(e.getKey())).append('=').append(Uri.encode(e.getValue()));
        }
        try (OutputStream os = c.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        return readBody(c);
    }

    static String readBody(HttpURLConnection c) throws Exception {
        int code = c.getResponseCode();
        java.io.InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
        StringBuilder sb = new StringBuilder();
        if (is != null) {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
        }
        if (code >= 400) throw new Exception("HTTP " + code + ": " + sb);
        return sb.toString();
    }
}
