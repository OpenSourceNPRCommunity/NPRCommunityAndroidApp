package com.nprcommunity.npronecommunity.Background;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.nprcommunity.npronecommunity.API.APIDataResponse;
import com.nprcommunity.npronecommunity.API.Recommendations;
import com.orhanobut.hawk.Hawk;

import java.net.URL;

public class BackgroundAudioService extends Service {

    private final String TAG = "BACKGROUNDAUDIOSERVICE";
    private final IBinder iBinder = new LocalBinder();

    public BackgroundAudioService() {
        super();
    }

    @Override
    public void onCreate() {
        //Init Hawk
        Hawk.init(getApplicationContext()).build();
        updateLineUp();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    public class LocalBinder extends Binder {
        BackgroundAudioService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BackgroundAudioService.this;
        }
    }

    /**
     * Loads in a new media
     * @param url the media to load in
     */
    public void startNewMedia(URL url) {

    }

    /**
     * Plays the current media if exists
     */
    public void playMedia() {

    }

    /**
     * Pauses the current media if exists
     */
    public void pauseMedia() {

    }

    /**
     * Stops the current media if exists
     */
    public void stopMedia() {

    }

    private void updateLineUp() {
        Recommendations recommendations =
                new Recommendations(this, Recommendations.DEFAULT_RECOMMENDATIONS_URL);
        APIDataResponse recommendationsApiDataResponse = () -> {
            Log.d(TAG, "updateLineUp: ");
        };
        recommendations.updateData(recommendationsApiDataResponse);
    }
}
