package com.nprcommunity.npronecommunity.Background.Queue;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;

import com.nprcommunity.npronecommunity.API.APIRecommendations;
import com.nprcommunity.npronecommunity.Store.CacheStructures.QueueItemCache;
import com.nprcommunity.npronecommunity.Store.JSONCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class LineUpQueue {

    private static String TAG = "BACKGROUND.QUEUE.LINEUPQUEUE";
    private static String JSONCACHE_KEY = "community_lineup_queue";
    private static LineUpQueue lineUpQueue;

    public enum ApiItem {
        API_ITEM
    }

    private ArrayList<MediaSessionCompat.QueueItem> queueItemList = new ArrayList<>();

    protected LineUpQueue() {
        QueueItemCache queueCache = (QueueItemCache) JSONCache.getObject(JSONCACHE_KEY);
        if (queueCache != null && queueCache.data != null) {
            List<APIRecommendations.ItemJSON> data = (List)queueCache.data;
            for (APIRecommendations.ItemJSON dataItem : data) {
                queueItemList.add(translateAPIQueueItem(dataItem));
            }
        }
    }

    public static MediaSessionCompat.QueueItem translateAPIQueueItem(APIRecommendations.ItemJSON apiItemJSON) {
        Bundle apiItemBundle = new Bundle();
        apiItemBundle.putSerializable(ApiItem.API_ITEM.name(), apiItemJSON);

        Uri audioURI = new Uri.Builder().path(apiItemJSON.links.getValidAudio().href).build();
        Uri imageURI = new Uri.Builder().path(apiItemJSON.links.getValidImage().href).build();
        MediaDescriptionCompat mediaDescription = new MediaDescriptionCompat.Builder()
                .setDescription(apiItemJSON.attributes.description)
                .setMediaId(apiItemJSON.href)
                .setMediaUri(audioURI)
                .setTitle(apiItemJSON.attributes.title)
                .setIconUri(imageURI)
                .setSubtitle(apiItemJSON.attributes.program)
                .setExtras(apiItemBundle)
                .build();
        Random random = new Random(System.currentTimeMillis());
        return new MediaSessionCompat.QueueItem(mediaDescription, random.nextLong());
    }

    public static LineUpQueue getInstance() {
        if (lineUpQueue == null) {
            lineUpQueue = new LineUpQueue();
        }
        return lineUpQueue;
    }

    public void removeQueueItem(String itemURL) {
        MediaSessionCompat.QueueItem tmpItemJSON;
        Iterator<MediaSessionCompat.QueueItem> itemIterator = queueItemList.iterator();
        while (itemIterator.hasNext()) {
            tmpItemJSON = itemIterator.next();
            // goes through whole list to check for duplicates
            if (tmpItemJSON.getDescription().getMediaId().equals(itemURL)) {
                itemIterator.remove();
            }
        }
        putQueueItemList();
    }

    public void removeQueueItem(int position) {
        queueItemList.remove(position);
        putQueueItemList();
    }

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

    public MediaSessionCompat.QueueItem getQueueItem(int position) {
        return queueItemList.get(position);
    }

    public MediaSessionCompat.QueueItem peek() {
        if (queueItemList.size() > 0) {
            return queueItemList.get(0);
        }
        return null;
    }

    public boolean addQueueItem(APIRecommendations.ItemJSON queueItem) {
        boolean retVal = queueItemList.add(translateAPIQueueItem(queueItem));
        putQueueItemList();
        return retVal;
    }

    public ArrayList<MediaSessionCompat.QueueItem> getQueue() {
        return (ArrayList<MediaSessionCompat.QueueItem>) queueItemList.clone();
    }

    public int size() {
        return queueItemList.size();
    }

    public boolean addQueueItem(int index, APIRecommendations.ItemJSON queueItem) {
        if (index >= queueItemList.size()) {
            index = queueItemList.size() - 1;
        }
        if (index < 0) {
            index = 0;
        }
        queueItemList.add(index, translateAPIQueueItem(queueItem));
        putQueueItemList();
        return true;
    }

    private void putQueueItemList() {
        ArrayList<APIRecommendations.ItemJSON> tmpQueueItemList =
                new ArrayList<>(queueItemList.size());
        for (int i = 0; i < queueItemList.size(); i++) {
            tmpQueueItemList.add((APIRecommendations.ItemJSON) queueItemList.get(i)
                    .getDescription().getExtras().getSerializable(ApiItem.API_ITEM.name()));
        }
        JSONCache.putObject(
                JSONCACHE_KEY,
                new QueueItemCache(tmpQueueItemList, JSONCACHE_KEY)
        );
    }

    public void forceSaveQueue() {
        putQueueItemList();
    }
}
