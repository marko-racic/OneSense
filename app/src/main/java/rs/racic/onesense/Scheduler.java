package rs.racic.onesense;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

/** Zakazuje periodično osvežavanje (izbor korisnika; minimum koji dozvoljava Android je 15 min). */
public class Scheduler {

    private static final int JOB_ID = 101;
    private static final long MIN_MS = 15 * 60 * 1000L;

    public static void schedule(Context ctx) {
        try {
            long interval = new Prefs(ctx).intervalMin() * 60_000L;
            if (interval < MIN_MS) interval = MIN_MS;

            JobScheduler js = (JobScheduler) ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (js == null) return;
            JobInfo job = new JobInfo.Builder(JOB_ID, new ComponentName(ctx, AdJobService.class))
                    .setPeriodic(interval)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .build();
            js.schedule(job); // isti JOB_ID zamenjuje prethodni raspored
        } catch (Exception ignored) {
            // nikad ne sme srušiti UI
        }
    }
}
