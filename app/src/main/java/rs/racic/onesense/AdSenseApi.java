package rs.racic.onesense;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/** AdSense Management API v2 — procenjena zarada po periodu. */
public class AdSenseApi {

    /** Periodi koje main ekran rotira tap-om (API enum-i). */
    public static final String[] RANGES = {"TODAY", "YESTERDAY", "LAST_7_DAYS", "MONTH_TO_DATE"};

    public static class Earnings {
        public final double amount;
        public final String currency; // ISO-4217, npr. "EUR"

        Earnings(double amount, String currency) {
            this.amount = amount;
            this.currency = currency;
        }
    }

    /** Zarada danas — widžet uvek vuče ovaj period. */
    public static Earnings earningsToday(String accessToken, String accountName) throws Exception {
        return earnings(accessToken, accountName, "TODAY");
    }

    /** reports:generate sa željenim dateRange i metric ESTIMATED_EARNINGS. */
    public static Earnings earnings(String accessToken, String accountName, String range) throws Exception {
        String url = "https://adsense.googleapis.com/v2/" + accountName
                + "/reports:generate?dateRange=" + range + "&metrics=ESTIMATED_EARNINGS";
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        c.setRequestProperty("Authorization", "Bearer " + accessToken);
        JSONObject o = new JSONObject(OAuth.readBody(c));

        // Valuta dolazi u zaglavlju kolone metrika (type=METRIC_CURRENCY).
        String currency = "EUR";
        JSONArray headers = o.optJSONArray("headers");
        if (headers != null) {
            for (int i = 0; i < headers.length(); i++) {
                JSONObject h = headers.getJSONObject(i);
                if ("METRIC_CURRENCY".equals(h.optString("type"))) {
                    String cc = h.optString("currencyCode", null);
                    if (cc != null && !cc.isEmpty()) {
                        currency = cc;
                        break;
                    }
                }
            }
        }

        // totals.cells[0] je prvi (jedini) traženi metric; prazan izveštaj = 0.
        JSONObject totals = o.optJSONObject("totals");
        double amount = 0;
        if (totals != null) {
            JSONArray cells = totals.optJSONArray("cells");
            if (cells != null && cells.length() > 0) {
                amount = Double.parseDouble(cells.getJSONObject(0).optString("value", "0"));
            }
        }
        return new Earnings(amount, currency);
    }

    /** "12.34 €" — simbol za česte valute, inače ISO kod. */
    public static String format(Earnings e) {
        return format(e.amount, e.currency);
    }

    public static String format(double amount, String currency) {
        String sym;
        switch (currency == null ? "" : currency) {
            case "EUR": sym = "€"; break;
            case "USD": sym = "$"; break;
            case "GBP": sym = "£"; break;
            case "JPY": sym = "¥"; break;
            default: sym = currency; break;
        }
        return String.format(Locale.US, "%.2f %s", amount, sym);
    }
}
