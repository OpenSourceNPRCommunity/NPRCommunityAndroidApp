package com.nprcommunity.npronecommunity.Store;

import java.io.File;
import java.io.FileInputStream;

public interface CacheResponse {
    void executeFunc(FileInputStream fileInputStream, String url);
}
