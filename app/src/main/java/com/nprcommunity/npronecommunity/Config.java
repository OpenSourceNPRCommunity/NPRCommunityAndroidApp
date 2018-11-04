package com.nprcommunity.npronecommunity;

import okhttp3.OkHttpClient;
import okreplay.OkReplayInterceptor;

public final class Config {
    public static final String LOGIN_URL = "https://<domain-name-here>/oauth/start";
    public static final String OATH_URL = "https://<domain-name-here>/oauth/success-418";
    //disables reload of url information
    public static final boolean ENABLE_CACHE_TIMEOUT = BuildConfig.DEBUG ? true:
            /*do not edit*/ true;
    //disables sending updates to remote endpoint
    public static final boolean ENABLE_RATING_SENDER = BuildConfig.DEBUG ? true:
            /*do not edit*/ true;
    //disables sending updates to remote endpoint
    public static final boolean ENABLE_STATION_SENDER = BuildConfig.DEBUG ? true:
            /*do not edit*/ true;

    public static final OkReplayInterceptor okReplayInterceptor = new OkReplayInterceptor();
    public static final OkHttpClient OK_HTTP_CLIENT = BuildConfig.DEBUG ?
            new OkHttpClient.Builder()
                    .addInterceptor(okReplayInterceptor)
                    .build() : new OkHttpClient();
}