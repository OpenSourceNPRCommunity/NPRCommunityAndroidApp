package com.nprcommunity.npronecommunity.Store;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
    private CacheResponseMedia cacheResponseMedia;
    private final WeakReference<Context> weakContext;
    private String url = null;
    private ProgressCallback progressCallback;
    private static final int READ_TIMEOUT = 2000,
                                CONNECT_TIMEOUT = 2000;

    protected DownloadMediaTask(Context weakContext,
                                FileCache.Type type,
                                CacheResponseMedia cacheResponseMedia,
                                String url,
                                ProgressCallback progressCallback) {
        this.type = type;
        this.cacheResponseMedia = cacheResponseMedia;
        this.weakContext = new WeakReference<>(weakContext);
        this.url = url;
        this.progressCallback = progressCallback;
    }

    @Override
    public void run() {
        FileInputStream fileInputStream = null;
        while (true) {
            if (!isNetworkAvailable()) {
                progressCallback.updateProgress(0, 0, 0, ProgressCallback.Type.WAITING_FOR_INTERNET);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    break;
                }
            } else {
                fileInputStream = getFileInputStream();
                if (fileInputStream != null) {
                    break;
                }
            }
        }
        cacheResponseMedia.callback(fileInputStream, url);
    }

    private FileInputStream getFileInputStream() {
        Context context = weakContext.get();
        if(context == null) {
            Log.e(TAG, "doInBackground: activity is null");
            return null;
        }
        FileCache cache = FileCache.getInstances(context);
        FileInputStream cacheInputStream = null;
        URLConnection urlConnection;
        InputStream urlInputStream = null;
        int total = 0;
        try {
            //get url data //TODO add in check for get the url to add header if its to npr
            urlConnection = new URL(url).openConnection();
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(READ_TIMEOUT);
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
            Log.e(TAG, "getFileInputStream: malformed url stream [" + url + "] type [" +
                    type.name() + "]", e);
            return null;
        } catch (IOException e) {
            Log.e(TAG, "getFileInputStream: url stream", e);
            return null;
        }
        try {
            //save file to correct type
            cacheInputStream = cache.saveFile(url, urlInputStream, type,
                    progressCallback, total);
            Log.i(TAG, "getFileInputStream: successfully saved file: " + url);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "getFileInputStream: save file", e);
            return null;
        } finally {
            if (urlInputStream != null) {
                try {
                    urlInputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "getFileInputStream: failed to close input stream", e);
                }
            }
        }
        return cacheInputStream;
    }

    private boolean isNetworkAvailable() {
        Context context = weakContext.get();
        if(context == null) {
            Log.e(TAG, "doInBackground: activity is null");
            return false;
        }
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
}
