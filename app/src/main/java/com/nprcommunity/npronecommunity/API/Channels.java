package com.nprcommunity.npronecommunity.API;

import android.content.Context;
import android.util.Log;

import com.nprcommunity.npronecommunity.Store.BaseCache;
import com.nprcommunity.npronecommunity.Store.ChannelCache;
import com.nprcommunity.npronecommunity.Store.RecommendationCache;
import com.nprcommunity.npronecommunity.Store.JSONCache;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.List;

public class Channels extends API {
    private static String TAG = "API.CHANNELS";

    public Channels(Context context) {
        super(context);
        URL = "https://listening.api.npr.org/v2/channels";
    }

    public Channels(Context context, String urlParent) {
        super(context);
        URL = urlParent;
    }

    public ChannelCache getData() {
        return (ChannelCache) data;
    }

    @Override
    public void executeFunc(String jsonData, Boolean success) {
        if(success) {
            Moshi moshi = new Moshi.Builder().build();
            JsonAdapter<ChannelsJSON> jsonAdapter = moshi.adapter(ChannelsJSON.class);
            try {
                ChannelsJSON channelsJSON = jsonAdapter.fromJson(jsonData);
                //Set data
                ChannelCache baseCache = new ChannelCache(channelsJSON, URL);
                JSONCache.putObject(URL, baseCache);
                data = baseCache;
            } catch (IOException e) {
                Log.e(TAG, "executeFunc: Error adapting json data to user: " + jsonData);
            }
        }
    }

    public static class ChannelsJSON {
        public String version,
                href;
        public List<ItemJSON> items;
    }

    public static class ItemJSON {
        public String version,
                href;
        public AttributesJSON attributes;
    }

    public static class AttributesJSON {
        public String id,
                fullName,
                description,
                displayType;
        public int refreshRule;
    }
}
