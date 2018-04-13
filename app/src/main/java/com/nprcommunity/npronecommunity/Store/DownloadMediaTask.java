package com.nprcommunity.npronecommunity.Store;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;

public class DownloadMediaTask extends AsyncTask<String, Void, FileInputStream> {
    private String TAG = "STORE.DOWNLOADMEDIATASK";

    private FileCache.Type type;

    private CacheResponse cacheResponse;

    private final WeakReference<Context> weakContext;

    public DownloadMediaTask(Context weakContext, FileCache.Type type, CacheResponse cacheResponse) {
        this.type = type;
        this.cacheResponse = cacheResponse;
        this.weakContext = new WeakReference<>(weakContext);
    }

    protected FileInputStream doInBackground(String... urls) {
        Context context = weakContext.get();
        if(context == null) {
            Log.e(TAG, "doInBackground: activity is null");
            return null;
        }
        String url = urls[0];
        FileCache cache = FileCache.getInstances(context);
        FileInputStream cacheInputStream = null;
        boolean fileExists = cache.fileExists(url, FileCache.Type.IMAGE, context);
        if(!fileExists) {
            // file does not exist
            InputStream urlInputStream = null;
            try {
                //get url data
                urlInputStream = new URL(url).openStream();
            } catch (IOException e) {
                Log.e(TAG, "doInBackground: url stream", e);
            }
            try {
                //save file to correct type
                cacheInputStream = cache.saveFile(url, urlInputStream, type, context);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "doInBackground: save file", e);
            }
            if(cacheInputStream == null) {
                //if saving the file failed, will return null InputStream
                Log.e(TAG, "doInBackground: cacheInputStream failed");
            }
        } else {
            //get CacheInputStream
            try {
                cacheInputStream = cache.getInputStream(url, type, context);
            } catch (FileNotFoundException e) {
                //failed to find file: should not happen
                Log.e(TAG, "doInBackground: failed to get data although exists", e);
            }
        }
        return cacheInputStream;
    }

    protected void onPostExecute(FileInputStream fileInputStream) {
       cacheResponse.executeFunc(fileInputStream);
    }
}
