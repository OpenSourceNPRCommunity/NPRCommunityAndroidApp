package com.nprcommunity.npronecommunity.API;

import android.content.Context;
import android.util.Log;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import okio.Okio;
import okio.Source;

public class APIStation extends API {
    private static String TAG = "API.STATION";
    public static final String DEFAULT_STATION_URL = "https://station.api.npr.org/v3/stations";

    public APIStation(Context context) {
        super(context);
        URL = DEFAULT_STATION_URL;
    }

    public APIStation(Context context, String searchParam) {
        super(context);
        URL = DEFAULT_STATION_URL + "?q=" + searchParam;
    }

    public StationJSON getData() {
        return (StationJSON) data;
    }

    @Override
    public void executeFunc(String jsonData, Boolean success) {
        if(success) {
            Moshi moshi = new Moshi.Builder().build();
            JsonAdapter<StationJSON> jsonAdapter = moshi.adapter(StationJSON.class);
            try {
                StationJSON stationJSON = jsonAdapter.fromJson(jsonData);
                //Set data
                data = stationJSON;
            } catch (IOException e) {
                Log.e(TAG, "executeFunc: Error adapting json data to user", e);
            }
        }
    }

    public static class StationJSON {
        public String version,
                href;
        public List<ItemJSON> items;
    }

    public static class ItemJSON {
        public String version,
                href;
        public AttributeJSON attributes;
        public LinkJSON links;
    }

    public static class AttributeJSON {
        public String orgId,
                guid;
        public BrandJSON brand;
    }

    public static class BrandJSON {
        public String name,
                call,
                frequency,
                band,
                tagline,
                marketCity,
                marketState;
    }

    public static class LinkJSON {
        public List<BrandLinkJSON> brand;
        public List<DonationLinkJSON> donation;

        public boolean hasImage() {
            return brand != null && brand.size() > 0;
        }

        public BrandLinkJSON getValidBrand() {
            //search for logo first, if no logo then do the next
            for (BrandLinkJSON brandLinkJSON : brand) {
                if (brandLinkJSON.rel.equals("logo")) {
                    return brandLinkJSON;
                }
            }
            return brand.get(0);
        }
    }

    public static class BrandLinkJSON {
        public String href;
        public @Json(name="content-type") String content_type;
        public String rel;
    }

    public static class DonationLinkJSON {
        public String href,
                guid,
                typeName,
                title,
                typeId;
    }
}