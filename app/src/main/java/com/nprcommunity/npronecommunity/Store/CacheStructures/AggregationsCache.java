package com.nprcommunity.npronecommunity.Store.CacheStructures;

import com.nprcommunity.npronecommunity.API.APIAggregations;
import com.nprcommunity.npronecommunity.Config;
import com.nprcommunity.npronecommunity.Util;

import java.util.Date;

public class AggregationsCache extends BaseCache<APIAggregations.AggregationJSON> {

    public AggregationsCache(APIAggregations.AggregationJSON data, String urlParent) {
        super(data, urlParent);
    }

    @Override
    public boolean isExpired() {
        long updateMilli = lastUpdatedDate.getTime();

        if (!Config.ENABLE_CACHE_TIMEOUT) {
            return false;
        }

        // if the past milli is greater then 1 hour, need to refresh recommendation
        return (updateMilli + (Util.MILLI_HOUR) < (new Date()).getTime());
    }
}
