package rs.racic.onesense;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Hvata redirect iz browsera (com.googleusercontent.apps.XXX:/oauth2redirect?code=...&state=...)
 * i zamenjuje kod za tokene.
 */
public class OAuthRedirectActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handle(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handle(intent);
    }

    private void handle(Intent intent) {
        Uri data = intent == null ? null : intent.getData();
        if (data == null) { finish(); return; }

        String state = data.getQueryParameter("state");
        String code = data.getQueryParameter("code");
        String error = data.getQueryParameter("error");
        Prefs prefs = new Prefs(this);
        String expected = prefs.pendingState();

        if (error != null) {
            done(getString(R.string.err_login_failed) + ": " + error);
        } else if (state == null || expected == null || !state.equals(expected)) {
            done(getString(R.string.err_state_mismatch));
        } else if (code == null) {
            done(getString(R.string.err_no_code));
        } else {
            String verifier = prefs.pendingVerifier();
            new Thread(() -> {
                String msg;
                try {
                    String[] t = OAuth.exchangeCode(code, verifier);
                    long expiresAt = System.currentTimeMillis() + Long.parseLong(t[2]) * 1000L;
                    prefs.tokens(t[0], expiresAt);
                    if (t[1] != null) prefs.refreshToken(t[1]);
                    msg = getString(R.string.ok_logged_in);
                    Scheduler.schedule(this);
                    Refresher.refresh(this);
                } catch (Exception e) {
                    msg = getString(R.string.err_login_failed) + ": " + e.getMessage();
                }
                final String m = msg;
                runOnUiThread(() -> done(m));
            }).start();
            return;
        }
        finish();
    }

    private void done(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
    }
}
