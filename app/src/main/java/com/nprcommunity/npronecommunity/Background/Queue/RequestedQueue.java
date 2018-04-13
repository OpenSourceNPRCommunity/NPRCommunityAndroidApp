package com.nprcommunity.npronecommunity.Background.Queue;

import android.util.Log;

import com.squareup.tape2.QueueFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RequestedQueue implements AudioQueue {

    private static String TAG = "BACKGROUND.QUEUE.REQUESTEDQUEUE";
    private static RequestedQueue requestedQueue;
    private static QueueFile queueFile;
    private boolean canUseQueueFile = false;
    private final Object lock = new Object();

    private RequestedQueue(String queueFilePath) {
        initQueue(new File(queueFilePath));
    }

    /**
     * Creates a queue file and inits teh queueFile processor
     * @param file
     */
    private void initQueue(File file) {
        try {
            queueFile = new QueueFile.Builder(file).build();
            canUseQueueFile = true;
        } catch (IOException e) {
            Log.e(TAG, "initQueue: ", e);
            canUseQueueFile = false;
        }
    }

    public static RequestedQueue getInstance(String queueFilePath) {
        if (requestedQueue == null) {
            requestedQueue = new RequestedQueue(queueFilePath);
        }
        return requestedQueue;
    }

    @Override
    public boolean removeURL(String url) {
        synchronized (lock) {
            if (canUseQueueFile) {
                Iterator<byte[]> iterator = queueFile.iterator();
                while (iterator.hasNext()) {
                    String element = new String(iterator.next());
                    if (element.equals(url)) {
                        iterator.remove();
                        return true;
                    }
                }
            }
            return false;
        }
    }

    @Override
    public String peek() {
        synchronized (lock) {
            String url = null;
            if (canUseQueueFile) {
                try {
                    byte[] tmpUrl = queueFile.peek();
                    if (tmpUrl != null) {
                        url = new String(tmpUrl);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "peek: ", e);
                }
            }
            return url;
        }
    }

    @Override
    public boolean addURL(String url) {
        synchronized (lock) {
            if (canUseQueueFile) {
                try {
                    queueFile.add(url.getBytes());
                } catch (IOException e) {
                    Log.e(TAG, "addAudio: ", e);
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public List<String> getQueue() {
        synchronized (lock) {
            List<String> urlList = new ArrayList<>();
            if (canUseQueueFile) {
                Iterator<byte[]> iterator = queueFile.iterator();
                while (iterator.hasNext()) {
                    urlList.add(new String(iterator.next()));
                }
            }
            return urlList;
        }
    }
}
