package rs.racic.onesense;

import android.content.Context;
import android.content.SharedPreferences;

/** Jednostavno čuvanje stanja aplikacije. */
public class Prefs {

    private final SharedPreferences sp;

    public Prefs(Context ctx) {
        sp = ctx.getSharedPreferences("onesense", Context.MODE_PRIVATE);
    }

    // --- OAuth ---
    public String refreshToken() { return sp.getString("refresh_token", null); }
    public void refreshToken(String v) { sp.edit().putString("refresh_token", v).apply(); }
    public String accessToken() { return sp.getString("access_token", null); }
    public long accessExpiresAt() { return sp.getLong("access_expires", 0); }
    public void tokens(String access, long expiresAt) {
        sp.edit().putString("access_token", access).putLong("access_expires", expiresAt).apply();
    }
    public String pendingState() { return sp.getString("oauth_state", null); }
    public String pendingVerifier() { return sp.getString("oauth_verifier", null); }
    public void pendingOAuth(String state, String verifier) {
        sp.edit().putString("oauth_state", state).putString("oauth_verifier", verifier).apply();
    }
    public void clearAuth() {
        sp.edit().remove("refresh_token").remove("access_token").remove("access_expires").apply();
    }

    // --- Podešavanja ---
    public String accountId() { return sp.getString("account_id", ""); }
    public void accountId(String v) { sp.edit().putString("account_id", v).apply(); }
    public int intervalMin() { return sp.getInt("interval_min", 15); }
    public void intervalMin(int v) { sp.edit().putInt("interval_min", v).apply(); }
    /** Period na glavnom ekranu: indeks u AdSenseApi.RANGES (tap rotira). */
    public int rangeIdx() { return sp.getInt("range_idx", 0); }
    public void rangeIdx(int v) { sp.edit().putInt("range_idx", v).apply(); }

    // --- Poslednji rezultat (iznos je već formatiran: "12.34 €") ---
    public String lastAmount() { return sp.getString("last_amount", null); }
    public long lastUpdate() { return sp.getLong("last_update", 0); }
    public String lastError() { return sp.getString("last_error", null); }
    public void result(String amount, long when) {
        sp.edit().putString("last_amount", amount).putLong("last_update", when).remove("last_error").apply();
    }
    public void error(String msg) { sp.edit().putString("last_error", msg).apply(); }

    // --- Po widzetu (svaki svoj nalog i boju) ---
    public String widgetProp(int id) { return sp.getString("w_prop_" + id, null); }
    public void widgetProp(int id, String v) { sp.edit().putString("w_prop_" + id, v).apply(); }
    public String widgetName(int id) { return sp.getString("w_name_" + id, null); }
    public void widgetName(int id, String v) { sp.edit().putString("w_name_" + id, v).apply(); }
    public int widgetColor(int id) { return sp.getInt("w_color_" + id, 0); }
    public void widgetColor(int id, int v) { sp.edit().putInt("w_color_" + id, v).apply(); }
    /** 1x1 widžet: koji period prikazuje (indeks u AdSenseApi.RANGES). */
    public int widgetRange(int id) { return sp.getInt("w_range_" + id, 0); }
    public void widgetRange(int id, int v) { sp.edit().putInt("w_range_" + id, v).apply(); }
    public String widgetAmount(int id) { return sp.getString("w_amount_" + id, null); }
    public long widgetTime(int id) { return sp.getLong("w_time_" + id, 0); }
    public void widgetResult(int id, String amount, long when) {
        sp.edit().putString("w_amount_" + id, amount).putLong("w_time_" + id, when).remove("w_err_" + id).apply();
    }
    /** 1x2 widžet: JSON od 4 formatirana iznosa (po redu AdSenseApi.RANGES). */
    public String widgetAll(int id) { return sp.getString("w_all_" + id, null); }
    public void widgetAllResult(int id, String json, long when) {
        sp.edit().putString("w_all_" + id, json).putLong("w_time_" + id, when).remove("w_err_" + id).apply();
    }
    public void widgetError(int id, String msg) { sp.edit().putString("w_err_" + id, msg).apply(); }
    public void widgetRemove(int id) {
        sp.edit().remove("w_prop_" + id).remove("w_name_" + id).remove("w_color_" + id)
                .remove("w_range_" + id)
                .remove("w_amount_" + id).remove("w_all_" + id)
                .remove("w_time_" + id).remove("w_err_" + id).apply();
    }

    public boolean isConfigured() {
        return !accountId().isEmpty() && refreshToken() != null;
    }

    // --- Keš imena naloga (JSON: id -> displayName) ---
    public String namesCache() { return sp.getString("acc_names", null); }
    public long namesAt() { return sp.getLong("acc_names_at", 0); }
    public void namesCache(String json, long when) {
        sp.edit().putString("acc_names", json).putLong("acc_names_at", when).apply();
    }
    public String nameFor(String accId) {
        try {
            String v = new org.json.JSONObject(namesCache()).optString(accId, null);
            return v == null || v.isEmpty() ? null : v;
        } catch (Exception e) {
            return null;
        }
    }

    // --- Keš cele liste naloga (JSON array: id, name) ---
    public String accList() { return sp.getString("acc_list", null); }
    public long accListAt() { return sp.getLong("acc_list_at", 0); }
    public void accList(String json, long when) {
        sp.edit().putString("acc_list", json).putLong("acc_list_at", when).apply();
    }
}
