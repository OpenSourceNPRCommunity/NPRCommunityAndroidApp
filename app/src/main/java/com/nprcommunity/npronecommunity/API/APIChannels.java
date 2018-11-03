package com.nprcommunity.npronecommunity.API;

import android.content.Context;
import android.util.Log;

import com.nprcommunity.npronecommunity.Store.CacheStructures.ChannelCache;
import com.nprcommunity.npronecommunity.Store.JSONCache;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class APIChannels extends API {
    private static String TAG = "API.CHANNELS";
    public static final String DEFAULT_CHANNELS_URL = "https://listening.api.npr.org/v2/channels";

    public static final Map<String, Boolean> DEFAULT_CHANNELS_MAP = getDefaultChannels();

    private static Map<String, Boolean> getDefaultChannels() {
        //Values do not matter, will only be checking if keys exist
        Map<String, Boolean> result = new HashMap<String, Boolean>();
        result.put("followed", Boolean.TRUE);
        result.put("history", Boolean.TRUE);
        result.put("newscasts", Boolean.TRUE);
        result.put("recommended", Boolean.TRUE);
        result.put("shows", Boolean.TRUE);
        return Collections.unmodifiableMap(result);
    }

    public APIChannels(Context context) {
        super(context);
        URL = DEFAULT_CHANNELS_URL;
    }

    public APIChannels(Context context, String urlParent) {
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
                Log.e(TAG, "callback: Error adapting json data to user: " + jsonData);
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

        //custom attribute used later on for is selected
        public boolean isChecked;
    }

    public static class AttributesJSON {
        public String id,
                fullName,
                description,
                displayType;
        public int refreshRule;
    }
}
