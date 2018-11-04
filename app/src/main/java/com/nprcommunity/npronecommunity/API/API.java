package com.nprcommunity.npronecommunity.API;

import android.content.Context;
import android.support.annotation.NonNull;

import com.nprcommunity.npronecommunity.Config;
import com.nprcommunity.npronecommunity.Store.CacheStructures.BaseCache;
import com.nprcommunity.npronecommunity.Store.JSONCache;
import com.nprcommunity.npronecommunity.Store.SettingsAndTokenManager;

import java.io.InputStream;

import okhttp3.OkHttpClient;
import okio.Source;

public abstract class API {
    public String URL_BASE = "https://api.npr.org",
                    URL = "";
    private SettingsAndTokenManager tokenManager;

    protected Object data;

    private OkHttpClient okHttpClient = Config.OK_HTTP_CLIENT;

    public API(Context context) {
        tokenManager = new SettingsAndTokenManager(context);
    }

    public void executeFunc(String jsonData, Boolean success) {};

    /**
     * Checks if data exists in cached database
     * Does not set, is responsible for child
     * @param apiDataResponse
     */
    public void updateData(APIDataResponse apiDataResponse) {
        data = JSONCache.getObject(URL);
        //If the data is null, then reload
        if(data != null && !((BaseCache)data).isExpired()) {
            //Overrides the callback of this class
            //Insteads skips to execute function
            apiDataResponse.executeFunc();
        } else {
            DataLoaderJSON dataLoader = new DataLoaderJSON(this, apiDataResponse, okHttpClient);
            dataLoader.execute(URL, tokenManager.getToken());
        }
    }
}
