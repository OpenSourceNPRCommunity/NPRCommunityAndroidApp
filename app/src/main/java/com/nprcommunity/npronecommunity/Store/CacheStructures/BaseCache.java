package com.nprcommunity.npronecommunity.Store.CacheStructures;

import java.util.Date;

public abstract class BaseCache<T> {
    protected Date originalDate;
    protected Date lastUpdatedDate;
    public final String urlParent;
    public final T data;

    public BaseCache(T data, String urlParent) {
        this.originalDate = new Date();
        this.lastUpdatedDate = new Date();
        this.urlParent = urlParent;
        this.data = data;
    }

    public void updateLastUpdated() {
        this.lastUpdatedDate = new Date();
    }
    public void setOriginalDate(Date originalDate) { this.originalDate = originalDate; };

    public Date getOriginalDate() {
        return  originalDate;
    }

    public boolean isExpired() {
        return false;
    }
}
