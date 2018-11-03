package com.nprcommunity.npronecommunity.Background;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.media.session.MediaSessionCompat;

import com.nprcommunity.npronecommunity.API.APIRecommendations;
import com.nprcommunity.npronecommunity.Background.Queue.LineUpQueue;

import java.util.List;

public class MediaQueueManager {
    private static final String TAG = "MEDIAQUEUEMANAGER";
    private LineUpQueue lineUpQueue;
    private static MediaQueueManager mediaQueueManager;
    private static final Object lock = new Object();

    private MediaQueueManager(Context context) {
        lineUpQueue = LineUpQueue.getInstance();
    }

    public static MediaQueueManager getInstance(Context context) {
        synchronized (lock) {
            if (mediaQueueManager == null) {
                mediaQueueManager = new MediaQueueManager(context);
            }
            return mediaQueueManager;
        }
    }

    public List<MediaSessionCompat.QueueItem> getMediaQueue() {
        synchronized (lock) {
            return lineUpQueue.getQueue();
        }
    }

    public int getMediaIndex(@NonNull APIRecommendations.ItemJSON queueItem) {
        List<MediaSessionCompat.QueueItem> itemJSONS = lineUpQueue.getQueue();
        for (int i = 0; i < itemJSONS.size(); i++) {
            if (itemJSONS.get(i).getDescription().getMediaId().equals(queueItem.href)) {
                return i;
            }
        }
        return -1;
    }

    protected boolean playMediaNow(APIRecommendations.ItemJSON queueItem) {
        synchronized (lock) {
            return lineUpQueue.addQueueItem(0, queueItem);
        }
    }

    /**
     * Gets the next Queue Item if exists or {@code null] if empty does not modify
     * @return
     */
    public APIRecommendations.ItemJSON peekNextTrack() {
        synchronized (lock) {
            MediaSessionCompat.QueueItem queueItem = lineUpQueue.peek();
            if (queueItem != null) {
                return (APIRecommendations.ItemJSON) queueItem.getDescription().getExtras()
                        .getSerializable(LineUpQueue.ApiItem.API_ITEM.name());
            }
            return null;
        }
    }
    public int queueSize() {
        synchronized (lock) {
            return lineUpQueue.size();
        }
    }

    protected void remove(APIRecommendations.ItemJSON queueItem) {
        synchronized (lock) {
            lineUpQueue.removeQueueItem(queueItem.href);
        }
    }

    protected void swap(int fromPosition, int toPosition) {
        synchronized (lock) {
            lineUpQueue.swapQueueItem(fromPosition, toPosition);
        }
    }

    protected void remove(int position) {
        synchronized (lock) {
            lineUpQueue.removeQueueItem(position);
        }
    }

    protected boolean addToQueue(APIRecommendations.ItemJSON queueItem) {
        synchronized (lock) {
            if (!queueItem.links.hasValidAudio()) {
                return false;
            }
            for (int i = 0; i < lineUpQueue.size(); i++) {
                if (lineUpQueue.getQueueItem(i).getDescription().getMediaId().equals(queueItem.href)) {
                    return false;
                }
            }
            return lineUpQueue.addQueueItem(queueItem);
        }
    }

    public MediaSessionCompat.QueueItem getQueueTrack(int position) {
        synchronized (lock) {
            return lineUpQueue.getQueueItem(position);
        }
    }

    public APIRecommendations.ItemJSON getAPIQueueTrack(int position) {
        synchronized (lock) {
            return (APIRecommendations.ItemJSON) lineUpQueue.getQueueItem(position).getDescription().
                    getExtras().getSerializable(LineUpQueue.ApiItem.API_ITEM.name());
        }
    }

    public void forceSaveQueue() {
        synchronized (lock) {
            lineUpQueue.forceSaveQueue();
        }
    }
}
