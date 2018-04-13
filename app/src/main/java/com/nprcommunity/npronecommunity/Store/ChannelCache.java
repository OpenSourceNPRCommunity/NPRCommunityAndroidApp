package com.nprcommunity.npronecommunity.Store;

import com.nprcommunity.npronecommunity.API.Channels;

import java.util.Date;

public class ChannelCache extends BaseCache<Channels.ChannelsJSON>{

    public ChannelCache(Channels.ChannelsJSON data, String urlParent) {
        super(data, urlParent);
    }
}
