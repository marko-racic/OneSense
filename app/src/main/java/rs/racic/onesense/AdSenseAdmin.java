package rs.racic.onesense;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** AdSense Management API v2 — lista naloga (pub-…) do kojih nalog ima pristup. */
public class AdSenseAdmin {

    public static class Account {
        public final String id;          // "accounts/pub-1234567890123456"
        public final String displayName;

        Account(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        /** Kratki prikaz bez prefiksa "accounts/". */
        public String shortId() {
            return id.startsWith("accounts/") ? id.substring("accounts/".length()) : id;
        }

        @Override
        public String toString() {
            String label = displayName == null || displayName.isEmpty() ? "(bez imena)" : displayName;
            return label + "  —  " + shortId();
        }
    }

    public static List<Account> listAccounts(String accessToken) throws Exception {
        List<Account> out = new ArrayList<>();
        JSONObject resp = get(accessToken, "https://adsense.googleapis.com/v2/accounts?pageSize=100");
        JSONArray accounts = resp.optJSONArray("accounts");
        if (accounts == null) return out;
        for (int i = 0; i < accounts.length(); i++) {
            JSONObject a = accounts.getJSONObject(i);
            String name = a.optString("name", ""); // "accounts/pub-..."
            if (name.isEmpty()) continue;
            out.add(new Account(name, a.optString("displayName", "")));
        }
        return out;
    }

    /** Serijalizacija liste za lokalni keš. */
    public static String toJson(List<Account> list) {
        JSONArray a = new JSONArray();
        for (Account acc : list) {
            JSONObject o = new JSONObject();
            try {
                o.put("id", acc.id);
                o.put("name", acc.displayName == null ? "" : acc.displayName);
            } catch (Exception ignored) {
            }
            a.put(o);
        }
        return a.toString();
    }

    public static List<Account> fromJson(String json) throws Exception {
        List<Account> out = new ArrayList<>();
        if (json == null || json.isEmpty()) return out;
        JSONArray a = new JSONArray(json);
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            out.add(new Account(o.optString("id"), o.optString("name")));
        }
        return out;
    }

    private static JSONObject get(String accessToken, String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        c.setRequestProperty("Authorization", "Bearer " + accessToken);

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
        return new JSONObject(sb.toString());
    }
}
