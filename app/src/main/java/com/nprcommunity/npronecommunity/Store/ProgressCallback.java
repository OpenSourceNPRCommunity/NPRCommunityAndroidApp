package com.nprcommunity.npronecommunity.Store;

public interface ProgressCallback {

    enum Type {
        DOWNLOADING,
        WAITING_FOR_INTERNET,
        ERROR,
        COMPLETE,
    }

    /**
     * Trigger whenever MILLI_NOTIFY is reached.
     * @param progress
     * @param total
     */
    void updateProgress(int progress, int total, int speed, Type type);
}
