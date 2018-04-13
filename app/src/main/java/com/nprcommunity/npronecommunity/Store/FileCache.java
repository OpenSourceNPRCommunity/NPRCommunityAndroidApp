package com.nprcommunity.npronecommunity.Store;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class FileCache {
    private static FileCache fileCache;
    private String TAG = "STORE.CACHE",
                    AUDIO_PATH,
                    IMAGE_PATH,
                    QUEUE_FILE_PATH;
    private ConcurrentMap<String, Boolean> audioConcurrentMap;
    private ConcurrentMap<String, Boolean> imageConcurrentMap;

    private static final Object lock = new Object();

    public enum Type {
        IMAGE,
        AUDIO
    }

    private FileCache(Context context) {
        AUDIO_PATH = context.getFilesDir() + File.separator + "AudioFiles";
        setupPath(AUDIO_PATH);
        IMAGE_PATH = context.getFilesDir() + File.separator + "ImageFiles";
        setupPath(IMAGE_PATH);
        QUEUE_FILE_PATH = context.getFilesDir() + File.separator + "QueueFile";
    }

    private void updateMaps() {
        audioConcurrentMap = new ConcurrentHashMap<>();
        imageConcurrentMap = new ConcurrentHashMap<>();
        List<List<String>> arrayLists = fileCache.getFiles();
        for(String audio: arrayLists.get(0)) {
            imageConcurrentMap.putIfAbsent(audio, Boolean.TRUE);
        }
        for(String image: arrayLists.get(1)) {
            audioConcurrentMap.putIfAbsent(image, Boolean.TRUE);
        }
    }

    private void setupPath(String path) {
        //Set up fileCache
        File folder = new File(path);
        if(!folder.exists()) {
            if(!folder.mkdirs()) {
                Log.e(TAG, "FileCache: error making dir");
            }
        }
    }

    public static FileCache getInstances(Context context) {
        if(fileCache == null) {
            fileCache = new FileCache(context);
            fileCache.updateMaps();
        }
        return fileCache;
    }

    public String getQueueFileLocation() {
        return QUEUE_FILE_PATH;
    }

    public boolean fileExists(String filename, Type type, Context context) {
        String path = getFilePath(filename, type);
        if(path == null) {
            return false;
        }
        synchronized (lock) {
            File file = new File(path);
            return file.exists();
        }
    }

    public List<List<String>> getFiles() {
        List<List<String>> arrayLists = new ArrayList<>(2);
        File dir = new File(IMAGE_PATH);
        arrayLists.add(Arrays.asList(dir.list()));
        dir = new File(AUDIO_PATH);
        arrayLists.add(Arrays.asList(dir.list()));
        return arrayLists;
    }

    private String getFilePath(String filename, Type type) {
        String path = null,
                safeFilename = Base64.encodeToString(filename.getBytes(),
                        Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
        switch (type) {
            case IMAGE:
                path = IMAGE_PATH + File.separator + safeFilename;
                break;
            case AUDIO:
                path = AUDIO_PATH + File.separator + safeFilename;
                break;
        }
        return path;
    }

    public Bitmap getImage(String filename, Context context) {
        String path = getFilePath(filename, Type.AUDIO);
        if(path == null) {
            return null;
        }
        Bitmap bitmap = null;
        synchronized (lock) {
            try {
                FileInputStream in = context.openFileInput(path);
                bitmap = BitmapFactory.decodeStream(in);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "getImage", e);
            }
        }
        return bitmap;
    }

    public void getImage(String filename, Context context, CacheResponse cacheResponse) {
        new DownloadMediaTask(context, Type.IMAGE, cacheResponse).execute(filename);

        String path = getFilePath(filename, Type.IMAGE);
        if(fileExists(filename, Type.IMAGE, context)) {
            //The file exists load in the bitmap
            Bitmap bitmap = null;
            synchronized (lock) {
                try {
                    FileInputStream in = context.openFileInput(path);
                    bitmap = BitmapFactory.decodeStream(in);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "getImage: " + filename, e);
                    cacheResponse.executeFunc(null);
                }
            }
        } else {
            //The file does not exist
            new DownloadMediaTask(context, Type.IMAGE, cacheResponse).execute(filename);
        }
    }

//    public void getFile(String filename, Context context, CacheResponse cacheResponse) {
//        String path = getFilePath(filename, Type.AUDIO);
//        if(fileExists(filename, Type.IMAGE, context)) {
//            Bitmap bitmap = null;
//            synchronized (lock) {
//                try {
//                    FileInputStream in = context.openFileInput(path);
//                    bitmap = BitmapFactory.decodeStream(in);
//                } catch (FileNotFoundException e) {
//                    Log.e(TAG, "getImage", e);
//                }
//            }
//        } else {
//            saveFile()
//        }
//        return bitmap;
//    }

    public FileInputStream getInputStream(String filename, Type type, Context context) throws FileNotFoundException {
//        return context.openFileInput(getFilePath(filename, type));
        String path = getFilePath(filename, type);
        File file = new File(path);
        return new FileInputStream(file);
    }

    public FileInputStream saveFile(String filename, InputStream inputStream, Type type, Context context) throws FileNotFoundException {
        String path = getFilePath(filename, type);
        File file = new File(path);
        synchronized (lock) {
            FileOutputStream outputStream;
            try {
                outputStream = new FileOutputStream(file);
//                outputStream = context.openFileOutput(path, Context.MODE_PRIVATE);
                byte[] buffer = new byte[1024];
                int len;
                while((len = inputStream.read(buffer)) > -1) {
                    outputStream.write(buffer, 0, len);
                }
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "saveFile: outputstream", e);
                return null;
            }
        }
        return getInputStream(filename, type, context);
    }
}
