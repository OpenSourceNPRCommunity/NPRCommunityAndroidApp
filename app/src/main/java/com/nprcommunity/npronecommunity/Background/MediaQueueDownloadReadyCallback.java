package com.nprcommunity.npronecommunity.Background;

import java.io.FileInputStream;

public interface MediaQueueDownloadReadyCallback {
    void callback(boolean success, FileInputStream fileInputStream);
}
