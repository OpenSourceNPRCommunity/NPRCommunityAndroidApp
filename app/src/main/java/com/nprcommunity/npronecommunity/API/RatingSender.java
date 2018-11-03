package com.nprcommunity.npronecommunity.API;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.nprcommunity.npronecommunity.Background.BackgroundAudioService;
import com.nprcommunity.npronecommunity.Config;
import com.nprcommunity.npronecommunity.Store.CacheStructures.RecommendationCache;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class used for sending updates Ratings
 */
public class RatingSender {

    private static final String TAG = "RatingSender";
    private APIRecommendations.RatingJSON ratingJSON;
    private String url,
                    token;
    private int TIMEOUT = 10000; //10 second timeout for connect
    public static final String BASE_URL = "https://listening.api.npr.org/v2/ratings";
    private BackgroundAudioService backgroundAudioService;
    private boolean hasRecommend = false;

    public enum Type {
        START,
        COMPLETED,
        SKIP,
        THUMBUP,
        PASS, //currently not used
        TIMEOUT,
    }

    private RatingSender(){}

    public RatingSender(@NonNull String url,
                        @NonNull boolean hasRecommend,
                        @NonNull APIRecommendations.RatingJSON ratingJSON,
                        @NonNull String token,
                        @NonNull BackgroundAudioService backgroundAudioService) {
        this.ratingJSON = ratingJSON;
        this.url = url;
        this.hasRecommend = hasRecommend;
        this.token = token;
        this.backgroundAudioService = backgroundAudioService;
    }

    public void sendAsyncRating() {
        if (Config.ENABLE_RATING_SENDER) {
            new Thread(() -> {
                Moshi moshi = new Moshi.Builder().build();
                JsonAdapter<APIRecommendations.RatingJSON> jsonAdapter = moshi.adapter(APIRecommendations.RatingJSON.class);
                //endpoint expects ratingJSON to be in an array
                String str = "[" + jsonAdapter.toJson(ratingJSON) + "]";
                GenericUrl url = new GenericUrl(RatingSender.this.url);
                HttpHeaders headers = new HttpHeaders();
                List<String> list = new ArrayList<>();
                list.add("Bearer " + RatingSender.this.token);
                headers.set("Authorization", list);
                HttpTransport transport = new NetHttpTransport();
                try {
                    HttpRequest request = transport.createRequestFactory()
                            .buildPostRequest(
                                    url,
                                    ByteArrayContent.fromString("application/json", str)
                            );
                    request.setHeaders(headers);
                    request.setConnectTimeout(TIMEOUT);
                    request.setReadTimeout(TIMEOUT);
                    HttpResponse response = request.execute();
                    try {

                        Log.i(TAG, "sendAsyncRating: successfully sent for [" + ratingJSON + "]");

                        //if uses recommendations
                        if (hasRecommend) {
                            //overrides default url in cache when saving
                            APIRecommendations recommendations = new APIRecommendations(
                                    backgroundAudioService,
                                    APIRecommendations.DEFAULT_RECOMMENDATIONS_URL
                            );
                            recommendations.executeFunc(response.parseAsString(), true);
                            RecommendationCache recommendationCache = recommendations.getData();
                            //add in items that were recommended
                            if (recommendationCache != null && recommendationCache.data != null) {
                                for (APIRecommendations.ItemJSON itemJSON : recommendationCache.data.items) {
                                    //add them to the queue
                                    backgroundAudioService.addToQueue(itemJSON);
                                }
                            } else {
                                Log.e(TAG, "sendAsyncRating: recommendations is null");
                            }
                        }
                    } finally {
                        if (response != null) {
                            response.disconnect();
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "sendAsyncRating", e);
                }
            }).start();
        }
    }
}
