package com.nprcommunity.npronecommunity.Store.CacheStructures;

import com.nprcommunity.npronecommunity.API.APIChannels;

public class ChannelCache extends BaseCache<APIChannels.ChannelsJSON> {

    public ChannelCache(APIChannels.ChannelsJSON data, String urlParent) {
        super(data, urlParent);
    }
}
