package com.nprcommunity.npronecommunity.Background;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.nprcommunity.npronecommunity.API.APIRecommendations;
import com.nprcommunity.npronecommunity.API.Shared;
import com.nprcommunity.npronecommunity.Background.Queue.LineUpQueue;
import com.nprcommunity.npronecommunity.R;
import com.nprcommunity.npronecommunity.Store.DownloadPoolExecutor;
import com.nprcommunity.npronecommunity.Store.DownloadMediaTask;
import com.nprcommunity.npronecommunity.Store.FileCache;
import com.nprcommunity.npronecommunity.Store.ProgressCallback;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.nprcommunity.npronecommunity.Background.BackgroundAudioService.ACTION;

/**
 * Class used for downloading media in the queue
 * Will deal with deleting
 * This class is a bit confusing I will admit it now that I am looking back on. I know there
 * are Future and Promises that existin Java, but I wanted to try my own implementation. It
 * turns out to be a bit confusing.
 * The real point of this class is that downloading is a bit of a mess:
 *  1. Background service inits
 *  2. User cancels download
 *  3. Background cancels download
 *  4. On download complete, notify related parties
 *
 *  All while this is going on the program could be called from a number of different threads,
 *  so it gets a wee bit wild.
 *
 *  I think the best thing to do here is to just dive into the code and please contact
 *  me if you see anything blatantly wrong.
 */
public class MediaQueueDownloadManager implements MediaQueueChangedListener {
    private final String TAG = "QUEUEDOWNLOADMANAGER";
    private final MediaQueueManager mediaQueueManager;
    private final MediaSessionCompat mediaSessionCompat;
    private final FileCache fileCache;
    private final Context context;
    private final Map<String, ThreadListener> threadListeners = new HashMap<>();
    private final ConcurrentMap<String, DownloadMediaTask> downloadTasks = new ConcurrentHashMap<>();
    private final Object lockThreadListener = new Object();
    private final Object lockDownloadTasks = new Object();

    protected MediaQueueDownloadManager(MediaQueueManager mediaQueueManager,
                                        MediaSessionCompat mediaSessionCompat,
                                        FileCache fileCache,
                                        Context context) {
        this.mediaQueueManager = mediaQueueManager;
        this.mediaSessionCompat = mediaSessionCompat;
        this.fileCache = fileCache;
        this.context = context;
        setupQueueDownloads();
    }

    private void setupQueueDownloads() {
        List<MediaSessionCompat.QueueItem> itemJSONList = mediaQueueManager.getMediaQueue();
        for (int i = 0; i < itemJSONList.size(); i++) {
            APIRecommendations.ItemJSON itemJSON = (APIRecommendations.ItemJSON) itemJSONList.get(i)
                    .getDescription().getExtras().getSerializable(LineUpQueue.ApiItem.API_ITEM.name());
            APIRecommendations.AudioJSON audioJSON = itemJSON.links.getValidAudio();
            Shared.Progress progress = audioJSON.progressTracker;
            if (!progress.isFullyDownloaded()) {
                //isnt fully downloaded setup download
                Bundle bundleMediaProgress = new Bundle();
                bundleMediaProgress.putString(
                    ACTION,
                    BackgroundAudioService.Action.MEDIA_DOWNLOADING_PROGRESS.name()
                );
                bundleMediaProgress.putIntArray(
                        BackgroundAudioService.Action.MEDIA_DOWNLOADING_PROGRESS.name(),
                        new int[] {
                                i, //the current position
                                0, //in nanoseconds
                                0, //get percentage
                        }
                );
                bundleMediaProgress.putString(
                        BackgroundAudioService.ActionExtras.MEDIA_PREPARED_HREF.name(),
                        itemJSON.href
                );
                mediaSessionCompat.sendSessionEvent(
                        BackgroundAudioService.Action.MEDIA_DOWNLOADING_PROGRESS.name(),
                        bundleMediaProgress
                );
                startDownload(i,itemJSON);
            }
        }
    }

    private void startDownload(int i, APIRecommendations.ItemJSON itemJSON) {
        synchronized (lockThreadListener) {
            //do not recreate download tasks if one already exists
            if (downloadTasks.get(itemJSON.href) != null) {
                Log.d(TAG, "startDownload: already downloading [" + itemJSON.href + "]");
                return;
            }
        }

        APIRecommendations.AudioJSON audioJSON = itemJSON.links.getValidAudio();
        ProgressCallback progressCallback = (progress, total, speed) -> {
            //this is progress for audio loading
            Log.d(TAG, "startDownload: progress loading audio: "
                    + audioJSON.href + " at "
                    + " progress [" + progress + "] total [" + total + "] "
                    + " percent [" + ((double)progress)/((double)total));
            audioJSON.progressTracker.setProgress(progress);
            audioJSON.progressTracker.setTotal(total);

            //force save
            mediaQueueManager.forceSaveQueue();

            Bundle bundleMediaProgress = new Bundle();
            bundleMediaProgress.putString(
                    ACTION,
                    BackgroundAudioService.Action.MEDIA_DOWNLOADING_PROGRESS.name()
            );
            double percentage = audioJSON.progressTracker.getPercentage();
            bundleMediaProgress.putIntArray(
                    BackgroundAudioService.Action.MEDIA_DOWNLOADING_PROGRESS.name(),
                    new int[] {
                            i, //the current position
                            speed, //in nanoseconds
                            (int)(percentage*100), //get percentage
                    }
            );
            bundleMediaProgress.putString(
                    BackgroundAudioService.ActionExtras.MEDIA_PREPARED_HREF.name(),
                    itemJSON.href
            );
            mediaSessionCompat.sendSessionEvent(
                    BackgroundAudioService.Action.MEDIA_DOWNLOADING_PROGRESS.name(),
                    bundleMediaProgress
            );
        };

        DownloadMediaTask downloadMediaTask = fileCache.getAudio(
                audioJSON.href,
                !audioJSON.progressTracker.isFullyDownloaded(),
                (FileInputStream fileInputStream, String url) -> {
                    if (fileInputStream == null) {
                        //something failed set full downloaded to false
                        audioJSON.progressTracker.setIsFullyDownloaded(false);

                        synchronized (lockThreadListener) {
                            ThreadListener threadListener = threadListeners.get(itemJSON.href);
                            if (threadListener != null && threadListener.isAlive()) {
                                synchronized (lockThreadListener) {
                                    threadListener.notify();
                                }
                            } else {
                                // if there is no thread waiting for response
                                // AND if it is a failure, pump out a download error
                                Bundle bundleMediaError = new Bundle();
                                bundleMediaError.putString(
                                        ACTION, BackgroundAudioService.Action.MEDIA_ERROR_LOADING.name()
                                );
                                bundleMediaError.putString(
                                        BackgroundAudioService.Action.MEDIA_ERROR_LOADING.name(),
                                        context.getString(R.string.error_media_not_found)
                                );
                                bundleMediaError.putSerializable(
                                        BackgroundAudioService.ActionExtras.MEDIA_ERROR_LOADING_REMOVE_ITEM.name(),
                                        itemJSON
                                );
                                mediaSessionCompat.sendSessionEvent(
                                        BackgroundAudioService.Action.MEDIA_ERROR_LOADING.name(),
                                        bundleMediaError
                                );
                            }
                        }
                    } else {
                        //success set the fully downloaded and file input stream

                        //update progress to complete
                        //doesnt matter, download is complete successfully notify frontend
                        progressCallback.updateProgress(1, 1, 0);

                        //set fully downloaded
                        audioJSON.progressTracker.setIsFullyDownloaded(true);

                        synchronized (lockDownloadTasks) {
                            downloadTasks.remove(itemJSON.href);
                        }

                        synchronized (lockThreadListener) {
                            //set the fileInputStream for thread
                            ThreadListener threadListener = threadListeners.get(itemJSON.href);
                            if (threadListener != null) {
                                //set the fileInputStream
                                threadListener.setFileInputStream(fileInputStream);
                            } else {
                                Log.d(TAG, "startDownload: no threadListener found for [" +
                                        itemJSON.href + "]");
                            }
                            if (threadListener != null && threadListener.isAlive()) {
                                synchronized (threadListener) {
                                    threadListener.notify();
                                }
                            }
                        }
                    }
                },
                progressCallback
        );
        if (downloadMediaTask != null) {
            //async task started for downloading
            synchronized (lockDownloadTasks) {
                downloadTasks.put(itemJSON.href, downloadMediaTask);
            }
        }
    }

    public boolean waitForResponse(int i,
                                APIRecommendations.ItemJSON itemJSON,
                                MediaQueueDownloadReadyCallback callback) {
        synchronized (lockThreadListener) {
            if (threadListeners.get(itemJSON.href) != null) {
                //thread listener already exist, cannot add another
                return false;
            }
        }

        ThreadListener threadListener = new ThreadListener(itemJSON, callback);
        threadListener.start();

        synchronized (lockThreadListener) {
            threadListeners.put(itemJSON.href, threadListener);
        }
        //if download hasn't begun then start one
        startDownload(i, itemJSON);
        return true;
    }

    @Override
    public void addItem(int i, APIRecommendations.ItemJSON item) {
        startDownload(i, item);
    }

    @Override
    public void removeItem(int i, APIRecommendations.ItemJSON item) {
        Log.d(TAG, "removeItem: removing item [" + item + "]");
        DownloadMediaTask downloadMediaTask;
        synchronized (lockDownloadTasks) {
            //stop the download task if downloading
            downloadMediaTask = downloadTasks.get(item.href);
            if (downloadMediaTask != null) {
                //stops the download media task
                downloadMediaTask.stopDownload();
                downloadTasks.remove(item.href);
            }
        }

        if (downloadMediaTask != null) {
            //join on the runnable before deleting
            DownloadPoolExecutor downloadPoolExecutor = DownloadPoolExecutor.getInstance(
                    DownloadPoolExecutor.Type.Audio
            );
            Thread tmpThread = downloadPoolExecutor.removeRunnable(downloadMediaTask);
            if (tmpThread != null) {
                tmpThread.interrupt();
            }
        }

        //delete the file from the file system
        boolean deleted = fileCache.deleteFileIfExists(item.links.getValidAudio().href, FileCache.Type.AUDIO);
        if (deleted) {
            Log.d(TAG, "removeItem: successfully deleted [" + item.href + "]");
        } else {
            Log.e(TAG, "removeItem: unsuccessfully deleted [" + item.href + "]");
        }
    }

    public class ThreadListener extends Thread {
        private FileInputStream fileInputStream;
        private final APIRecommendations.ItemJSON itemJSON;
        private final MediaQueueDownloadReadyCallback callback;

        public ThreadListener(APIRecommendations.ItemJSON itemJSON,
                              MediaQueueDownloadReadyCallback callback) {
            this.itemJSON = itemJSON;
            this.callback = callback;
        }

        public void run() {
            try {
                synchronized (this) {
                    this.wait();
                }
                //get the fileInputStream for thread
                //thread listener will never be different then the put
                ThreadListener threadListener = threadListeners.get(itemJSON.href);
                if (threadListener != null) {
                    //set the fileInputStream
                    callback.callback(true, threadListener.getFileInputStream());
                } else {
                    Log.e(TAG, "run: thread listener not found for [" + itemJSON + "]");
                    //file input stream not found
                    callback.callback(true, null);
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "run: failed interruputed", e);
                callback.callback(false, null);
            } finally {
                //delete the current thread listener
                synchronized (lockThreadListener) {
                    threadListeners.remove(itemJSON.href);
                }
            }
        }

        public void setFileInputStream(FileInputStream fileInputStream) {
            this.fileInputStream = fileInputStream;
        }

        public FileInputStream getFileInputStream() {
            return fileInputStream;
        }
    }
}
