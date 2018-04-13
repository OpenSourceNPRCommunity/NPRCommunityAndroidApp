package com.nprcommunity.npronecommunity.Store;

import com.nprcommunity.npronecommunity.API.Recommendations;

public class RecommendationCache extends BaseCache<Recommendations.RecommendationsJSON> {

    public RecommendationCache(Recommendations.RecommendationsJSON data, String urlParent) {
        super(data, urlParent);
    }
}