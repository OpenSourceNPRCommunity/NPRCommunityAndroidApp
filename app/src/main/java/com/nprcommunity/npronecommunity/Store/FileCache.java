package com.nprcommunity.npronecommunity.Store;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

public class FileCache {
    private static FileCache fileCache;
    private String TAG = "STORE.CACHE",
                    AUDIO_PATH,
                    IMAGE_PATH;
    public static final int MILLI_SECOND_IN_NANO = 1000000,
                        MILLI_NOTIFY = 750;

    public enum Type {
        IMAGE,
        AUDIO
    }

    private FileCache(Context context) {
        AUDIO_PATH = context.getFilesDir() + File.separator + "AudioFiles";
        setupPath(AUDIO_PATH);
        IMAGE_PATH = context.getFilesDir() + File.separator + "ImageFiles";
        setupPath(IMAGE_PATH);
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
        }
        return fileCache;
    }

    public boolean fileExists(String filename, Type type) {
        String path = getFilePath(filename, type);
        if(path == null) {
            return false;
        }
        File file = new File(path);
        return file.exists();
    }

    public File getFile(@NonNull String url,@NonNull Type type) {
        return new File(getFilePath(url, type));
    }

    public List<List<String>> getFiles() {
        List<List<String>> arrayLists = new ArrayList<>(2);
        File dir = new File(IMAGE_PATH);
        arrayLists.add(Arrays.asList(dir.list()));
        dir = new File(AUDIO_PATH);
        arrayLists.add(Arrays.asList(dir.list()));
        return arrayLists;
    }

    private String getFilePath(@NonNull String filename,@NonNull Type type) {
        byte[] digestedFileName = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            digestedFileName = messageDigest.digest(filename.getBytes());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "FileCache: error message digest not supported, this shouldnt happen", e);
        }
        String path = null, safeFilename;
        if (digestedFileName != null && digestedFileName.length > 0) {
            safeFilename = Base64.encodeToString(digestedFileName,
                    Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
        } else {
            //Should not ever be called but if it is may result in failing to download
            //data because of the length of the file name being to long and the use of urls
            safeFilename = Base64.encodeToString(filename.getBytes(),
                    Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
        }
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

    public void getImage(String filename, Context context, CacheResponse cacheResponse,
                         ProgressCallback progressCallback) {
        String path = getFilePath(filename, Type.IMAGE);
        if(fileExists(filename, Type.IMAGE)) {
            //The file exists load in the audio
            try {
                File file = new File(path);
                FileInputStream in = new FileInputStream(file);
                cacheResponse.executeFunc(in, filename);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "getImage: " + filename, e);
                cacheResponse.executeFunc(null, null);
            }
        } else {
            //The file does not exist
            DownloadManager downloadManager = DownloadManager.getInstance();
            downloadManager.execute(
                new DownloadMediaTask(context, Type.IMAGE, cacheResponse, filename,
                    progressCallback)
            );
        }
    }

    public DownloadMediaTask getAudio(String filename, boolean forceRedownload,
                           Context context, CacheResponse cacheResponse,
                           ProgressCallback progressCallback) {
        String path = getFilePath(filename, Type.AUDIO);
        if(!forceRedownload && fileExists(filename, Type.AUDIO)) {
            //The file exists load in the audio
            try {
                File file = new File(path);
                FileInputStream in = new FileInputStream(file);
                cacheResponse.executeFunc(in, filename);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "getAudio: " + filename, e);
                cacheResponse.executeFunc(null, null);
            }
        } else {
            //The file does not exist
            DownloadManager downloadManager = DownloadManager.getInstance();
            DownloadMediaTask downloadMediaTask = new DownloadMediaTask(context, Type.AUDIO, cacheResponse, filename,
                            progressCallback);
            downloadManager.execute(downloadMediaTask);
            return downloadMediaTask;
        }
        return null;
    }

    public FileInputStream getInputStream(String filename, Type type, Context context) throws FileNotFoundException {
        String path = getFilePath(filename, type);
        File file = new File(path);
        return new FileInputStream(file);
    }

    public boolean deleteFileIfExists(String filename, Type type) {
        if (fileExists(filename, type)) {
            String path = getFilePath(filename, type);
            File file = new File(path);
            //todo maybe race condition for deleting file, downloading....
            return file.delete();
        }
        return true;
    }

    public FileInputStream saveFile(String filename,
                                    InputStream inputStream,
                                    Type type,
                                    Context context,
                                    ProgressCallback progressCallback,
                                    int total,
                                    Boolean stopDownload) throws FileNotFoundException {
        String path = getFilePath(filename, type);
        File file = new File(path);
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(file);
//            FileLock fileLock = null;
            int count = 0;
//            try {
//                fileLock = outputStream.getChannel().lock();
                byte[] buffer = new byte[1024];
                int len;
                int prevProgress = count;
                long start = System.nanoTime();
                long end;
                while ((len = inputStream.read(buffer)) > -1) {
                    if (stopDownload) {
                        Log.d(TAG, "saveFile: stopDownload requested breaking");
                        return null;
                    }
                    outputStream.write(buffer, 0, len);
                    count += len;
                    end = System.nanoTime();
                    if ((end-start)/MILLI_SECOND_IN_NANO >= MILLI_NOTIFY) {
                        progressCallback.updateProgress(count, total,
                                (count - prevProgress)*(1000/MILLI_NOTIFY));
                        start = end;
                        prevProgress = count;
                    }
                }
                //set finished to download
                progressCallback.updateProgress(total, total, 0);
//            } finally {
//                if (fileLock != null) {
//                    fileLock.release();
//                }
//            }
            outputStream.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "saveFile: filenot found outputstream filename:" + filename, e);
            ///todo log make toast saying not found?
            return null;
        } catch (Exception e) {
            Log.e(TAG, "saveFile: outputstream filename:" + filename, e);
            return null;
        }
        return getInputStream(filename, type, context);
    }
}
