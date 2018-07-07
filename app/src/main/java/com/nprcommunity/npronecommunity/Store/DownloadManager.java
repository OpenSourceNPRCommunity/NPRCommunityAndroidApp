package com.nprcommunity.npronecommunity.Store;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DownloadManager extends ThreadPoolExecutor {

    private static final BlockingQueue<Runnable> workQueue = new LinkedBlockingDeque<>();
    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    private static int HALF_NUMBER_OF_CORES = getNumberCores();
    private static DownloadManager downloadManager;
    //used to track down the threads
    private final Map<Runnable, Thread> threadHashMap = new HashMap<>();
    private static final Object lock = new Object();

    public DownloadManager(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                           TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public static DownloadManager getInstance() {
        if (downloadManager == null) {
            downloadManager = new DownloadManager(
                    HALF_NUMBER_OF_CORES,
                    HALF_NUMBER_OF_CORES,
                    KEEP_ALIVE_TIME,
                    KEEP_ALIVE_TIME_UNIT,
                    workQueue
            );
        }
        return downloadManager;
    }

    private static int getNumberCores() {
        int cores = Runtime.getRuntime().availableProcessors()/2;
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
