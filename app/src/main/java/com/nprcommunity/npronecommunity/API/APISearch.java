package com.nprcommunity.npronecommunity.API;

import android.content.Context;
import android.util.Log;

import com.nprcommunity.npronecommunity.Search;
import com.squareup.moshi.FromJson;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonEncodingException;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.ToJson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static com.nprcommunity.npronecommunity.API.APISearch.ItemJSON.*;
import static com.nprcommunity.npronecommunity.API.APISearch.ItemJSON.Type.aggregation;
import static com.nprcommunity.npronecommunity.API.APISearch.ItemJSON.Type.audio;

public class APISearch extends API {
    private static String TAG = "API.SEARCH";
    public static final String DEFAULT_CHANNELS_URL = "https://listening.api.npr.org/v2/search/recommendations";

    public APISearch(Context context) {
        super(context);
        URL = DEFAULT_CHANNELS_URL;
    }

    public APISearch(Context context, String searchParam) {
        super(context);
        URL = DEFAULT_CHANNELS_URL + "?searchTerms=" + searchParam;
    }

    //in reality search cache will always be quierred
    public SearchJSON getData() {
        return (SearchJSON) data;
    }

    @Override
    public void executeFunc(String jsonData, Boolean success) {
        if(success) {
            Moshi moshi = new Moshi.Builder().build();
            //prepare stringssearchJSON
            try {
                //TODO This is extremely messy, we will have to clean that
                JSONObject itemsJsonObject = new JSONObject(jsonData);
                JSONArray items = (JSONArray) itemsJsonObject.get("items");
                if (items != null) {
                    SearchJSON searchJSON = new SearchJSON();
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject jsonObject = items.getJSONObject(i);
                        try {
                            JSONObject tmpAttr = jsonObject.getJSONObject("attributes");
                            if (tmpAttr != null) {
                                String tmpType = tmpAttr.getString("type");
                                if (tmpType != null) {
                                    if (tmpType.equals(audio.name())) {
                                        JsonAdapter<APIRecommendations.ItemJSON> jsonAdapter
                                                = moshi.adapter(APIRecommendations.ItemJSON.class);
                                        try {
                                            APIRecommendations.ItemJSON itemJSON
                                                    = jsonAdapter.fromJson(jsonObject.toString());
                                            if (itemJSON != null && itemJSON.isValidItem()) {
                                                itemJSON.attributes.clean();
                                                //Add ItemJSON
                                                searchJSON.items.add(new ItemJSON(audio, itemJSON));
                                            } else {
                                                Log.e(TAG, "callback: itemJSON is null or isnt valid[" +
                                                        itemJSON + "]");
                                            }
                                        } catch (IOException e) {
                                            Log.e(TAG, "callback: Error adapting json data: " + jsonData);
                                        } catch (JsonDataException e) {
                                            Log.e(TAG, "callback: error: ", e);
                                        }
                                    } else if (tmpType.equals(aggregation.name())) {
                                        JsonAdapter<APIAggregations.AggregationJSON> jsonAdapter
                                                = moshi.adapter(APIAggregations.AggregationJSON.class);
                                        try {
                                            APIAggregations.AggregationJSON aggregationJSON
                                                    = jsonAdapter.fromJson(jsonObject.toString());
                                            if (aggregationJSON != null && aggregationJSON.isValidAggregation()) {
                                                APIAggregations.cleanAggregation(aggregationJSON);
                                                //Add Aggregation
                                                searchJSON.items.add(new ItemJSON(aggregation, aggregationJSON));
                                            } else {
                                                Log.e(TAG, "callback: aggregationJSON is null or isnt valid[" +
                                                        aggregationJSON + "]");
                                            }
                                        } catch (IOException e) {
                                            Log.e(TAG, "callback: Error adapting json data: " + jsonData);
                                        } catch (JsonDataException e) {
                                            Log.e(TAG, "callback: error: ", e);
                                        }
                                    } else {
                                        Log.e(TAG, "callback: unknown type [" + tmpType + "]");
                                    }
                                } else {
                                    Log.e(TAG, "callback: could not setTmpType");
                                }
                            } else {
                                Log.e(TAG, "callback: could not setTmpArr");
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "callback: could not parse", e);
                        }
                    }
                    data = searchJSON;
                }
            } catch (JSONException e) {
                Log.e(TAG, "callback: could not convert from string", e);
            }
        }
    }

    public static class SearchJSON {
        public String version,
                href,
                query;
        public List<ItemJSON> items;

        public SearchJSON() {
            items = new ArrayList<>();
        }

        /*
        public boolean isValidSearch() {
            if (items == null) {
                return false;
            }
            Iterator<ItemJSON> iterator = items.iterator();
            while(iterator.hasNext()) {
                ItemJSON tmpItem = iterator.next();
                Type tmpType = tmpItem.setType;
                //TODO errors at each level.. maybe log them...
                if (tmpType != null) {
                    switch (tmpType) {
                        case audio:
                            if (tmpItem.ifTypeAudio.isValidItem()) {
                                tmpItem.setType = audio;
                                tmpItem.ifTypeAudio.attributes.clean();
                            }
                            break;
                        case aggregation:
                            if (tmpItem.ifTypeAggregation.isValidAggregation()) {
                                tmpItem.setType = Type.aggregation;
                                APIRecommendations.cleanRecommendationItems(
                                        tmpItem.ifTypeAggregation.items
                                );
                            }
                            break;
                    }
                }

                if (tmpItem.setType == null) {
                    //type not found or not valid, removing
                    iterator.remove();
                }
            }
            return true;
        }
        */
    }

    public static class ItemJSON {
        public enum Type {
            audio,
            aggregation
        }
        public Type type;
        public APIRecommendations.ItemJSON ifTypeAudio;
        public APIAggregations.AggregationJSON ifTypeAggregation;

        public ItemJSON(Type type, Object value) {
            this.type = type;
            switch (type) {
                case audio:
                    ifTypeAudio = (APIRecommendations.ItemJSON) value;
                    break;
                case aggregation:
                    ifTypeAggregation = (APIAggregations.AggregationJSON) value;
                    break;
            }
        }
    }
}
