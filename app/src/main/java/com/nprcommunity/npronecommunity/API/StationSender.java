package com.nprcommunity.npronecommunity.API;

import android.support.annotation.NonNull;
import android.util.Log;

import com.nprcommunity.npronecommunity.Config;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class StationSender {

    private static final String TAG = "StationSender";
    private String url,
            token;
    private int TIMEOUT = 10000; //10 second timeout for connect
    private int stationId;
    public static final String DEFAULT_URL = "https://identity.api.npr.org/v2/stations";

    private OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .build();

    private StationSender(){}

    public StationSender(@NonNull String token,
                         int stationId) {
        this.url = DEFAULT_URL;
        this.token = token;
        this.stationId = stationId;
    }

    public StationSender(@NonNull String url,
                         @NonNull String token,
                         int stationId) {
        this.url = url;
        this.token = token;
        this.stationId = stationId;
    }

    public void sendAsyncStation(StationSenderInterface stationSenderInterface) {
        if (Config.ENABLE_STATION_SENDER) {
            new Thread(() -> {

                boolean success = false;
                Moshi moshi = new Moshi.Builder().build();
                JsonAdapter<APIRecommendations.RatingJSON> jsonAdapter = moshi.adapter(APIRecommendations.RatingJSON.class);
                Request request = new Request.Builder()
                        .url(StationSender.this.url)
                        .addHeader("Authorization", "Bearer " + StationSender.this.token)
                        .post(RequestBody.create(MediaType.parse("application/json"),
                                "[" + stationId + "]"))
                        .build();
                try (Response response = okHttpClient.newCall(request).execute()) {
                    Log.i(TAG, "sendAsyncStation: successfully sent");
                    if (!response.isSuccessful()) {
                        Log.w(TAG, "sendAsyncStation: response unsuccessful: url[" + url +
                                "] response code: " + response.code());
                    }
                    success = response.isSuccessful();
                } catch (IOException e) {
                    Log.e(TAG, "sendAsyncStation: failed to get call", e);
                }
                stationSenderInterface.execAfter(success);
            }).start();
        }
    }
}
