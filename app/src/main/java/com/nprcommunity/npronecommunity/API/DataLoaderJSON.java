package com.nprcommunity.npronecommunity.API;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
//import com.orhanobut.hawk.Hawk;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class DataLoaderJSON extends AsyncTask<String, Void, Boolean> {
    private static String TAG = "API.DataLoaderJSON";
    private String responseJSON;
    private API responseFunc;
    private APIDataResponse apiDataResponse;

    public DataLoaderJSON(API responseFunc, APIDataResponse apiDataResponse) {
        super();
        this.responseFunc = responseFunc;
        this.apiDataResponse = apiDataResponse;
    }

    protected Boolean doInBackground(String... urlAndToken) {
        GenericUrl url = new GenericUrl(urlAndToken[0]);
        HttpHeaders headers = new HttpHeaders();
        List<String> list = new ArrayList<>();
        list.add("Bearer " + urlAndToken[1]);
        headers.set("Authorization", list);
        HttpTransport transport = new NetHttpTransport();
        HttpResponse response = null;
        try {
            HttpRequest request = transport.createRequestFactory().buildGetRequest(url);
            request.setHeaders(headers);
            response = request.execute();
            responseJSON = response.parseAsString();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "doInBackground", e);
            responseJSON = null;
        } finally {
            if (response != null) {
                try {
                    response.disconnect();
                } catch (IOException e) {
                    Log.e(TAG, "doInBackground: Failed to disconnect", e);
                }
            }
        }
        return false;
    }

    protected void onPostExecute(Boolean b) {
        // TODO: check this.exception
        // TODO: do something with the feed
        responseFunc.executeFunc(responseJSON, b);
        apiDataResponse.executeFunc();
    }
}