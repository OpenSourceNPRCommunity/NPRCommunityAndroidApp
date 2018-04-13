package com.nprcommunity.npronecommunity.Background.Queue;

import java.util.List;

public class LineUpQueue implements AudioQueue {

    private static String TAG = "BACKGROUND.QUEUE.LINEUPQUEUE";
    private static LineUpQueue lineUpQueue;
    private boolean canUseQueueFile = false;

    private LineUpQueue() {
        loadLineUpQueue();
    }

    private void loadLineUpQueue() {

    }

    @Override
    public boolean removeURL(String url) {
        return false;
    }

    @Override
    public String peek() {
        return null;
    }

    @Override
    public boolean addURL(String url) {
        return false;
    }

    @Override
    public List<String> getQueue() {
        return null;
    }
}
