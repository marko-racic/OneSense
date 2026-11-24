package rs.racic.onesense;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/** Upisuje stack trace svakog pada u prefs; pri sledecem startu se prikazuje korisniku. */
public class CrashReport {

    public static void install(final Context ctx) {
        final SharedPreferences sp = ctx.getSharedPreferences("crash", Context.MODE_PRIVATE);
        final Thread.UncaughtExceptionHandler old = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                try {
                    StringBuilder sb = new StringBuilder();
                    Throwable top = e;
                    while (top != null) {
                        sb.append(Log.getStackTraceString(top)).append("\n--- caused by ---\n");
                        top = top.getCause();
                    }
                    sp.edit().putString("last_crash", sb.toString()).commit();
                } catch (Throwable ignored) {
                }
                if (old != null) old.uncaughtException(t, e);
            }
        });
    }

    /** Vraca trag poslednjeg pada (i brise ga), ili null. */
    public static String consume(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences("crash", Context.MODE_PRIVATE);
        String s = sp.getString("last_crash", null);
        if (s != null) sp.edit().remove("last_crash").apply();
        return s;
    }
}
