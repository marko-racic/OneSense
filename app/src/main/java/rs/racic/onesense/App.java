package rs.racic.onesense;

import android.app.Application;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashReport.install(this);
    }
}
