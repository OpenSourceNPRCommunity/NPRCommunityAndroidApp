package com.nprcommunity.npronecommunity.Store.CacheStructures;

import com.nprcommunity.npronecommunity.API.APIRecommendations;

import java.util.List;

public class QueueItemCache extends BaseCache<List<APIRecommendations.ItemJSON>> {

    public QueueItemCache(List<APIRecommendations.ItemJSON> data, String urlParent) {
        super(data, urlParent);
    }
}

