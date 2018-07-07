package com.nprcommunity.npronecommunity.Background.Queue;

import android.content.Context;
import android.util.Log;

import com.nprcommunity.npronecommunity.API.APIDataResponse;
import com.nprcommunity.npronecommunity.API.APIRecommendations;
import com.nprcommunity.npronecommunity.Store.CacheStructures.QueueItemCache;
import com.nprcommunity.npronecommunity.Store.CacheStructures.RecommendationCache;
import com.nprcommunity.npronecommunity.Store.JSONCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class LineUpQueue implements AudioQueue {

    private static String TAG = "BACKGROUND.QUEUE.LINEUPQUEUE";
    private static String JSONCACHE_KEY = "community_lineup_queue";
    private static LineUpQueue lineUpQueue;

    private ArrayList<APIRecommendations.ItemJSON> queueItemList = new ArrayList<>();

    protected LineUpQueue() {
        QueueItemCache queueCache = (QueueItemCache) JSONCache.getObject(JSONCACHE_KEY);
        if (queueCache != null && queueCache.data != null) {
            List<APIRecommendations.ItemJSON> data = (List)queueCache.data;
            queueItemList.addAll(data);
        }
    }

    public static LineUpQueue getInstance() {
        if (lineUpQueue == null) {
            lineUpQueue = new LineUpQueue();
        }
        return lineUpQueue;
    }

    @Override
    public void removeQueueItem(String itemURL) {
        APIRecommendations.ItemJSON tmpItemJSON;
        Iterator<APIRecommendations.ItemJSON> itemIterator = queueItemList.iterator();
        while (itemIterator.hasNext()) {
            tmpItemJSON = itemIterator.next();
            // goes through whole list to check for duplicates
            if (tmpItemJSON.href.equals(itemURL)) {
                itemIterator.remove();
            }
        }
        putQueueItemList();
    }

    @Override
    public void removeQueueItem(int position) {
        queueItemList.remove(position);
        putQueueItemList();
    }

    @Override
    public void swapQueueItem(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(queueItemList, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(queueItemList, i, i - 1);
            }
        }
        putQueueItemList();
    }

    @Override
    public APIRecommendations.ItemJSON getQueueItem(int position) {
        return queueItemList.get(position);
    }

    @Override
    public APIRecommendations.ItemJSON peek() {
        if (queueItemList.size() > 0) {
            return queueItemList.get(0);
        }
        return null;
    }

    @Override
    public boolean addQueueItem(APIRecommendations.ItemJSON queueItem) {
        boolean retVal = queueItemList.add(queueItem);
        putQueueItemList();
        return retVal;
    }

    @Override
    public ArrayList<APIRecommendations.ItemJSON> getQueue() {
        //TODO: Fix these unchecked exceptions
        return (ArrayList<APIRecommendations.ItemJSON>) queueItemList.clone();
    }

    @Override
    public int size() {
        return queueItemList.size();
    }

    @Override
    public boolean addQueueItem(int index, APIRecommendations.ItemJSON queueItem) {
        if (index >= queueItemList.size()) {
            index = queueItemList.size() - 1;
        }
        if (index < 0) {
            index = 0;
        }
        queueItemList.add(index, queueItem);
        putQueueItemList();
        return true;
    }

    private void putQueueItemList() {
        List<APIRecommendations.ItemJSON> tmpQueueItemList = getQueue();
        JSONCache.putObject(
                JSONCACHE_KEY,
                new QueueItemCache(tmpQueueItemList, JSONCACHE_KEY)
        );
    }

    public void forceSaveQueue() {
        putQueueItemList();
    }
}
