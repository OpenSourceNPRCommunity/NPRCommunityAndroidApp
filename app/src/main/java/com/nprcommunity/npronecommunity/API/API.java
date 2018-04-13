package com.nprcommunity.npronecommunity.API;

import android.content.Context;

import com.nprcommunity.npronecommunity.Store.JSONCache;
import com.nprcommunity.npronecommunity.TokenManager;
import com.orhanobut.hawk.Hawk;

public abstract class API {
    public String URL_BASE = "https://api.npr.org",
                    URL = "";
    private TokenManager tokenManager;

    protected Object data;

    private API() {}

    public API(Context context) {
        tokenManager = new TokenManager(context);
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
        if(data != null) {
            //Overrides the executeFunc of this class
            //Insteads skips to execute function
            apiDataResponse.executeFunc();
        } else {
            DataLoaderJSON dataLoader = new DataLoaderJSON(this, apiDataResponse);
            dataLoader.execute(URL, tokenManager.GetToken());
        }
    };
}
