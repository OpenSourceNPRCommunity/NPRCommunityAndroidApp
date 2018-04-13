package com.nprcommunity.npronecommunity.API;

import android.content.Context;
import android.util.Log;

import com.nprcommunity.npronecommunity.Store.RecommendationCache;
import com.nprcommunity.npronecommunity.Store.JSONCache;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.List;

public class Recommendations extends API {
    private static String TAG = "API.RECOMMENDATIONS";

    public static final String DEFAULT_RECOMMENDATIONS_URL =
            "https://listening.api.npr.org/v2/recommendations";

    public Recommendations(Context context, String url) {
        super(context);
        URL = url;
    }

    public RecommendationCache getData() {
        return (RecommendationCache)data;
    }

    @Override
    public void executeFunc(String jsonData, Boolean success) {
        if(success) {
            Moshi moshi = new Moshi.Builder().build();
            JsonAdapter<RecommendationsJSON> jsonAdapter = moshi.adapter(RecommendationsJSON.class);
            try {
                RecommendationsJSON recommendationsJSON = jsonAdapter.fromJson(jsonData);
                //Set data
                RecommendationCache baseCache = new RecommendationCache(recommendationsJSON, URL);
                JSONCache.putObject(URL, baseCache);
                data = baseCache;
            } catch (IOException e) {
                Log.e(TAG, "executeFunc: Error adapting json data to user: " + jsonData);
            }
        }
    }

    public static class RecommendationsJSON {
        public String version,
                href;
        public List<ItemJSON> items;
    }

    public static class ItemJSON {
        public String version,
                href;
        public AttributesJSON attributes;
        public LinksJSON links;
    }

    public static class AttributesJSON {
        public String type,
                uid,
                title,
                audioTitle;
        public boolean primary;
        public GeoFenceJSON geoFence;
        public OrganizationJSON organization;
        public String button;
        public boolean skippable;
        public String rationale,
                        slug,
                        provider,
                        program;
        public int duration;
        public String date,
                        expires,
                        description,
                        song,
                        artist,
                        album,
                        label;
        public RatingJSON rating;
        public LinksJSON links;
    }

    public static class GeoFenceJSON {
        public boolean restricted;

        public List<String> countries;
    }

    public static class OrganizationJSON {
        public String name,
                        logoUrl,
                        homepageUrl,
                        donateUrl;
    }

    public static class RatingJSON {
        public String mediaId,
                        origin,
                        rating;
        public int elapsed,
                    duration;
        public String timestamp,
                        channel,
                        cohort;
    }

    public static class LinksJSON {
        public List<AudioJSON> audio;
        public List<ImageJSON> image;
    }

    public static class AudioJSON {
        public String href;
        public @Json(name="content-type") String content_type;
    }

    public static class ImageJSON {
        public String href;
        public @Json(name="content-type") String content_type;
        public String rel;
    }
}