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
import com.nprcommunity.npronecommunity.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StationSender {

    private static final String TAG = "StationSender";
    private String url,
            token;
    private int TIMEOUT = 10000; //10 second timeout for connect
    private int stationId;
    public static final String DEFAULT_URL = "https://identity.api.npr.org/v2/stations";

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
                //endpoint expects ratingJSON to be in an array
                String str = "[" + stationId + "]";
                GenericUrl url = new GenericUrl(StationSender.this.url);
                HttpHeaders headers = new HttpHeaders();
                List<String> list = new ArrayList<>();
                list.add("Bearer " + StationSender.this.token);
                headers.set("Authorization", list);
                HttpTransport transport = new NetHttpTransport();
                try {
                    HttpRequest request = transport.createRequestFactory()
                            .buildPutRequest(
                                    url,
                                    ByteArrayContent.fromString("application/json", str)
                            );
                    request.setHeaders(headers);
                    request.setConnectTimeout(TIMEOUT);
                    request.setReadTimeout(TIMEOUT);
                    HttpResponse response = request.execute();
                    Log.i(TAG, "sendAsyncStation: successfully sent for [" + stationId + "]");
                    stationSenderInterface.execAfter(response.isSuccessStatusCode());
                } catch (IOException e) {
                    Log.e(TAG, "sendAsyncStation", e);
                    stationSenderInterface.execAfter(false);
                }
            }).start();
        }
    }
}
