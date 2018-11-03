package com.nprcommunity.npronecommunity.Store;

import java.io.File;
import java.io.FileInputStream;

public interface CacheResponseMedia {
    public void callback(FileInputStream fileInputStream, String url);
}
