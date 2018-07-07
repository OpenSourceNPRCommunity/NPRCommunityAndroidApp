package com.nprcommunity.npronecommunity.Store.CacheStructures;

import com.nprcommunity.npronecommunity.API.APIRecommendations;
import com.nprcommunity.npronecommunity.Config;
import com.nprcommunity.npronecommunity.Util;

import java.util.Date;

public class RecommendationCache extends BaseCache<APIRecommendations.RecommendationsJSON> {

    public RecommendationCache(APIRecommendations.RecommendationsJSON data, String urlParent) {
        super(data, urlParent);
    }

    @Override
    public boolean isExpired() {
        long updateMilli = lastUpdatedDate.getTime();

        if (!Config.ENABLE_CACHE_TIMEOUT) {
            return false;
        }

        // if the past milli is greater then 30 minutes, need to refresh recommendation
        return (updateMilli + (Util.MILLI_MINUTE*30) < (new Date()).getTime());
    }
}