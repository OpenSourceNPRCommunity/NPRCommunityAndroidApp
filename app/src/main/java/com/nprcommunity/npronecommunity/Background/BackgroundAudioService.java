package com.nprcommunity.npronecommunity.Background;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.nprcommunity.npronecommunity.API.APIDataResponse;
import com.nprcommunity.npronecommunity.API.APIRecommendations;
import com.nprcommunity.npronecommunity.API.RatingSender;
import com.nprcommunity.npronecommunity.API.Shared;
import com.nprcommunity.npronecommunity.Layout.Callback.ContentQueuePlayingListener;
import com.nprcommunity.npronecommunity.R;
import com.nprcommunity.npronecommunity.Store.CacheStructures.RecommendationCache;
import com.nprcommunity.npronecommunity.Store.FileCache;
import com.nprcommunity.npronecommunity.Store.SettingsAndTokenManager;
import com.nprcommunity.npronecommunity.Util;
import com.orhanobut.hawk.Hawk;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

public class BackgroundAudioService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnCompletionListener,
        AudioManager.OnAudioFocusChangeListener,
        ContentQueuePlayingListener {

    private final String TAG = "BACKGROUNDAUDIOSERVICE";
    private final IBinder iBinder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private MediaQueueManager mediaQueueManager;
    private APIRecommendations.ItemJSON currentMedia;
    private FileCache fileCache;
    private AudioManager audioManager;
    private SettingsAndTokenManager settingsAndTokenManager;
    private MediaQueueDownloadManager mediaQueueDownloadManager;
    private Observable observable = new Observable() {
        @Override
        public void notifyObservers(Object arg) {
            this.setChanged();
            super.notifyObservers(arg);
        }
    };
    private boolean isPrepared = false,
            isPlaying = false,
            wasPlayingBeforeLostFocus = false,
            playMedia = true,
            isCompleted = false;
    private static final Object lock = new Object();
    private volatile boolean loadingLineUp = false;

    private final int MAX_AUDIO_LEVEL = 100;

    public static final String ACTION = "ACTION";

    private int minimalQueueSize = 2;

    @Override
    public void swap(int fromPosition, int toPosition) {
        mediaQueueManager.swap(fromPosition, toPosition);
        if (toPosition == 0) {
            nextMediaHelper(true, false);
        } else if (fromPosition == 0) {
            nextMediaHelper(true, false);
        }
    }

    @Override
    public void remove(int position) {

        //send update
        //this is mainly used for items that are removed before played, must send update
        if (isCompleted) {
            //if is completed send that
            sendRatingUpdate(RatingSender.Type.COMPLETED);
        } else {
            if (position == 0) {
                //if not completed but is currently listening to it
                sendRatingUpdate(RatingSender.Type.SKIP);
            } else {
                //not completed and not currently listening too
                sendRatingUpdate(RatingSender.Type.PASS);
            }
        }

        mediaQueueDownloadManager.removeItem(position, mediaQueueManager.getQueueTrack(position));
        mediaQueueManager.remove(position);
        if (position == 0) {
            nextMediaHelper(true, false);
        }
        if (mediaQueueManager.queueSize() <= minimalQueueSize) {
            autoLoadNextUpQueue();
        }
    }

    @Override
    public int remove(APIRecommendations.ItemJSON itemJSON) {
        int removeIndex = mediaQueueManager.getMediaIndex(itemJSON);
        if (removeIndex >= 0) {
            remove(removeIndex);
        } else {
            Log.e(TAG, "remove: Error removing [" + itemJSON +"] not found [" +
                removeIndex + "]");
        }
        return removeIndex;
    }

    public enum Action {
        PAUSE_BUTTON,
        PLAY_BUTTON,
        SEEK_BUTTON,
        MEDIA_TITLE,
        MEDIA_NEXT,
        SEEK_CHANGE,
        MEDIA_PREPARED,
        MEDIA_ADDED_TO_QUEUE,
        MEDIA_DOWNLOADING_PROGRESS,
        MEDIA_ERROR_LOADING,
        MEDIA_COMPLETE,
    }

    public enum ActionExtras {
        MEDIA_PREPARED_HREF,
        MEDIA_PREPARED_IS_SKIPPABLE,
        MEDIA_ERROR_LOADING_REMOVE_ITEM,
        MEDIA_NEXT_LAST_MEDIA_HREF,
        MEDIA_NEXT_IS_SKIPPABLE,
    }

    public BackgroundAudioService() {
        super();
    }

    @Override
    public void onCreate() {
        //Init Hawk
        if (!Hawk.isBuilt()) {
            Hawk.init(getApplicationContext()).build();
        }

        //Setup Media Player
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        //set volume to max for this app
        float maxAudio = getVolume(MAX_AUDIO_LEVEL);
        mediaPlayer.setVolume(maxAudio, maxAudio);
        //TODO: Might need a wifi lock as well

        mediaQueueManager = MediaQueueManager.getInstance(this);
        fileCache = FileCache.getInstances(this);

        //TODO in the future use the new standard for audio manager
        //for reference https://developer.android.com/guide/topics/media-apps/audio-focus
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        settingsAndTokenManager = new SettingsAndTokenManager(this);

        mediaQueueDownloadManager = new MediaQueueDownloadManager(
                mediaQueueManager,
                observable,
                fileCache,
                this,
                this
        );

        nextMedia(false);
        startSeekUpdateThread();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isPrepared = true;
        isCompleted = false;

        //send update to all observers
        Bundle bundleMediaNext = new Bundle();
        bundleMediaNext.putString(ACTION, Action.MEDIA_PREPARED.name());
        bundleMediaNext.putBoolean(
                ActionExtras.MEDIA_PREPARED_IS_SKIPPABLE.name(),
                currentMedia.attributes.isSkippable()
        );
        observable.notifyObservers(bundleMediaNext);

        if (!playMedia) {
            return;
        }
        requestAudioFocusAndPlay();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "onError: [" + what + "] [" + extra + "]");
        mp.reset();
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        isPlaying = false;
        isCompleted = true;
        pauseMedia();
        if (settingsAndTokenManager.getConfigBoolean(
                SettingsAndTokenManager.SettingsKey.AUTO_PLAY_ENABLED,
                true)) {
            nextMedia(true);
        } else {
            //send update to reanable next button if disabled
            Bundle bundleMediaComplete = new Bundle();
            bundleMediaComplete.putString(ACTION, Action.MEDIA_COMPLETE.name());
            observable.notifyObservers(bundleMediaComplete);
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            // Permanent loss of audio focus
            // PAUSE playback immediately

            // losing permanently, set was playing to false no matter what.
            wasPlayingBeforeLostFocus = false;

            this.pauseMedia();
        }
        else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            // PAUSE playback

            // set last playback to whether or not music was playing
            wasPlayingBeforeLostFocus = isPlaying;

            this.pauseMedia();
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // Lower the volume, keep playing
            float quarterVolume = getVolume(MAX_AUDIO_LEVEL/4);
            synchronized (lock) {
                mediaPlayer.setVolume(quarterVolume, quarterVolume);
            }
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // Your app has been granted audio focus again
            // Raise volume to normal, restart playback if necessary
            float maxVolume = getVolume(MAX_AUDIO_LEVEL);
            synchronized (lock) {
                mediaPlayer.setVolume(maxVolume, maxVolume);
            }

            // if the media was playing before lost focus then continue playing
            if (wasPlayingBeforeLostFocus) {
                playMedia();
            }
        }
    }

    @Override
    public void onDestroy() {
        stopAndReleaseMedia();
        audioManager.abandonAudioFocus(this);
        super.onDestroy();
    }

    private float getVolume(int level) {
        if (level > MAX_AUDIO_LEVEL) {
            level = MAX_AUDIO_LEVEL;
        }
        final float max = (float) 1.0;
        return max-((float)(Math.log(MAX_AUDIO_LEVEL-level)/Math.log(MAX_AUDIO_LEVEL)));
    }

    public class LocalBinder extends Binder {
        public BackgroundAudioService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BackgroundAudioService.this;
        }
    }

    /**
     * Sets the play now media, which loads the currently selected track as the play next and
     * moves forward in playing it
     * @param queueItem
     */
    public void playMediaNow(APIRecommendations.ItemJSON queueItem) {
        int index = mediaQueueManager.getMediaIndex(queueItem);
        if (index >= 0) {
            swap(index, 0);
        } else if (mediaQueueManager.playMediaNow(queueItem)) {
            nextMediaHelper(true, false);
        } else {
            Log.e(TAG, "playMediaNow: error adding new queue, look through logs");
            //todo add toast or something
        }
    }

    /**
     * Loads in the next media
     * @param prepareAndPlay
     */
    public void nextMedia(boolean prepareAndPlay) {
        nextMediaHelper(prepareAndPlay, true);
    }

    /**
     * Loads in a new media
     */
    private void nextMediaHelper(boolean prepareAndPlay, boolean removeTrack) {
        if (mediaPlayer != null) {

            //Stop if playing
            if (isPlaying) {
                synchronized (lock) {
                    mediaPlayer.stop();
                }
            }

            isPlaying = false;

            //set is prepared to false
            isPrepared = false;

            APIRecommendations.ItemJSON lastMedia = currentMedia;

            //Remove last track
            if (currentMedia != null && removeTrack) {
                mediaQueueManager.remove(currentMedia);
                //remove item from download
                mediaQueueDownloadManager.removeItem(0, currentMedia);
                //send update
                if (isCompleted) {
                    //if is completed send that
                    sendRatingUpdate(RatingSender.Type.COMPLETED);
                } else {
                    //if not completed but listening
                    sendRatingUpdate(RatingSender.Type.SKIP);
                }
            }

            //Peeks the next track
            currentMedia = mediaQueueManager.peekNextTrack();

            //send update to all observers
            Bundle bundleMediaNext = new Bundle();
            bundleMediaNext.putString(ACTION, Action.MEDIA_NEXT.name());
            bundleMediaNext.putBooleanArray(
                    Action.MEDIA_NEXT.name(),
                    /**
                     * [0] has media
                     * [1] has next media
                     */
                    new boolean[]{
                            hasMedia(),
                            hasNextMedia()
                    }
            );
            if (currentMedia != null) {
                bundleMediaNext.putBoolean(
                        ActionExtras.MEDIA_NEXT_IS_SKIPPABLE.name(),
                        currentMedia.attributes.skippable
                );
            }

            if (lastMedia != null && removeTrack) {
                //if the last track was not null and the a track was removed
                //then send the media next last href as a part of the next
                bundleMediaNext.putString(
                        ActionExtras.MEDIA_NEXT_LAST_MEDIA_HREF.name(),
                        lastMedia.href
                );
            }

            observable.notifyObservers(bundleMediaNext);

            //grab next media if <= 2 songs
            if (mediaQueueManager.queueSize() <= minimalQueueSize) {
                autoLoadNextUpQueue();
            }

            if (currentMedia == null) {
                Log.e(TAG, "nextMedia: failed to peek the next track");
            } else {
                if(!mediaQueueDownloadManager.waitForResponse(
                        0,
                        currentMedia,
                        mediaDownloadedCallback(prepareAndPlay, currentMedia)
                        )) {
                    Log.i(TAG, "nextMediaHelper: already waiting for response!! [" +
                        currentMedia + "]");
                }
            }
        } else {
            Log.d(TAG, "nextMedia: media player is null");
        }
    }

    private MediaQueueDownloadReadyCallback mediaDownloadedCallback(boolean prepareAndPlay,
                                                                      APIRecommendations.ItemJSON itemJSON) {
        return (boolean success, FileInputStream fileInputStream) -> {
            try {
                if (fileInputStream == null) {
                    //send media error to front end ui
                    Bundle bundleMediaError = new Bundle();
                    bundleMediaError.putString(ACTION, Action.MEDIA_ERROR_LOADING.name());
                    bundleMediaError.putString(
                            Action.MEDIA_ERROR_LOADING.name(),
                            getString(R.string.error_media_not_found)
                    );
                    bundleMediaError.putSerializable(
                            ActionExtras.MEDIA_ERROR_LOADING_REMOVE_ITEM.name(),
                            itemJSON
                    );
                    observable.notifyObservers(bundleMediaError);
                    Log.e(TAG, "mediaDownloadedCallback: error fileInputStream not found [" +
                        currentMedia + "]");
                    return;
                }

                //Reset the audio to be able to set to the new track
                synchronized (lock) {
                    mediaPlayer.reset();
                    //set the audio source
                    mediaPlayer.setDataSource(
                            fileInputStream.getFD()
                    );
                    if (prepareAndPlay) {
                        mediaPlayer.prepareAsync();
                        playMedia = true;
                    } else if (currentMedia != null && itemJSON.href.equals(currentMedia.href)) {
                        mediaPlayer.prepareAsync();
                        playMedia = false;
                    }
                }
                Bundle bundleMediaTitle = new Bundle();
                bundleMediaTitle.putString(ACTION, Action.MEDIA_TITLE.name());
                bundleMediaTitle.putString(
                        Action.MEDIA_TITLE.name(),
                        currentMedia.attributes.title
                );
                observable.notifyObservers(bundleMediaTitle);
            } catch (IOException e) {
                Log.e(TAG, "nextMediaHelper: setting data source [" +
                        currentMedia + "]", e);
            }
        };
    }

    private void autoLoadNextUpQueue() {
        if (!loadingLineUp) {
            loadingLineUp = true;
            APIRecommendations recommendations = new APIRecommendations(
                    this,
                    APIRecommendations.DEFAULT_RECOMMENDATIONS_URL
            );
            APIDataResponse recommendationsApiDataResponse = () -> {
                try {
                    RecommendationCache recommendationsData = recommendations.getData();
                    if (recommendationsData == null || recommendationsData.data == null) {
                        Log.e(TAG, "recommendationsApiDataResponse: APIRecommendations data is null");
                        //TODO: Add error to screen or something
                    } else {
                        Log.d(TAG, "recommendationsApiDataResponse: APIRecommendations got data!");
                        APIRecommendations.RecommendationsJSON recommendationsJSON = recommendationsData.data;
                        if (recommendationsJSON.items != null) {
                            for (APIRecommendations.ItemJSON item : recommendationsJSON.items) {
                                //Check if had links for audio and image
                                if (item.links != null &&
                                        item.links.audio != null && item.links.audio.size() > 0 &&
                                        item.links.image != null && item.links.image.size() > 0) {
                                    // add the first possible valid audio type
                                    APIRecommendations.AudioJSON audioJSON = Util.getAudioJSON(item.links.audio);
                                    if (audioJSON != null) {
                                        BackgroundAudioService.this.addToQueue(item);
                                    }
                                }
                            }
                        } else {
                            Log.w(TAG, "loadLineUpQueue: No items found for loadLinUpQueue data.");
                        }
                    }
                } finally {
                    loadingLineUp = false;
                }
            };
            recommendations.updateData(recommendationsApiDataResponse);
        }
    }

    /**
     * Plays the current media if exists
     */
    public void playMedia() {
        if (mediaPlayer != null) {
            isPlaying = false;
            if (isPrepared) {
                requestAudioFocusAndPlay();
            } else {
                synchronized (lock) {
                    mediaPlayer.prepareAsync();
                }
            }
        } else {
            Log.d(TAG, "playMedia: media player is null");
        }
    }

    private void requestAudioFocusAndPlay() {
        // Request audio focus for playback
        int result = audioManager.requestAudioFocus(this,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);
        switch (result) {
            case AudioManager.AUDIOFOCUS_REQUEST_GRANTED:
                synchronized (lock) {
                    mediaPlayer.start();
                }
                isPlaying = true;
                Bundle bundle = new Bundle();
                bundle.putString(ACTION, Action.PAUSE_BUTTON.name());
                observable.notifyObservers(bundle);
                break;
            default:
                Log.e(TAG, "requestAudioFocusAndPlay: error requesting focus[" + result + "]");
                //TODO some type of visual error
        }
    }

    /**
     * Pauses the current media if exists
     */
    public void pauseMedia() {
        if (mediaPlayer != null) {
            synchronized (lock) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    isPlaying = false;
                }
            }
            Bundle bundle = new Bundle();
            bundle.putString(ACTION, Action.PLAY_BUTTON.name());
            observable.notifyObservers(bundle);
        } else {
            Log.d(TAG, "pauseMedia: media player is null");
        }
    }

    /**
     * Stops the current media if exists
     */
    public void stopAndReleaseMedia() {
        if (mediaPlayer != null) {
            synchronized (lock) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    isPlaying = false;
                }
                mediaPlayer.stop();
                isPrepared = false;
                mediaPlayer.release();
                currentMedia = null;
            }
        } else {
            Log.d(TAG, "stopAndReleaseMedia: media player is null");
        }
    }

    public @NonNull String getMediaTitle() {
        String mediaTitle = getString(R.string.nothing_to_play);
        if (currentMedia != null) {
            mediaTitle = currentMedia.attributes.title;
            if (mediaTitle == null || mediaTitle.equals("")) {
                mediaTitle = getString(R.string.nothing_to_play);
            }
        }
        return mediaTitle;
    }

    public @NonNull String getMediaDescription() {
        String mediaDesc = "";
        if (currentMedia != null) {
            mediaDesc = currentMedia.attributes.description;
        }
        return mediaDesc;
    }

    public @NonNull String getMediaHref() {
        String itemJSONHref = "";
        if (currentMedia != null) {
            itemJSONHref = currentMedia.href;
        }
        return itemJSONHref;
    }

    public boolean getMediaIsSkippable() {
        boolean skippable = true;
        if (currentMedia != null) {
            skippable = currentMedia.attributes.skippable;
        }
        return skippable;
    }


    public int getMediaDuration() {
        if (isPrepared) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    public int getMediaCurrentPosition() {
        if (isPrepared) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public @NonNull Shared.ImageJSON getMediaImage() {
        Shared.ImageJSON imageJSON;
        if (currentMedia != null && currentMedia.links.hasImage()) {
            imageJSON = currentMedia.links.getValidImage();
        } else {
            imageJSON = new Shared.ImageJSON();
        }
        return imageJSON;
    }

    public void seekMedia(int millisecond) {
        if (mediaPlayer != null && isPrepared) {
            int seekVal = millisecond;
            if (seekVal < 0 ) { seekVal = 0; }
            synchronized (lock) {
                mediaPlayer.seekTo(seekVal);
            }
        }
    }

    public boolean getIsPlaying() {
        return isPlaying;
    }

    public boolean addToQueue(APIRecommendations.ItemJSON queueItem) {
        if (!mediaQueueManager.addToQueue(queueItem)) {
            return false;
        }

        if (mediaQueueManager.queueSize() == 1) {
            //adds prepare and play if list is empty
            nextMediaHelper(false, false);
        } else {
            //just starts download by adding to the end of the queue
            mediaQueueDownloadManager.addItem(mediaQueueManager.queueSize()-1, queueItem);
        }

        Bundle bundleMediaTitle = new Bundle();
        bundleMediaTitle.putString(ACTION, Action.MEDIA_ADDED_TO_QUEUE.name());
        bundleMediaTitle.putBooleanArray(
                Action.MEDIA_ADDED_TO_QUEUE.name(),
                new boolean[] {
                        hasMedia(),
                        hasNextMedia()
                }
        );
        observable.notifyObservers(bundleMediaTitle);
        return true;
    }


    /**
     * Returns if it has a next media to play after current
     * @return
     */
    public boolean hasNextMedia() {
        return mediaQueueManager.queueSize() > 1;
    }

    /**
     * Returns if it has a media to play
     * @return
     */
    public boolean hasMedia() {
        return mediaQueueManager.queueSize() > 0;
    }

    /**
     * Used by main ui thread for updates
     * @param observer
     */
    public void addObserver(Observer observer) {
        observable.addObserver(observer);
    }

    /**
     * Remove for updates
     * @param observer
     */
    public void removeObserver(Observer observer) {
        observable.deleteObserver(observer);
    }

    private void startSeekUpdateThread() {
        //TODO maybe not use thread? but have observer for each second to update?
        new Thread(() -> {
            int lastSeek = getMediaCurrentPosition()/1000;
            int currentSeek;
            while(true) {
                currentSeek = getMediaCurrentPosition();
                if (currentSeek/1000 != lastSeek) {
                    lastSeek = currentSeek/1000;
                    Bundle bundleMediaTitle = new Bundle();
                    bundleMediaTitle.putString(ACTION, Action.SEEK_CHANGE.name());
                    bundleMediaTitle.putInt(
                            Action.SEEK_CHANGE.name(),
                            currentSeek
                    );
                    observable.notifyObservers(bundleMediaTitle);
                    //update elapsed
                    currentMedia.attributes.rating.elapsed = currentSeek/1000;
                }
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    Log.e(TAG, "onProgressChanged: failed to sleep", e);
                }
            }
        }).start();
    }

    /**
     * Sends a rating update
     */
    public void sendRatingUpdate(RatingSender.Type type) {
        if (currentMedia != null) {
            //set the current media information
            if (type.equals(RatingSender.Type.COMPLETED)) {
                currentMedia.attributes.rating.rating = type.name();
                currentMedia.attributes.rating.elapsed =
                        currentMedia.attributes.rating.duration;
            }
            //Note: the elapsed is continuously set by background thread

            //send update
            if (mediaQueueManager.queueSize() <= minimalQueueSize+1
                    && currentMedia.attributes.rating != null) {
                //if less then request recommendation plus one
                //TODO may want to specify more options: https://dev.npr.org/guide/services/listening/#Ratings
                new RatingSender(
                        RatingSender.BASE_URL + "?recommend=true",
                        true,
                        currentMedia.attributes.rating,
                        settingsAndTokenManager.getToken(),
                        BackgroundAudioService.this
                ).sendAsyncRating();
            } else {
                //do not request recommendation
                new RatingSender(
                        RatingSender.BASE_URL + "?recommend=false",
                        false,
                        currentMedia.attributes.rating,
                        settingsAndTokenManager.getToken(),
                        BackgroundAudioService.this
                ).sendAsyncRating();
            }
        }
    }
}
