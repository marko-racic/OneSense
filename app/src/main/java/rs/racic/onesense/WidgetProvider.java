package rs.racic.onesense;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 1x1 widžet: procenjena AdSense zarada danas za svoj nalog, u izabranoj boji. Tap = odmah osveži. */
public class WidgetProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH = "rs.racic.onesense.ACTION_REFRESH";

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
        ComponentName name = new ComponentName(ctx, WidgetProvider.class);
        int[] ids = mgr.getAppWidgetIds(name);
        for (int id : ids) update(ctx, mgr, id);
    }

    private static void update(Context ctx, AppWidgetManager mgr, int widgetId) {
        Prefs prefs = new Prefs(ctx);
        RemoteViews views = new RemoteViews(ctx.getPackageName(), R.layout.widget_visit);

        int colorIdx = prefs.widgetColor(widgetId);
        views.setInt(R.id.widget_root, "setBackgroundResource", bgDrawable(colorIdx));
        views.setTextColor(R.id.widget_number, Palette.num(colorIdx));
        views.setTextColor(R.id.widget_sub, Palette.sub(colorIdx));
        views.setTextColor(R.id.widget_name, Palette.sub(colorIdx));

        String acc = prefs.widgetProp(widgetId);
        String amount = prefs.widgetAmount(widgetId);
        long last = prefs.widgetTime(widgetId);

        String number;
        if (acc == null) {
            number = "…";
        } else if (amount != null) {
            number = amount;
        } else {
            number = last == 0 ? "…" : "!";
        }
        views.setTextViewText(R.id.widget_number, number);

        String sub;
        if (acc == null) {
            sub = ctx.getString(R.string.widget_sub_default);
        } else if (last > 0) {
            sub = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(last));
        } else {
            sub = ctx.getString(R.string.widget_sub_default);
        }
        views.setTextViewText(R.id.widget_sub, sub);

        // ispod broja: naziv perioda (ne ime naloga) — bira se pri dodavanju widžeta
        String[] rangeNames = ctx.getResources().getStringArray(R.array.range_names);
        String rangeName = rangeNames[prefs.widgetRange(widgetId) % rangeNames.length];
        views.setTextViewText(R.id.widget_name, rangeName);

        // Tap na ceo widžet → odmah osveži
        Intent tap = new Intent(ctx, WidgetProvider.class);
        tap.setAction(ACTION_REFRESH);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, widgetId, tap,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pi);

        mgr.updateAppWidget(widgetId, views);
    }

    /** Deljeno sa WidgetAllProvider: pozadina po indeksu palete. */
    static int bgDrawable(int colorIdx) {
        switch (colorIdx) {
            case 1: return R.drawable.widget_bg_c1;
            case 2: return R.drawable.widget_bg_c2;
            case 3: return R.drawable.widget_bg_c3;
            case 4: return R.drawable.widget_bg_c4;
            case 5: return R.drawable.widget_bg_c5;
            case 6: return R.drawable.widget_bg_c6;
            default: return R.drawable.widget_bg_c0;
        }
    }
}
