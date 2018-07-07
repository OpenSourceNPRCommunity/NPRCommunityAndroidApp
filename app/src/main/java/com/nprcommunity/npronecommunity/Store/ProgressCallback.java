package com.nprcommunity.npronecommunity.Store;

public interface ProgressCallback {
    /**
     * Trigger whenever MILLI_NOTIFY is reached.
     * @param progress
     * @param total
     */
    public void updateProgress(int progress, int total, int speed);
}
