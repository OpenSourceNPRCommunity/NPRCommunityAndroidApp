package com.nprcommunity.npronecommunity.Store;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.nio.channels.FileLock;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileCache {
    private static FileCache fileCache;
    private String TAG = "STORE.CACHE",
                    AUDIO_PATH,
                    IMAGE_PATH;
    public static final int MILLI_SECOND_IN_NANO = 1000000,
                        MILLI_NOTIFY = 750;
    private Context context;
    private Map<String, SoftReference<Bitmap>> imageCache = new ConcurrentHashMap<>();

    public enum Type {
        IMAGE,
        AUDIO
    }

    private FileCache(Context context) {
        AUDIO_PATH = context.getFilesDir() + File.separator + "AudioFiles";
        setupPath(AUDIO_PATH);
        IMAGE_PATH = context.getFilesDir() + File.separator + "ImageFiles";
        setupPath(IMAGE_PATH);
        this.context = context;
    }

    private void setupPath(String path) {
        //Set up fileCache
        File folder = new File(path);
        if(!folder.exists()) {
            if(!folder.mkdirs()) {
                Log.e(TAG, "FileCache: error making dir");
            }
        }
    }

    public static FileCache getInstances(Context context) {
        if(fileCache == null) {
            fileCache = new FileCache(context);
        }
        return fileCache;
    }

    public boolean fileExists(String filename, Type type) {
        String path = getFilePath(filename, type);
        if(path == null) {
            return false;
        }
        File file = new File(path);
        return file.exists();
    }

    private boolean waitTillFree(String filename, Type type, int millisMaxWait) {
        AtomicBoolean success = new AtomicBoolean(false);
        Thread thread = new Thread(() -> {
            while (imageCache.containsKey(filename)) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            }
            success.set(true);
        });
        thread.start();
        try {
            // wait for max seconds to wait before giving up
            thread.join(millisMaxWait);
        } catch (InterruptedException e) {
            Log.e(TAG, "getFileLock: waiting for lock", e);
        }
        return success.get();
    }

    public long getDirSize(Type type) {
        long size = 0;
        File[] files = getFiles(type);
        for (File file: files) {
            size += file.length();
        }
        return size;
    }

    public File getFile(@NonNull String url,@NonNull Type type) {
        return new File(getFilePath(url, type));
    }

    @Deprecated
    public List<List<String>> getFiles() {
        List<List<String>> arrayLists = new ArrayList<>(2);
        File dir = new File(IMAGE_PATH);
        arrayLists.add(Arrays.asList(dir.list()));
        dir = new File(AUDIO_PATH);
        arrayLists.add(Arrays.asList(dir.list()));
        return arrayLists;
    }

    public File[] getFiles(Type type) {
        File dir = null;
        switch (type) {
            case IMAGE:
                dir = new File(IMAGE_PATH);
                break;
            case AUDIO:
                dir = new File(AUDIO_PATH);
                break;
        }
        return dir.listFiles();
    }

    private String getFilePath(@NonNull String filename,@NonNull Type type) {
        byte[] digestedFileName = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            digestedFileName = messageDigest.digest(filename.getBytes());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "FileCache: error message digest not supported, this shouldnt happen", e);
        }
        String path = null, safeFilename;
        if (digestedFileName != null && digestedFileName.length > 0) {
            safeFilename = Base64.encodeToString(digestedFileName,
                    Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
        } else {
            //Should not ever be called but if it is may result in failing to download
            //data because of the length of the file name being to long and the use of urls
            safeFilename = Base64.encodeToString(filename.getBytes(),
                    Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
        }
        switch (type) {
            case IMAGE:
                path = IMAGE_PATH + File.separator + safeFilename;
                break;
            case AUDIO:
                path = AUDIO_PATH + File.separator + safeFilename;
                break;
        }
        return path;
    }

    private Bitmap getCacheImage(String filename) {
        if (imageCache.containsKey(filename)) {
            SoftReference<Bitmap> imageBitmap = imageCache.get(filename);
            if (imageBitmap != null) {
                return imageBitmap.get();
            }
        }
        return null;
    }

    public void getImage(String filename, CacheResponseImage cacheResponseImage,
                         ProgressCallback progressCallback) {
        String path = getFilePath(filename, Type.IMAGE);

        //check if image is in memory cache
        Bitmap cacheImage = getCacheImage(filename);
        if (cacheImage != null) {
            cacheResponseImage.callback(cacheImage);
            return;
        }

        // Check if file exists
        if(fileExists(filename, Type.IMAGE)) {
            //start a new thread to update the last modified
            // todo optimize this somehow in the future... to reduce threads
            Thread thread = new Thread(() -> {
                int MAX_ATTEMPTS = 5;
                int waitTime = 5;
                int startWaitTime = 6;
                File file = new File(getFilePath(filename, Type.IMAGE));
                for (int i = 0; i < MAX_ATTEMPTS; i++) {
                    Bitmap bitmap = getCacheImage(filename);
                    if (bitmap != null) {
                        // same image in thread, just check if another thread already cached
                        cacheResponseImage.callback(bitmap);
                    }
                    if (file.canWrite()) {
                        try {
                            bitmap = BitmapFactory.decodeFile(
                                    getFilePath(filename, Type.IMAGE)
                            );
                            if (bitmap != null) {
                                imageCache.put(filename, new SoftReference<>(bitmap));
                                cacheResponseImage.callback(bitmap);
                                if (file.setLastModified((new Date()).getTime())) {
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "getImage: failed to set last modified: " + filename, e);
                        }
                    }
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "getImage: could not sleep");
                    }
                    //exponential backoff, max wait time will be about ~3 seconds
                    waitTime *= startWaitTime;
                }
            });
            thread.start();
        } else {
            //The file does not exist
            DownloadPoolExecutor downloadPoolExecutor = DownloadPoolExecutor.getInstance(
                    DownloadPoolExecutor.Type.Image
            );
            downloadPoolExecutor.execute(
                new DownloadMediaTask(
                        context,
                        Type.IMAGE,
                        (fileInputStream, url) -> {
                            if (fileInputStream != null) {
                                Bitmap bitmap = BitmapFactory.decodeStream(fileInputStream);
                                SoftReference<Bitmap> softReference = new SoftReference<>(bitmap);
                                imageCache.put(filename, softReference);
                            } else {
                                // if there is another thread downloading, then
                                // wait till the file is free
                                if (fileExists(filename, Type.IMAGE)) {
                                    if (!waitTillFree(filename, Type.IMAGE, 5000)) {
                                        Log.e(TAG, "getImage: Failed to download image: " + filename);
                                        return;
                                    }
                                }
                            }

                            Bitmap tmpCacheImage = getCacheImage(filename);
                            cacheResponseImage.callback(tmpCacheImage);
                        },
                        filename,
                        progressCallback)
            );
        }
    }

    public DownloadMediaTask getAudio(String filename, boolean forceRedownload,
                                      CacheResponseMedia cacheResponseMedia, ProgressCallback progressCallback) {
        String path = getFilePath(filename, Type.AUDIO);
        if(!forceRedownload && fileExists(filename, Type.AUDIO)) {
            //The file exists load in the audio
            FileLock fileLock = null;
            try {
                File file = new File(path);
                FileInputStream in = new FileInputStream(file);
                fileLock = in.getChannel().lock(0L, Long.MAX_VALUE, true);
                cacheResponseMedia.callback(in, filename);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "getAudio: " + filename, e);
                cacheResponseMedia.callback(null, null);
            } catch (IOException e) {
                Log.e(TAG, "getAudio: " + filename, e);
                cacheResponseMedia.callback(null, null);
            } finally {
                if (fileLock != null) {
                    try {
                        fileLock.release();
                    } catch (IOException e) {
                        Log.e(TAG, "getAudio: " + filename, e);
                        cacheResponseMedia.callback(null, null);
                    }
                }
            }
        } else {
            //The file does not exist
            DownloadPoolExecutor downloadPoolExecutor = DownloadPoolExecutor.getInstance(
                    DownloadPoolExecutor.Type.Audio
            );
            DownloadMediaTask downloadMediaTask = new DownloadMediaTask(context, Type.AUDIO, cacheResponseMedia, filename,
                            progressCallback);
            downloadPoolExecutor.execute(downloadMediaTask);
            return downloadMediaTask;
        }
        return null;
    }

    public FileInputStream getInputStream(String filename, Type type) throws FileNotFoundException {
        String path = getFilePath(filename, type);
        File file = new File(path);
        return new FileInputStream(file);
    }

    public void deleteAllFiles() {
        File[] files = getFiles(Type.IMAGE);
        for (File file: files) {
            if (!file.delete()) {
                Log.e(TAG, "deleteAllFiles: failed to delete image: " + file.getName());
            }
        }
        files = getFiles(Type.AUDIO);
        for (File file: files) {
            if (!file.delete()) {
                Log.e(TAG, "deleteAllFiles: failed to delete audio: " + file.getName());
            }
        }
    }

    public boolean deleteFileIfExists(String filename, Type type) {
        if (fileExists(filename, type)) {
            String path = getFilePath(filename, type);
            File file = new File(path);
            //todo maybe race condition for deleting file, downloading....will have to do this in other places
            return file.delete();
        }
        return true;
    }

    public FileInputStream saveFile(String filename,
                                    InputStream inputStream,
                                    Type type,
                                    ProgressCallback progressCallback,
                                    int total,
                                    Boolean stopDownload) throws FileNotFoundException {
        String path = getFilePath(filename, type);
        File file = new File(path);
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(file);
            FileLock fileLock = null;
            int count = 0;
            try {
                fileLock = outputStream.getChannel().lock();
                byte[] buffer = new byte[1024];
                int len;
                int prevProgress = count;
                long start = System.nanoTime();
                long end;
                while ((len = inputStream.read(buffer)) > -1) {
                    if (stopDownload) {
                        Log.d(TAG, "saveFile: stopDownload requested breaking");
                        return null;
                    }
                    outputStream.write(buffer, 0, len);
                    count += len;
                    end = System.nanoTime();
                    if ((end-start)/MILLI_SECOND_IN_NANO >= MILLI_NOTIFY) {
                        if (progressCallback != null) {
                            progressCallback.updateProgress(
                                    count,
                                    total,
                                    (count - prevProgress)*(1000/MILLI_NOTIFY)
                            );
                        }
                        start = end;
                        prevProgress = count;
                    }
                }
                //set finished to download
                if (progressCallback != null) {
                    progressCallback.updateProgress(total, total, 0);
                }
            } finally {
                if (fileLock != null) {
                    fileLock.release();
                }
            }
            outputStream.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "saveFile: filenot found outputstream filename:" + filename, e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "saveFile: outputstream filename:" + filename, e);
            return null;
        }
        return getInputStream(filename, type);
    }

    public static void createCacheJobService(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        //only schedule the job if it is not pending
        if (jobScheduler != null) {
            boolean hasBeenScheduled = false;
            for (JobInfo jobInfo : jobScheduler.getAllPendingJobs() ) {
                if (jobInfo.getId() == CacheClearJobService.JOB_ID) {
                    hasBeenScheduled = true;
                    break;
                }
            }
            if (!hasBeenScheduled) {
                jobScheduler.schedule(createJobBuilder(context).build());
            }
        }
    }

    public static void setUpCacheJobService(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        //only schedule the job if it is not pending
        if (jobScheduler != null) {
            jobScheduler.schedule(createJobBuilder(context).build());
        }
    }

    private static JobInfo.Builder createJobBuilder(Context context) {
        ComponentName serviceComponent = new ComponentName(context, CacheClearJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(
                CacheClearJobService.JOB_ID,
                serviceComponent
        );
        builder.setRequiresDeviceIdle(true);
        builder.setRequiresCharging(false);
        builder.setPeriodic(8 * 60 * 60 * 1000); // every 8 hours or 3 times a day it checks
        return builder;
    }
}
