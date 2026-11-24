package rs.racic.onesense;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity {

    private TextView statusNumber, statusText, statusRange;
    private TextView accountName, accountIdText;
    private Button loginButton, refreshButton, logoutButton, pickButton, intervalButton;
    private boolean autoLoginTriggered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusNumber = findViewById(R.id.status_number);
        statusText = findViewById(R.id.status_text);
        statusRange = findViewById(R.id.status_range);
        // Tap na karticu iznosa rotira period: danas → juče → 7 dana → mesec.
        findViewById(R.id.status_box).setOnClickListener(v -> {
            Prefs p = new Prefs(this);
            p.rangeIdx((p.rangeIdx() + 1) % AdSenseApi.RANGES.length);
            statusNumber.setText("…");
            render();
            doRefresh();
        });
        accountName = findViewById(R.id.property_name);
        accountIdText = findViewById(R.id.property_id);
        loginButton = findViewById(R.id.login_button);
        refreshButton = findViewById(R.id.refresh_button);
        logoutButton = findViewById(R.id.logout_button);
        pickButton = findViewById(R.id.pick_button);
        intervalButton = findViewById(R.id.interval_button);
        intervalButton.setOnClickListener(v -> showIntervalPicker());

        View accountBox = findViewById(R.id.property_box);
        accountBox.setOnClickListener(v -> showAccountPicker());

        // uvek otvori na vrhu, bez obzira na staru poziciju skrola
        ScrollView scroll = findViewById(R.id.main_scroll);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_UP));

        loginButton.setOnClickListener(v -> startLogin());
        refreshButton.setOnClickListener(v -> doRefresh());
        pickButton.setOnClickListener(v -> showAccountPicker());
        logoutButton.setOnClickListener(v -> {
            new Prefs(this).clearAuth();
            render();
        });

        // Trag pada od proslog pokretanja? Korisniku se pokazuje kratka poruka,
        // a sirovi stack trace ide u clipboard tek ako on to zatrazi.
        String crash = CrashReport.consume(this);
        if (crash != null) {
            final String trace = crash;
            new AlertDialog.Builder(this)
                    .setTitle(R.string.crash_title)
                    .setMessage(R.string.crash_message)
                    .setPositiveButton(R.string.crash_copy, (dialog, which) -> {
                        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(ClipData.newPlainText("OneSense", trace));
                        Toast.makeText(this, R.string.crash_copied, Toast.LENGTH_SHORT).show();
                    })
                    .setNeutralButton(android.R.string.ok, null)
                    .show();
        } else if (savedInstanceState == null && new Prefs(this).refreshToken() == null) {
            // Pri prvom pokretanju (nema tokena) — odmah idi na prijavu
            autoLoginTriggered = true;
            startLogin();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Prefs prefs = new Prefs(this);
        render();
        if (prefs.isConfigured()) {
            Scheduler.schedule(this);
            doRefresh();
        } else if (prefs.refreshToken() != null && prefs.accountId().isEmpty() && !pickerShown) {
            // Prijavljeni smo, ali nalog još nije izabran → ponudi listu
            showAccountPicker();
        }
    }

    private boolean pickerShown = false;

    private void startLogin() {
        try {
            String state = OAuth.randomUrlToken();
            String verifier = OAuth.randomUrlToken();
            new Prefs(this).pendingOAuth(state, verifier);
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(OAuth.authUrl(state, verifier)));
            startActivity(i);
        } catch (Exception e) {
            toast(e.getMessage());
        }
    }

    /** Lista naloga za izbor — iz lokalnog keša ako postoji, inače sa Google-a. */
    private void showAccountPicker() {
        final Prefs prefs = new Prefs(this);
        if (prefs.refreshToken() == null) {
            toast(getString(R.string.err_not_configured));
            return;
        }
        List<AdSenseAdmin.Account> cached = Refresher.cachedList(prefs);
        if (cached != null && !cached.isEmpty()) {
            showAccountDialog(prefs, cached);
            return;
        }
        syncAccountList(prefs);
    }

    /** Force povlačenje liste sa Google-a, upis u keš, pa prikaz dijaloga. */
    private void syncAccountList(final Prefs prefs) {
        pickerShown = true;
        toast(getString(R.string.loading_accounts));
        new Thread(() -> {
            List<AdSenseAdmin.Account> list;
            String error = null;
            try {
                list = Refresher.syncAccounts(prefs);
            } catch (Exception e) {
                list = null;
                error = e.getMessage() == null ? e.toString() : e.getMessage();
            }
            final List<AdSenseAdmin.Account> finalList = list;
            final String finalError = error;
            runOnUiThread(() -> {
                pickerShown = false;
                if (finalList == null) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.err_accounts_title)
                            .setMessage(finalError)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                    return;
                }
                if (finalList.isEmpty()) {
                    toast(getString(R.string.err_accounts_empty));
                    return;
                }
                // AdSense nalog je u praksi jedan — izaberi ga bez pitanja.
                if (finalList.size() == 1 && prefs.accountId().isEmpty()) {
                    prefs.accountId(finalList.get(0).id);
                    render();
                    doRefresh();
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
                    String id = list.get(which).id;
                    prefs.accountId(id);
                    render();
                    doRefresh();
                })
                .setNeutralButton(R.string.sync_adsense, (d, w) -> syncAccountList(prefs))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** Dijalog za izbor učestalosti pozadinskog osvežavanja. */
    private void showIntervalPicker() {
        final Prefs prefs = new Prefs(this);
        final String[] labels = getResources().getStringArray(R.array.interval_labels);
        final int[] values = getResources().getIntArray(R.array.interval_values);
        int cur = prefs.intervalMin();
        int checked = 0;
        for (int i = 0; i < values.length; i++) if (values[i] == cur) checked = i;

        new AlertDialog.Builder(this)
                .setTitle(R.string.interval_title)
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    prefs.intervalMin(values[which]);
                    Scheduler.schedule(this); // zamenjuje postojeći raspored
                    render();
                    d.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void doRefresh() {
        Prefs prefs = new Prefs(this);
        if (!prefs.isConfigured()) {
            toast(getString(R.string.err_not_configured));
            return;
        }
        statusNumber.setText("…");
        new Thread(() -> Refresher.refresh(this, (ok, msg) ->
                runOnUiThread(() -> {
                    render();
                    if (!ok) toast(msg); // uspeh je tih — iznos se samo ažurira
                }))).start();
    }

    private void render() {
        Prefs prefs = new Prefs(this);
        boolean authed = prefs.refreshToken() != null;
        loginButton.setText(authed ? R.string.login_again : R.string.login);
        logoutButton.setVisibility(authed ? View.VISIBLE : View.GONE);
        refreshButton.setEnabled(authed);
        pickButton.setEnabled(authed);

        String aid = prefs.accountId();
        if (aid.isEmpty()) {
            accountName.setText("—");
            accountIdText.setText(authed ? getString(R.string.hint_tap_to_pick)
                    : getString(R.string.hint_login_needed));
        } else {
            String name = prefs.nameFor(aid);
            accountName.setText(name != null ? name : shortId(aid));
            accountIdText.setText(name != null ? getString(R.string.id_prefix, shortId(aid)) : "");
        }

        String[] labels = getResources().getStringArray(R.array.interval_labels);
        int[] values = getResources().getIntArray(R.array.interval_values);
        int cur = prefs.intervalMin();
        String label = labels[0];
        for (int i = 0; i < values.length; i++) if (values[i] == cur) label = labels[i];
        intervalButton.setText(getString(R.string.refresh_every, label));

        String amount = prefs.lastAmount();
        String error = prefs.lastError();
        String[] rangeNames = getResources().getStringArray(R.array.range_names);
        int ri = prefs.rangeIdx() % AdSenseApi.RANGES.length;
        if (amount != null) {
            statusNumber.setText(amount);
            statusText.setText(error != null
                    ? getString(R.string.status_earnings_range, rangeNames[ri]) + " — "
                            + getString(R.string.status_prev_failed)
                    : getString(R.string.status_earnings_range, rangeNames[ri]));
        } else if (error != null) {
            statusNumber.setText("—");
            statusText.setText(error);
        } else {
            statusNumber.setText("—");
            statusText.setText(R.string.status_no_data);
        }
        // sve četiri oznake, trenutna boldovana — tap na karticu rotira
        android.text.SpannableStringBuilder hint = new android.text.SpannableStringBuilder();
        for (int i = 0; i < rangeNames.length; i++) {
            if (i > 0) hint.append(" · ");
            int start = hint.length();
            hint.append(rangeNames[i]);
            if (i == ri) {
                hint.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                        start, hint.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        statusRange.setText(hint);
    }

    private static String shortId(String id) {
        return id.startsWith("accounts/") ? id.substring("accounts/".length()) : id;
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
