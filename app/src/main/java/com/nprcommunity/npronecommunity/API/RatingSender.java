package com.nprcommunity.npronecommunity.API;

import android.support.annotation.NonNull;
import android.util.Log;

import com.nprcommunity.npronecommunity.Background.BackgroundAudioService;
import com.nprcommunity.npronecommunity.Config;
import com.nprcommunity.npronecommunity.Store.CacheStructures.RecommendationCache;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Okio;

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
    private OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .build();

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
                Request request = new Request.Builder()
                        .url(RatingSender.this.url)
                        .addHeader("Authorization", "Bearer " + RatingSender.this.token)
                        .post(RequestBody.create(MediaType.parse("application/json"),
                                "[" + jsonAdapter.toJson(ratingJSON) + "]"))
                        .build();
                try (Response response = okHttpClient.newCall(request).execute()) {
                    Log.i(TAG, "sendAsyncRating: successfully sent for [" + ratingJSON + "]");
                    if (!response.isSuccessful() || response.body() == null) {
                        Log.w(TAG, "sendAsyncRating: response unsuccessful: url[" + url +
                                "] response code: " + response.code());
                        return;
                    }

                    //if uses recommendations
                    if (hasRecommend) {
                        Log.d(TAG, "sendAsyncRating: has recommendations[" + ratingJSON + "]");
                        //overrides default url in cache when saving
                        APIRecommendations recommendations = new APIRecommendations(
                                backgroundAudioService,
                                APIRecommendations.DEFAULT_RECOMMENDATIONS_URL
                        );
                        Log.d(TAG, "sendAsyncRating: parsing recommendations[" + ratingJSON + "]");
                        recommendations.executeFunc(response.body().string(),
                                true);
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
                } catch (IOException e) {
                    Log.e(TAG, "sendAsyncRating: failed to get call", e);
                }
            }).start();
        }
    }
}
