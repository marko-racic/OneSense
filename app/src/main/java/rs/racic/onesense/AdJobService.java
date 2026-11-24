package rs.racic.onesense;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class AdJobService extends JobService {

    @Override
    public boolean onStartJob(JobParameters params) {
        new Thread(() -> {
            Refresher.refresh(AdJobService.this);
            jobFinished(params, false);
        }).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
