package com.nprcommunity.npronecommunity.Store;

import com.nprcommunity.npronecommunity.Store.CacheStructures.BaseCache;
import com.orhanobut.hawk.Hawk;

public class JSONCache {

    private static final Object lock = new Object();

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
            BaseCache tmpBaseCache;
            if (Hawk.contains(key)) {
                tmpBaseCache = Hawk.get(key);
                if (tmpBaseCache != null) {
                    baseCache.setOriginalDate(tmpBaseCache.getOriginalDate());
                }
            }
            baseCache.updateLastUpdated();
            Hawk.put(key, baseCache);
        }
    }
}
