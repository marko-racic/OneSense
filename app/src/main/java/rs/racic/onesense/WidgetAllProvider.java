package rs.racic.onesense;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 1x2 widžet: sva četiri perioda (danas/juče/7 dana/mesec) za svoj nalog.
 * Tap = odmah osveži. Ista podešavanja (nalog + boja) kao i kod 1x1.
 */
public class WidgetAllProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH = "rs.racic.onesense.ACTION_REFRESH_ALL";

    private static final int[] VALUE_IDS = {
            R.id.all_value_0, R.id.all_value_1, R.id.all_value_2, R.id.all_value_3};
    private static final int[] LABEL_IDS = {
            R.id.all_label_0, R.id.all_label_1, R.id.all_label_2, R.id.all_label_3};

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] appWidgetIds) {
        updateAll(ctx);
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (ACTION_REFRESH.equals(intent.getAction())) {
            Toast.makeText(ctx, R.string.refreshing, Toast.LENGTH_SHORT).show();
            PendingResult pending = goAsync();
            new Thread(() -> {
                try {
                    Refresher.refresh(ctx.getApplicationContext());
                } finally {
                    pending.finish();
                }
            }).start();
        } else {
            super.onReceive(ctx, intent);
        }
    }

    @Override
    public void onDeleted(Context ctx, int[] appWidgetIds) {
        Prefs prefs = new Prefs(ctx);
        for (int id : appWidgetIds) prefs.widgetRemove(id);
    }

    public static void updateAll(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        ComponentName name = new ComponentName(ctx, WidgetAllProvider.class);
        int[] ids = mgr.getAppWidgetIds(name);
        for (int id : ids) update(ctx, mgr, id);
    }

    private static void update(Context ctx, AppWidgetManager mgr, int widgetId) {
        Prefs prefs = new Prefs(ctx);
        RemoteViews views = new RemoteViews(ctx.getPackageName(), R.layout.widget_all);

        int colorIdx = prefs.widgetColor(widgetId);
        views.setInt(R.id.widget_root, "setBackgroundResource", WidgetProvider.bgDrawable(colorIdx));
        int num = Palette.num(colorIdx);
        int sub = Palette.sub(colorIdx);
        for (int id : VALUE_IDS) views.setTextColor(id, num);
        for (int id : LABEL_IDS) views.setTextColor(id, sub);
        views.setTextColor(R.id.all_sub, sub);

        String acc = prefs.widgetProp(widgetId);
        String json = prefs.widgetAll(widgetId);
        long last = prefs.widgetTime(widgetId);

        String[] values = null;
        if (json != null) {
            try {
                JSONArray arr = new JSONArray(json);
                values = new String[arr.length()];
                for (int i = 0; i < arr.length(); i++) values[i] = arr.optString(i, "—");
            } catch (Exception ignored) {
            }
        }
        for (int i = 0; i < VALUE_IDS.length; i++) {
            String v;
            if (acc == null) v = "…";
            else if (values != null && i < values.length) v = values[i];
            else v = last == 0 ? "…" : "!";
            views.setTextViewText(VALUE_IDS[i], v);
        }

        String subText;
        if (acc == null) {
            subText = ctx.getString(R.string.widget_sub_default);
        } else if (last > 0) {
            subText = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(last));
        } else {
            subText = ctx.getString(R.string.widget_sub_default);
        }
        views.setTextViewText(R.id.all_sub, subText);

        Intent tap = new Intent(ctx, WidgetAllProvider.class);
        tap.setAction(ACTION_REFRESH);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 1000 + widgetId, tap,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pi);

        mgr.updateAppWidget(widgetId, views);
    }
}
