package rs.racic.onesense;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;

import java.util.List;

/**
 * Osvežava: glavni ekran (globalni nalog) i svaki widžet za svoj nalog.
 * Uvek pozivati sa pozadinske niti.
 */
public class Refresher {

    public interface Callback {
        void done(boolean ok, String message);
    }

    public static boolean refresh(Context ctx) {
        return refresh(ctx, null);
    }

    public static boolean refresh(Context ctx, Callback cb) {
        Prefs prefs = new Prefs(ctx);
        String message;
        boolean ok = false;

        // 1) glavni ekran
        try {
            if (!prefs.isConfigured()) {
                message = ctx.getString(R.string.err_not_configured);
            } else {
                String token = validToken(prefs);
                String range = AdSenseApi.RANGES[prefs.rangeIdx() % AdSenseApi.RANGES.length];
                AdSenseApi.Earnings e = AdSenseApi.earnings(token, prefs.accountId(), range);
                prefs.result(AdSenseApi.format(e), System.currentTimeMillis());
                message = ctx.getString(R.string.ok_updated);
                ok = true;
            }
        } catch (Exception ex) {
            prefs.error(ex.getMessage() == null ? ex.toString() : ex.getMessage());
            message = ex.getMessage() == null ? ex.toString() : ex.getMessage();
        }

        // 2) svaki widžet za svoj nalog (1x1: danas, 1x2: svi periodi)
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
            int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, WidgetProvider.class));
            int[] wideIds = mgr.getAppWidgetIds(new ComponentName(ctx, WidgetAllProvider.class));
            if (ids.length + wideIds.length > 0) {
                String token = validToken(prefs);
                ensureNames(prefs, token);
                for (int id : ids) {
                    String prop = prefs.widgetProp(id);
                    if (prop == null) continue;
                    if (prefs.widgetName(id) == null) {
                        String n = prefs.nameFor(prop);
                        if (n != null) prefs.widgetName(id, n);
                    }
                    try {
                        String range = AdSenseApi.RANGES[prefs.widgetRange(id) % AdSenseApi.RANGES.length];
                        AdSenseApi.Earnings e = AdSenseApi.earnings(token, prop, range);
                        prefs.widgetResult(id, AdSenseApi.format(e), System.currentTimeMillis());
                        ok = true;
                    } catch (Exception ex) {
                        prefs.widgetError(id, String.valueOf(ex.getMessage()));
                    }
                }
                for (int id : wideIds) {
                    String prop = prefs.widgetProp(id);
                    if (prop == null) continue;
                    if (prefs.widgetName(id) == null) {
                        String n = prefs.nameFor(prop);
                        if (n != null) prefs.widgetName(id, n);
                    }
                    try {
                        org.json.JSONArray arr = new org.json.JSONArray();
                        for (String r : AdSenseApi.RANGES) {
                            arr.put(AdSenseApi.format(AdSenseApi.earnings(token, prop, r)));
                        }
                        prefs.widgetAllResult(id, arr.toString(), System.currentTimeMillis());
                        ok = true;
                    } catch (Exception ex) {
                        prefs.widgetError(id, String.valueOf(ex.getMessage()));
                    }
                }
            }
        } catch (Exception ignored) {
        }

        WidgetProvider.updateAll(ctx);
        WidgetAllProvider.updateAll(ctx);
        if (cb != null) cb.done(ok, message);
        return ok;
    }

    /** Upisuje listu naloga u keš imena (id -> displayName). */
    public static void cacheNames(Prefs prefs, List<AdSenseAdmin.Account> list) {
        try {
            org.json.JSONObject o = new org.json.JSONObject();
            for (AdSenseAdmin.Account a : list) {
                if (a.displayName != null && !a.displayName.isEmpty()) o.put(a.id, a.displayName);
            }
            prefs.namesCache(o.toString(), System.currentTimeMillis());
        } catch (Exception ignored) {
        }
    }

    /** Čuva listu u keš (i osvežava keš imena); sortira po nazivu. */
    public static void storeList(Prefs prefs, List<AdSenseAdmin.Account> list) {
        List<AdSenseAdmin.Account> sorted = new java.util.ArrayList<>(list);
        sorted.sort((a, b) -> {
            String an = a.displayName == null ? "" : a.displayName.toLowerCase();
            String bn = b.displayName == null ? "" : b.displayName.toLowerCase();
            return an.compareTo(bn);
        });
        cacheNames(prefs, sorted);
        try {
            prefs.accList(AdSenseAdmin.toJson(sorted), System.currentTimeMillis());
        } catch (Exception ignored) {
        }
    }

    /** Keširana lista naloga ili null ako je nema. */
    public static List<AdSenseAdmin.Account> cachedList(Prefs prefs) {
        try {
            return AdSenseAdmin.fromJson(prefs.accList());
        } catch (Exception e) {
            return null;
        }
    }

    /** Force sync liste sa Google-a; upisuje je u keš. */
    public static List<AdSenseAdmin.Account> syncAccounts(Prefs prefs) throws Exception {
        List<AdSenseAdmin.Account> list = AdSenseAdmin.listAccounts(validToken(prefs));
        storeList(prefs, list);
        return list;
    }

    /** Osvežava keš imena najviše jednom na 6 sati (uz validan token). */
    public static void ensureNames(Prefs prefs, String token) {
        if (System.currentTimeMillis() - prefs.accListAt() < 6 * 3600_000L) return;
        try {
            storeList(prefs, AdSenseAdmin.listAccounts(token));
        } catch (Exception ignored) {
        }
    }

    /** Vraća važeći access token, po potrebi ga osvežava (keširan u prefs — jedan refresh po rundi). */
    public static String validToken(Prefs prefs) throws Exception {
        String token = prefs.accessToken();
        if (token != null && System.currentTimeMillis() < prefs.accessExpiresAt() - 60_000) {
            return token;
        }
        String[] t = OAuth.refresh(prefs.refreshToken());
        long expiresAt = System.currentTimeMillis() + Long.parseLong(t[2]) * 1000L;
        prefs.tokens(t[0], expiresAt);
        if (t[1] != null) prefs.refreshToken(t[1]);
        return t[0];
    }
}
