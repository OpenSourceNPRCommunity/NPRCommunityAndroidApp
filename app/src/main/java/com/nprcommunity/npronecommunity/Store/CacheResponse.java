package com.nprcommunity.npronecommunity.Store;

import java.io.File;
import java.io.FileInputStream;

public interface CacheResponse {
    //TODO: Fix up method to not uneccessary return file input stream that is not used in BackgroundAudioService
    public void executeFunc(FileInputStream fileInputStream, String url);
}
