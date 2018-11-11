package com.nprcommunity.npronecommunity.API;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DataLoaderJSON extends AsyncTask<String, Void, Boolean> {
    private static String TAG = "API.DataLoaderJSON";
    private String responseJSON;
    private API responseFunc;
    private APIDataResponse apiDataResponse;
    private final OkHttpClient okHttpClient;

    public DataLoaderJSON(API responseFunc, APIDataResponse apiDataResponse, @NonNull OkHttpClient okHttpClient) {
        super();
        this.responseFunc = responseFunc;
        this.apiDataResponse = apiDataResponse;
        this.okHttpClient = okHttpClient;
    }

    protected Boolean doInBackground(String... urlAndToken) {
        Request request = new Request.Builder()
                .url(urlAndToken[0])
                .addHeader("Authorization", "Bearer " + urlAndToken[1])
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                Log.w(TAG, "doInBackground: response unsuccessful: url[" + urlAndToken [0] +
                        "] response code: " + response.code());
                return false;
            }
            responseJSON = response.body().string();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "doInBackground: failed to get call", e);
        }
        return false;
    }

    protected void onPostExecute(Boolean b) {
        responseFunc.executeFunc(responseJSON, b);
        apiDataResponse.callback();
    }
}