package com.nprcommunity.npronecommunity.Store;

import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DownloadPoolExecutor extends ThreadPoolExecutor {

    private static final BlockingQueue<Runnable> imageWorkQueue = new LinkedBlockingDeque<>(),
            audioWorkQueue = new LinkedBlockingDeque<>();
    private static final int KEEP_ALIVE_TIME = 10; // wait seconds before killing thread
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    private static int HALF_NUMBER_OF_CORES = getHalfNumberCores(),
                        QUARTER_NUMBER_OF_CORES = getQuarterNumberCores();
    private static DownloadPoolExecutor imageDownloadPoolExecutor,
                                    audioDownloadPoolExecutor;

    private final Object mapLock = new Object();

    //two way hash map used for tracking audio
    private final Map<String, Runnable> runnableHashMap = new HashMap<>();
    private final Map<Runnable, String> runnableHashMapReverse = new HashMap<>();
    private final Map<Runnable, Thread> runnableThreadHashMap = new HashMap<>();

    public enum MediaType {
        IMAGE,
        AUDIO
    }

    private DownloadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                                 TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        this.allowCoreThreadTimeOut(false);
    }

    public static DownloadPoolExecutor getInstance(@NonNull MediaType mediaType) {
        switch (mediaType) {
            case IMAGE:
                if (imageDownloadPoolExecutor == null) {
                    imageDownloadPoolExecutor = new DownloadPoolExecutor(
                            HALF_NUMBER_OF_CORES,
                            HALF_NUMBER_OF_CORES,
                            KEEP_ALIVE_TIME,
                            KEEP_ALIVE_TIME_UNIT,
                            imageWorkQueue
                    );
                }
                return imageDownloadPoolExecutor;
            default:
                if (audioDownloadPoolExecutor == null) {
                    audioDownloadPoolExecutor = new DownloadPoolExecutor(
                            QUARTER_NUMBER_OF_CORES,
                            QUARTER_NUMBER_OF_CORES,
                            KEEP_ALIVE_TIME,
                            KEEP_ALIVE_TIME_UNIT,
                            audioWorkQueue
                    );
                }
                return audioDownloadPoolExecutor;
        }
    }

    private static int getHalfNumberCores() {
        int cores = Runtime.getRuntime().availableProcessors()/2;
        if (cores <= 0) {
            cores = 1;
        }
        return cores;
    }

    private static int getQuarterNumberCores() {
        int cores = Runtime.getRuntime().availableProcessors()/4;
        if (cores <= 0) {
            cores = 1;
        }
        return cores;
    }

    public boolean inQueue(String href) {
        synchronized (mapLock) {
            return runnableHashMap.containsKey(href);
        }
    }

    /**
     * Returns true if added to queue, it already is in the queue.
     * @param runnable a runnable to execute
     * @param href the href for what is downloading
     * @param mediaType the type of media downloading
     * @return true if added, false if already exists in queue or running
     */
    public boolean execute(Runnable runnable, String href, MediaType mediaType) {
        synchronized (mapLock) {
            if (!runnableHashMap.containsKey(href)) {
                runnableHashMap.put(href, runnable);
                runnableHashMapReverse.put(runnable, href);
                execute(runnable);
                return true;
            }
            return false;
        }
    }

    @Override
    public void execute(Runnable runnable) {
        super.execute(runnable);
    }

    @Override
    protected void beforeExecute(Thread thread, Runnable runnable) {
        super.beforeExecute(thread, runnable);
        synchronized (mapLock) {
            runnableThreadHashMap.put(runnable, thread);
        }
    }

    @Override
    protected void afterExecute(Runnable runnable, Throwable throwable) {
        super.afterExecute(runnable, throwable);
        if (runnableHashMapReverse.containsKey(runnable)) {
            String key = runnableHashMapReverse.remove(runnable);
            runnableHashMap.remove(key);
            runnableThreadHashMap.remove(runnable);
        }
    }

    public boolean stopAndRemove(String key) {
        synchronized (mapLock) {
            if (runnableHashMap.containsKey(key)) {
                Runnable runnable = runnableHashMap.remove(key);
                remove(runnable);
                runnableHashMapReverse.remove(runnable);
                Thread thread = runnableThreadHashMap.remove(runnable);
                if (thread != null && thread.isAlive()) {
                    thread.interrupt();
                }
            }
            return false;
        }
    }

    public void forceShutdown() {
        synchronized (mapLock) {
            runnableHashMapReverse.clear();
            runnableHashMap.clear();
            runnableThreadHashMap.clear();
        }
        this.allowCoreThreadTimeOut(true);
        this.setKeepAliveTime(0, TimeUnit.SECONDS);
        this.shutdownNow();
    }
}
