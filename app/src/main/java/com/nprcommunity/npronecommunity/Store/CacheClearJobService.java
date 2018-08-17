package com.nprcommunity.npronecommunity.Store;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import java.io.File;
import java.util.Date;

public class CacheClearJobService extends JobService {

    private static final String TAG = "CacheClearJobService";
    public static final int JOB_ID = 656588732;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        FileCache fileCache = FileCache.getInstances(getApplicationContext());

        //delete existing image files
        File[] imageFiles = fileCache.getFiles(FileCache.Type.IMAGE);
        for(File file: imageFiles) {
            // if the file exists and the last modified is older then 3 days
            if (file.exists() && file.lastModified() > (new Date()).getTime() - (3 * 24 * 60 * 60 * 1000)) {
                Log.d(TAG, "onStartJob: deleting file: " + file.getAbsolutePath() + " "
                        + file.getName());
                if (!file.delete()) {
                    Log.e(TAG, "onStartJob: failed to delete: " + file.getAbsolutePath() +
                            " " + file.getName());
                }
            }
        }

        // reschedule the job
        FileCache.setUpCacheJobService(getApplicationContext());
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
