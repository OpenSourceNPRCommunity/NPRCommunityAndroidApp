package com.nprcommunity.npronecommunity.API;

import android.content.Context;
import android.util.Log;

import com.nprcommunity.npronecommunity.Store.CacheStructures.AggregationsCache;
import com.nprcommunity.npronecommunity.Store.JSONCache;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

import okio.BufferedSource;
import okio.Okio;
import okio.Source;

public class APIAggregations extends API {
    private static String TAG = "API.AGGREGATIONS";

    public static final String BASE_URL = "https://listening.api.npr.org/v2/";

    public APIAggregations(Context context, int aggId) {
        super(context);
        URL = BASE_URL + "aggregation/" + aggId + "/recommendations";
    }

    public AggregationsCache getData() {
        return (AggregationsCache)data;
    }

    @Override
    public void executeFunc(String jsonData, Boolean success) {
        super.executeFunc(jsonData, success);
        if(success) {
            Moshi moshi = new Moshi.Builder().add(new APIRecommendations.AtomicIntegerAdapter()).build();
            JsonAdapter<AggregationJSON> jsonAdapter = moshi.adapter(AggregationJSON.class);
            try {
                AggregationJSON aggregationsJSON = jsonAdapter.fromJson(jsonData);
                if (aggregationsJSON != null && aggregationsJSON.isValidAggregation()) {
                    cleanAggregation(aggregationsJSON);
                } else {
                    Log.e(TAG, "executeFun: Error invalid aggregations");
                }
                //Set data
                AggregationsCache aggregationsCache = new AggregationsCache(aggregationsJSON, URL);
                JSONCache.putObject(URL, aggregationsCache);
                data = aggregationsCache;
            } catch (IOException e) {
                Log.e(TAG, "callback: Error adapting json data to user", e);
            }
        }
    }

    protected static void cleanAggregation(AggregationJSON aggregationJSON) {
        APIRecommendations.cleanRecommendationItems(aggregationJSON.items);
    }

    public static class AggregationJSON implements Serializable {
        private static final String VERSION_1 = "1.0";
        public String version,
                href;
        public AggregationAttributesJSON attributes;
        public List<APIRecommendations.ItemJSON> items;
        public LinksJSON links;

        public boolean isValidAggregation() {
            return version != null &&
                    version.equals(VERSION_1) &&
                    href != null &&
                    !href.equals("") &&
                    attributes != null;
        }

        public boolean hasLinks() {
            return links != null;
        }
    }

    public static class AggregationAttributesJSON implements Serializable {
        private static final String DEFAULT_DESCRIPTION = "Unknown Description";
        private static final String DEFAULT_TITLE = "Unknown Title";

        String type,
                affiliation;
        AffiliationsMeta affiliationsMeta;
        String title,
                description,
                station;

        public String getTitle() {
            if (title == null || title.equals("")) {
                return DEFAULT_TITLE;
            }
            return title;
        }

        public String getDescription() {
            if (description == null || description.equals("")) {
                return DEFAULT_DESCRIPTION;
            }
            return description;
        }
    }

    public static class AffiliationsMeta {
        boolean id;
        public String title;
        float rating;
        String href;
        int daysSinceLastListen;
        List<String> notif_following;
        List<String> notif_rated;
        boolean following;
    }

    public static class LinksJSON implements Serializable{
        public List<Shared.WebJSON> web;
        public List<MoreJSON> more;
        public List<Shared.ImageJSON> image;

        public boolean hasWeb() {
            return web != null && web.size() > 0;
        }

        public boolean hasImage() {
            return image != null && image.size() > 0;
        }

        public Shared.ImageJSON getValidImage() {
            return image.get(0);
        }
    }

    public static class MoreJSON implements Serializable {
        public String href;
        public @Json(name="content-type") String content_type;
    }
}