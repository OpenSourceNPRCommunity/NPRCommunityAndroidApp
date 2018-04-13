package com.nprcommunity.npronecommunity.Store;

import com.nprcommunity.npronecommunity.API.Recommendations;

import java.util.Date;

public abstract class BaseCache<T> {
    public final Date originalDate;
    private Date lastUpdatedDate;
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
}
