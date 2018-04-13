package com.nprcommunity.npronecommunity.Store;

import com.orhanobut.hawk.Hawk;

public class JSONCache {

    public static final Object lock = new Object();

    /**
     * Getting an object thread safe.
     * @param key
     * @return
     */
    public static BaseCache getObject(String key) {
        synchronized (lock) {
            BaseCache baseCache = null;
            if(Hawk.contains(key)) {
                baseCache = Hawk.get(key);
            }
            return baseCache;
        }
    }

    public static void putObject(String key, BaseCache baseCache) {
        synchronized (lock) {
            if (Hawk.contains(key)) {
                baseCache = Hawk.get(key);
            }
            baseCache.updateLastUpdated();
            Hawk.put(key, baseCache);
        }
    }
}
