package com.nprcommunity.npronecommunity.API;

import android.content.Context;
import android.widget.Toast;

import com.nprcommunity.npronecommunity.Config;
import com.nprcommunity.npronecommunity.R;
import com.nprcommunity.npronecommunity.Store.CacheStructures.BaseCache;
import com.nprcommunity.npronecommunity.Store.JSONCache;
import com.nprcommunity.npronecommunity.Store.SettingsAndTokenManager;

import java.lang.ref.WeakReference;

import okhttp3.OkHttpClient;

public abstract class API {
    public String URL_BASE = "https://api.npr.org",
                    URL = "";
    private SettingsAndTokenManager tokenManager;

    protected Object data;

    private OkHttpClient okHttpClient = Config.OK_HTTP_CLIENT;

    private Context context;

    public API(Context context) {
        this.tokenManager = new SettingsAndTokenManager(context);
        this.context = context;
    }

    public void executeFunc(String jsonData, Boolean success) {
        if (!success) {
            Toast.makeText(context, R.string.error_download_generic, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Checks if data exists in cached database
     * Does not set, is responsible for child
     * @param apiDataResponse
     */
    public void updateData(APIDataResponse apiDataResponse, boolean forceReload) {
        data = JSONCache.getObject(URL);
        //If the data is null, then reload
        if(data != null && !((BaseCache)data).isExpired() && !forceReload) {
            //Overrides the callback of this class
            //Insteads skips to execute function
            apiDataResponse.callback();
        } else {
            // sets the data to null even if found because it was forced
            if (forceReload) {
                data = null;
            }
            DataLoaderJSON dataLoader = new DataLoaderJSON(this, apiDataResponse, okHttpClient);
            dataLoader.execute(URL, tokenManager.getToken());
        }
    }
}
