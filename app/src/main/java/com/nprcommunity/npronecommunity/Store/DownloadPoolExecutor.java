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
    //used to track down the threads
    private final Map<Runnable, Thread> threadHashMap = new HashMap<>();
    private static final Object lock = new Object();
    public enum Type {
        Image,
        Audio
    }

    private DownloadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                                 TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public static DownloadPoolExecutor getInstance(@NonNull Type type) {
        switch (type) {
            case Image:
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

    public void execute(Runnable runnable) {
        super.execute(runnable);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        synchronized (lock) {
            threadHashMap.put(r, t);
        }
    }

    public Thread removeRunnable(Runnable runnable) {
        synchronized (lock) {
            return threadHashMap.remove(runnable);
        }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        removeRunnable(r);
    }
}