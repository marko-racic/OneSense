package rs.racic.onesense;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Ekrancic koji se otvara pri dodavanju widzeta: izbor AdSense naloga i boje.
 * Nalog je read-only — bira se iskljucivo iz Google liste.
 */
public class WidgetConfigActivity extends Activity {

    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private TextView accNameText;
    private TextView accIdText;
    private LinearLayout colorRow;
    private TextView colorName;
    private int selectedColor = 0;
    private String pickedAccId;
    private String pickedAccName;
    private String[] paletteNames;
    private boolean wide; // 1x4 widžet vuče sve periode odjednom
    private int selectedRange = 0; // 1x1: koji period prikazuje

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_widget_config);

        widgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return; }

        // Koji provajder traži podešavanja? 1x1 = danas, 1x2 = svi periodi.
        AppWidgetManager mgr = AppWidgetManager.getInstance(this);
        ComponentName provider = mgr.getAppWidgetInfo(widgetId) == null
                ? null : mgr.getAppWidgetInfo(widgetId).provider;
        wide = provider != null && WidgetAllProvider.class.getName().equals(provider.getClassName());

        accNameText = findViewById(R.id.config_property_name);
        accIdText = findViewById(R.id.config_property_id);
        colorRow = findViewById(R.id.config_color_row);
        colorName = findViewById(R.id.config_color_name);
        Button pickButton = findViewById(R.id.config_pick_button);
        Button saveButton = findViewById(R.id.config_save_button);
        findViewById(R.id.config_property_box).setOnClickListener(v -> showAccountPicker());

        // podešavanja postojećeg widžeta (ako se ponovo otvara)
        Prefs prefs = new Prefs(this);
        String existing = prefs.widgetProp(widgetId);
        if (existing != null) {
            pickedAccId = existing;
            pickedAccName = prefs.widgetName(widgetId);
        }
        selectedColor = prefs.widgetColor(widgetId);

        renderAccount();
        paletteNames = getResources().getStringArray(R.array.palette_names);
        buildColorRow();
        selectedRange = prefs.widgetRange(widgetId);
        buildRangeRow();
        pickButton.setOnClickListener(v -> showAccountPicker());
        saveButton.setOnClickListener(v -> save());
    }

    /** Četiri dugmeta perioda za 1x1; kod 1x4 se sakrivaju (on ionako prikazuje sve). */
    private void buildRangeRow() {
        LinearLayout row = findViewById(R.id.config_range_row);
        if (wide) {
            findViewById(R.id.config_range_label).setVisibility(View.GONE);
            row.setVisibility(View.GONE);
            return;
        }
        row.removeAllViews();
        String[] names = getResources().getStringArray(R.array.range_names);
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3,
                getResources().getDisplayMetrics());
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            Button b = new Button(this);
            b.setText(names[i]);
            b.setAllCaps(false);
            b.setTextSize(12f);
            b.setBackgroundResource(i == selectedRange ? R.drawable.btn_primary : R.drawable.btn_secondary);
            b.setTextColor(getColor(i == selectedRange ? R.color.accent_dark : R.color.text));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(margin, 0, margin, 0);
            b.setLayoutParams(lp);
            b.setOnClickListener(v -> {
                selectedRange = index;
                buildRangeRow();
            });
            row.addView(b);
        }
    }

    private void renderAccount() {
        if (pickedAccId == null) {
            accNameText.setText("—");
            accIdText.setText(getString(R.string.hint_tap_to_pick));
        } else {
            accNameText.setText(pickedAccName != null && !pickedAccName.isEmpty()
                    ? pickedAccName : shortId(pickedAccId));
            accIdText.setText(pickedAccName != null && !pickedAccName.isEmpty()
                    ? getString(R.string.id_prefix, shortId(pickedAccId)) : "");
        }
    }

    private static String shortId(String id) {
        return id.startsWith("accounts/") ? id.substring("accounts/".length()) : id;
    }

    private void buildColorRow() {
        colorRow.removeAllViews();
        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 44, getResources().getDisplayMetrics());
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 7, getResources().getDisplayMetrics());

        for (int i = 0; i < Palette.COLORS.length; i++) {
            final int index = i;
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(margin, 0, margin, 0);
            lp.gravity = Gravity.CENTER_VERTICAL;
            dot.setLayoutParams(lp);
            dot.setContentDescription(paletteNames[i]);
            dot.setOnClickListener(v -> {
                selectedColor = index;
                buildColorRow();
            });
            colorRow.addView(dot);
        }
        updateColorDots();
    }

    /** Kruzic u boji pozadine palete, prsten u boji broja; deblji prsten = izabrano. */
    private void updateColorDots() {
        for (int i = 0; i < colorRow.getChildCount(); i++) {
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(Palette.bg(i));
            d.setStroke(i == selectedColor ? 6 : 2, Palette.num(i));
            colorRow.getChildAt(i).setBackground(d);
        }
        colorName.setText(paletteNames[Palette.idx(selectedColor)]);
    }

    private void showAccountPicker() {
        final Prefs prefs = new Prefs(this);
        if (prefs.refreshToken() == null) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.err_not_configured)
                    .setMessage(R.string.config_login_needed)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        List<AdSenseAdmin.Account> cached = Refresher.cachedList(prefs);
        if (cached != null && !cached.isEmpty()) {
            showAccountDialog(prefs, cached);
            return;
        }
        syncAccountList(prefs);
    }

    private void syncAccountList(final Prefs prefs) {
        Toast.makeText(this, R.string.loading_accounts, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            List<AdSenseAdmin.Account> list;
            try {
                list = Refresher.syncAccounts(prefs);
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        getString(R.string.err_accounts, String.valueOf(e.getMessage())),
                        Toast.LENGTH_LONG).show());
                return;
            }
            final List<AdSenseAdmin.Account> finalList = list;
            runOnUiThread(() -> {
                if (finalList.isEmpty()) {
                    Toast.makeText(this, R.string.err_accounts_empty, Toast.LENGTH_SHORT).show();
                    return;
                }
                showAccountDialog(prefs, finalList);
            });
        }).start();
    }

    private void showAccountDialog(final Prefs prefs, final List<AdSenseAdmin.Account> list) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.pick_account_title)
                .setAdapter(new AccountAdapter(this, list), (d, which) -> {
                    AdSenseAdmin.Account a = list.get(which);
                    pickedAccId = a.id;
                    pickedAccName = a.displayName;
                    renderAccount();
                })
                .setNeutralButton(R.string.sync_adsense, (d, w) -> syncAccountList(prefs))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void save() {
        if (pickedAccId == null || pickedAccId.isEmpty()) {
            Toast.makeText(this, R.string.err_not_configured, Toast.LENGTH_SHORT).show();
            return;
        }
        Prefs prefs = new Prefs(this);
        prefs.widgetProp(widgetId, pickedAccId);
        prefs.widgetColor(widgetId, selectedColor);
        prefs.widgetRange(widgetId, selectedRange);
        prefs.widgetName(widgetId,
                pickedAccName != null && !pickedAccName.isEmpty() ? pickedAccName : null);

        // odmah povuci iznos(e) za ovaj widzet
        new Thread(() -> {
            try {
                if (wide) {
                    String token = Refresher.validToken(prefs);
                    org.json.JSONArray arr = new org.json.JSONArray();
                    for (String r : AdSenseApi.RANGES) {
                        arr.put(AdSenseApi.format(AdSenseApi.earnings(token, pickedAccId, r)));
                    }
                    prefs.widgetAllResult(widgetId, arr.toString(), System.currentTimeMillis());
                    WidgetAllProvider.updateAll(WidgetConfigActivity.this);
                } else {
                    String range = AdSenseApi.RANGES[selectedRange % AdSenseApi.RANGES.length];
                    AdSenseApi.Earnings e = AdSenseApi.earnings(Refresher.validToken(prefs), pickedAccId, range);
                    prefs.widgetResult(widgetId, AdSenseApi.format(e), System.currentTimeMillis());
                    WidgetProvider.updateAll(WidgetConfigActivity.this);
                }
            } catch (Exception e) {
                prefs.widgetError(widgetId, String.valueOf(e.getMessage()));
            }
        }).start();

        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        setResult(RESULT_OK, result);
        finish();
    }
}
