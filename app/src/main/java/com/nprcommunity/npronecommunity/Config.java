package com.nprcommunity.npronecommunity;

public final class Config {
    public static final String LOGIN_URL = "LOGIN_URL";
    public static final String OATH_URL = "OATH_URL";
    //disables reload of url information
    public static final boolean ENABLE_CACHE_TIMEOUT = BuildConfig.DEBUG ? true:
            /*do not edit*/ true;
    //disables sending updates to remote endpoint
    public static final boolean ENABLE_RATING_SENDER = BuildConfig.DEBUG ? true:
            /*do not edit*/ true;
    //disables sending updates to remote endpoint
    public static final boolean ENABLE_STATION_SENDER = BuildConfig.DEBUG ? true:
            /*do not edit*/ true;
}
