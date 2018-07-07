package com.nprcommunity.npronecommunity.Background.Queue;

import com.nprcommunity.npronecommunity.API.APIRecommendations;

import java.util.ArrayList;

public interface AudioQueue {
    /**
     * Return number of values removed
     * @param itemURL url of the item
     * @return number of values removed
     */
    public void removeQueueItem(String itemURL);

    /**
     * Returns the head of the queue
     */
    public APIRecommendations.ItemJSON peek();

    /**
     * Add a queue item
     * @return adding the url was successful
     */
    public boolean addQueueItem(APIRecommendations.ItemJSON queueItem);

    /**
     * Gets a copy of the current queue list
     * @return list of urls for the current queue
     */
    public ArrayList<APIRecommendations.ItemJSON> getQueue();

    public int size();

    /**
     * Will not throw index out of bound exception, instead
     * insert at end if > array.length
     * insert at begining if < 0
     * @param index
     * @param queueItem
     * @return
     */
    public boolean addQueueItem(int index, APIRecommendations.ItemJSON queueItem);

    public void removeQueueItem(int position);

    public void swapQueueItem(int fromPosition, int toPosition);

    public APIRecommendations.ItemJSON getQueueItem(int position);

    public void forceSaveQueue();
}
