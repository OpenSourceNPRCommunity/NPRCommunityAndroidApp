package com.nprcommunity.npronecommunity.Store;

import android.content.Context;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

public class DownloadMediaTask implements Runnable{

    private String TAG = "STORE.DOWNLOADMEDIATASK";
    private FileCache.Type type;
    private CacheResponse cacheResponse;
    private final WeakReference<Context> weakContext;
    private String url = null;
    private ProgressCallback progressCallback;
    private Boolean stopDownload = Boolean.FALSE;

    protected DownloadMediaTask(Context weakContext,
                                FileCache.Type type,
                                CacheResponse cacheResponse,
                                String url,
                                ProgressCallback progressCallback) {
        this.type = type;
        this.cacheResponse = cacheResponse;
        this.weakContext = new WeakReference<>(weakContext);
        this.url = url;
        this.progressCallback = progressCallback;
    }

    public void stopDownload() {
        stopDownload = Boolean.TRUE;
    }

    @Override
    public void run() {
        cacheResponse.executeFunc(getFileInputStream(), url);
    }

    private FileInputStream getFileInputStream() {
        Context context = weakContext.get();
        if(context == null) {
            Log.e(TAG, "doInBackground: activity is null");
            return null;
        }
        FileCache cache = FileCache.getInstances(context);
        FileInputStream cacheInputStream = null;
        boolean fileExists = cache.fileExists(url, type);
        if(!fileExists) {
            // file does not exist
            URLConnection urlConnection;
            InputStream urlInputStream = null;
            int total = 0;
            try {
                //get url data //TODO add in check for get the url to add header if its to npr
                urlConnection = new URL(url).openConnection();
                List<String> contentLength = urlConnection.getHeaderFields().get("content-Length");
                if (contentLength != null && contentLength.size() > 0) {
                    try {
                        total = Integer.parseInt(contentLength.get(0));
                    } catch (NumberFormatException e) {
                        total = -1;
                    }
                }
                urlInputStream = urlConnection.getInputStream();
            } catch (MalformedURLException e) {
                Log.e(TAG, "doInBackground: malformed url stream [" + url + "] type [" +
                        type.name() + "]", e);
                return null;
            } catch (IOException e) {
                Log.e(TAG, "doInBackground: url stream", e);
                return null;
            }
            try {
                //save file to correct type
                cacheInputStream = cache.saveFile(url, urlInputStream, type, context,
                        progressCallback, total, stopDownload);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "doInBackground: save file", e);
                return null;
            }
        } else {
            //get CacheInputStream
            try {
                cacheInputStream = cache.getInputStream(url, type, context);
            } catch (FileNotFoundException e) {
                //failed to find file: should not happen
                Log.e(TAG, "doInBackground: failed to get data although exists", e);
                return null;
            }
        }
        return cacheInputStream;
    }
}
