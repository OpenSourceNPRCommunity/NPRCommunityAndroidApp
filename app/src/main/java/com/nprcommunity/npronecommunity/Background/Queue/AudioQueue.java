package com.nprcommunity.npronecommunity.Background.Queue;

import java.util.List;

public interface AudioQueue {
    /**
     * Return true if url was found and removed. Else false.
     * @param url String url
     * @return whether removed or not
     */
    public boolean removeURL(String url);

    /**
     * Returns the head of the queue
     */
    public String peek();

    /**
     * Add a url
     * @return adding the url was successful
     */
    public boolean addURL(String url);

    /**
     * Gets the current queue list
     * @return list of urls for the current queue
     */
    public List<String> getQueue();
}
