package com.nprcommunity.npronecommunity.Background;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.nprcommunity.npronecommunity.API.APIDataResponse;
import com.nprcommunity.npronecommunity.API.APIRecommendations;
import com.nprcommunity.npronecommunity.API.RatingSender;
import com.nprcommunity.npronecommunity.API.Shared;
import com.nprcommunity.npronecommunity.Background.Queue.LineUpQueue;
import com.nprcommunity.npronecommunity.Navigate;
import com.nprcommunity.npronecommunity.R;
import com.nprcommunity.npronecommunity.Store.CacheStructures.RecommendationCache;
import com.nprcommunity.npronecommunity.Store.DownloadPoolExecutor;
import com.nprcommunity.npronecommunity.Store.FileCache;
import com.nprcommunity.npronecommunity.Store.ProgressCallback;
import com.nprcommunity.npronecommunity.Store.SettingsAndTokenManager;
import com.nprcommunity.npronecommunity.Util;
import com.orhanobut.hawk.Hawk;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static com.nprcommunity.npronecommunity.Background.BackgroundAudioService.CommandCompat.ADD_ITEM;
import static com.nprcommunity.npronecommunity.Background.BackgroundAudioService.CommandCompat.PLAY_MEDIA_NOW;
import static com.nprcommunity.npronecommunity.Background.BackgroundAudioService.CommandCompat.REMOVE_INDEX;
import static com.nprcommunity.npronecommunity.Background.BackgroundAudioService.CommandCompat.REMOVE_ITEM;
import static com.nprcommunity.npronecommunity.Background.BackgroundAudioService.CommandCompat.SEND_RATING_TIMEOUT;
import static com.nprcommunity.npronecommunity.Background.BackgroundAudioService.CommandCompat.SWAP;
import static com.nprcommunity.npronecommunity.Background.BackgroundAudioService.CommandCompat.THUMBS_UP_RATING;
import static com.nprcommunity.npronecommunity.Background.BackgroundAudioService.CommandCompatExtras.PLAY_MEDIA_NOW_QUEUE_ITEM;
import static com.nprcommunity.npronecommunity.Store.DownloadPoolExecutor.MediaType.AUDIO;
import static com.nprcommunity.npronecommunity.Store.DownloadPoolExecutor.MediaType.IMAGE;

/**
 * Thanks to  Paul Trebilcox-Ruiz tutorial for helping with all the media compat android stuff.
 * The tutorial can be found at the following link:
 * https://code.tutsplus.com/tutorials/background-audio-in-android-with-mediasessioncompat--cms-27030
 */
public class BackgroundAudioService extends MediaBrowserServiceCompat implements
        MediaPlayer.OnCompletionListener,
        AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener {

    public static final String METADATA_KEY_IMAGE_HREF = "METADATA_KEY_IMAGE_HREF";
    private MediaSessionCompat mediaSessionCompat;
    private final String TAG = "BACKGROUNDAUDIOSERVICE";
    private MediaPlayer mediaPlayer;
    private MediaQueueManager mediaQueueManager;
    private APIRecommendations.ItemJSON currentMedia;
    private FileCache fileCache;
    private AudioManager audioManager;
    private SettingsAndTokenManager settingsAndTokenManager;
    private boolean isPrepared = false,
            isPlaying = false,
            wasPlayingBeforeLostFocus = false,
            playMedia = true,
            isCompleted = false;
    private static final Object lock = new Object();
    private volatile boolean loadingLineUp = false;
    private boolean foregroundStarted = false;

    private static final int NOTIFICATION_ID = 1526;

    private Thread seekThread = null;

    private final int MAX_AUDIO_LEVEL = 100;

    public static final String ACTION = "ACTION";

    private int minimalQueueSize = 2;

    private BroadcastReceiver noisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            pauseMedia();
        }
    };
    private MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onPlay() {
            super.onPlay();
            playMedia();
        }

        @Override
        public void onPause() {
            super.onPause();
            pauseMedia();
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
            throw new Error("onPlayFromMediaId(mediaId, extras) should not be called");
        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
            seekMedia((int)pos);
        }

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            super.onCommand(command, extras, cb);
            if (SEND_RATING_TIMEOUT.name().equals(command)) {
                sendRatingUpdate(RatingSender.Type.TIMEOUT);
            } else if (SWAP.name().equals(command)) {
                swap(
                        extras.getInt(CommandCompatExtras.SWAP_POS_ONE.name()),
                        extras.getInt(CommandCompatExtras.SWAP_POS_TWO.name())
                );
            } else if (PLAY_MEDIA_NOW.name().equals(command)) {
                playMediaNow(
                        (APIRecommendations.ItemJSON) extras.getSerializable(
                                PLAY_MEDIA_NOW_QUEUE_ITEM.name()
                        )
                );
            } else if (REMOVE_INDEX.name().equals(command)) {
                remove(extras.getInt(CommandCompatExtras.REMOVE_INDEX_I.name()));
            } else if (REMOVE_ITEM.name().equals(command)) {
                int removed = remove((APIRecommendations.ItemJSON) extras.getSerializable(
                        CommandCompatExtras.REMOVE_ITEM_OBJECT.name()
                    )
                );
                Bundle bundleRemoved = new Bundle();
                bundleRemoved.putString(ACTION, Action.MEDIA_REMOVED.name());
                bundleRemoved.putInt(BackgroundAudioService.ActionExtras.MEDIA_REMOVED_I.name(),
                        removed);
                mediaSessionCompat.sendSessionEvent(
                        BackgroundAudioService.Action.MEDIA_REMOVED.name(),
                        bundleRemoved
                );
            } else if (ADD_ITEM.name().equals(command)) {
                addToQueue((APIRecommendations.ItemJSON) extras.getSerializable(
                        CommandCompatExtras.ADD_ITEM_OBJECT.name()
                        )
                );
            } else if (THUMBS_UP_RATING.name().equals(command)) {
                sendRatingUpdate(RatingSender.Type.THUMBUP);
            }
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            nextMedia(true);
        }

        /**
         * Remove should never be called. Other functions need to
         * be handled through a custom command.
         * @param description
         */
        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            super.onRemoveQueueItem(description);
            throw new Error("onRemoveQueueItem(description) should not be called");
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description) {
            super.onAddQueueItem(description);
            throw new Error("onAddQueueItem(description) should not be called");
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description, int index) {
            super.onAddQueueItem(description, index);
            throw new Error("onAddQueueItem(description, index) should not be called");
        }

        @Override
        public void onFastForward() {
            super.onFastForward();
            mediaQueueManager.forceSaveQueue();
        }

        @Override
        public void onRewind() {
            super.onRewind();
            seekMedia(getMediaCurrentPosition() - (int)(10*Util.MILLI_SECOND));
        }

        @Override
        public void onStop() {
            super.onStop();
            pauseMedia();
            mediaQueueManager.forceSaveQueue();
            stopForeground(true);
            BackgroundAudioService.this.stopSelf();
        }
    };

    /**
     * Actions used for cross communication between frontend
     */
    public enum Action {
        MEDIA_ADDED_TO_QUEUE,
        MEDIA_DOWNLOADING_PROGRESS,
        MEDIA_ERROR_LOADING,
        MEDIA_COMPLETE,
        SEEK_CHANGE,
        MEDIA_REMOVED
    }

    /**
     * Extra information used in the Bundles for Action
     */
    public enum ActionExtras {
        MEDIA_ERROR_LOADING_REMOVE_ITEM,
        MEDIA_NEXT_IS_SKIPPABLE,
        MEDIA_PREPARED_HREF,
        MEDIA_REMOVED_I,
        MEDIA_DOWNLOADING_PROGRESS_SPEED,
        MEDIA_DOWNLOADING_PROGRESS_PERCENTAGE,
        MEDIA_DOWNLOADING_PROGRESS_TYPE,
    }

    public enum CommandCompat {
        SEND_RATING_TIMEOUT,
        SWAP,
        PLAY_MEDIA_NOW,
        REMOVE_INDEX,
        REMOVE_ITEM,
        ADD_ITEM,
        THUMBS_UP_RATING
    }

    public enum CommandCompatExtras {
        SWAP_POS_ONE,
        SWAP_POS_TWO,
        PLAY_MEDIA_NOW_QUEUE_ITEM,
        REMOVE_INDEX_I,
        REMOVE_ITEM_OBJECT,
        ADD_ITEM_OBJECT
    }

    /**
     * Simple constructor that does nothing but call super :)
     */
    public BackgroundAudioService() {
        super();
    }

    @Nullable
    @Override
    public MediaBrowserServiceCompat.BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        if(TextUtils.equals(clientPackageName, getPackageName())) {
            return new MediaBrowserServiceCompat.BrowserRoot(getString(R.string.app_name), null);
        }
        return null;
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(null);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost audio permanently
                // losing permanently, set was playing to false no matter what.
                wasPlayingBeforeLostFocus = false;

                this.pauseMedia();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // PAUSE playback
                // set last playback to whether or not music was playing
                wasPlayingBeforeLostFocus = isPlaying;

                this.pauseMedia();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lower the volume, keep playing
                float quarterVolume = getVolume(MAX_AUDIO_LEVEL/4);
                synchronized (lock) {
                    mediaPlayer.setVolume(quarterVolume, quarterVolume);
                }
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
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
                break;
        }
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
            Bundle bundleComplete = new Bundle();
            bundleComplete.putString(ACTION, Action.MEDIA_COMPLETE.name());
            mediaSessionCompat.sendSessionEvent(
                    BackgroundAudioService.Action.MEDIA_COMPLETE.name(),
                    bundleComplete
            );
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (seekThread != null && seekThread.isAlive()) {
            seekThread.interrupt();
        }
        mediaQueueManager.forceSaveQueue();
        setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED);
        unregisterReceiver(noisyReceiver);
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

        DownloadPoolExecutor.getInstance(IMAGE).forceShutdown();
        DownloadPoolExecutor.getInstance(AUDIO).forceShutdown();

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(this);
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSessionCompat, intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
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
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        //set volume to max for this app
        float maxAudio = getVolume(MAX_AUDIO_LEVEL);
        mediaPlayer.setVolume(maxAudio, maxAudio);

        //TODO: Might need a wifi lock as well
        fileCache = FileCache.getInstances(this);
        mediaQueueManager = MediaQueueManager.getInstance(this);

        //TODO in the future use the new standard for audio manager
        //for reference https://developer.android.com/guide/topics/media-apps/audio-focus
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        settingsAndTokenManager = new SettingsAndTokenManager(this);

        // init media Compat
        ComponentName mediaButtonReceiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        mediaSessionCompat = new MediaSessionCompat(getApplicationContext(), "Tag", mediaButtonReceiver, null);

        mediaSessionCompat.setCallback(mediaSessionCallback);
        mediaSessionCompat.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS |
                MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS);

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
        mediaSessionCompat.setMediaButtonReceiver(pendingIntent);
        mediaSessionCompat.setRatingType(RatingCompat.RATING_NONE);
        mediaSessionCompat.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE);
        mediaSessionCompat.setQueue(mediaQueueManager.getMediaQueue());

        MediaSessionCompat.Token token = mediaSessionCompat.getSessionToken();
        setSessionToken(token);

        //Handles headphones coming unplugged. cannot be done through a manifest receiver
        IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(noisyReceiver, filter);

        // starts download of all items in queue not already downloaded
        setupQueueDownloads();

        nextMedia(false);

        setMediaSessionMetadata();

        startSeekUpdateThread();
    }

    private void setMediaPlaybackState(int state) {
        float playbackSpeed = 0f;
        long next = hasNextMedia() ? PlaybackStateCompat.ACTION_SKIP_TO_NEXT : 0;
        PlaybackStateCompat.Builder playbackstateBuilder = new PlaybackStateCompat.Builder();
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            playbackSpeed = 1.0f;
            playbackstateBuilder.setActions(
                    PlaybackStateCompat.ACTION_PLAY_PAUSE |
                    PlaybackStateCompat.ACTION_PAUSE |
                    PlaybackStateCompat.ACTION_REWIND |
                    PlaybackStateCompat.ACTION_STOP |
                    next
            );
        } else if (state == PlaybackStateCompat.STATE_PAUSED){
            playbackSpeed = 0f;
            playbackstateBuilder.setActions(
                    PlaybackStateCompat.ACTION_PLAY_PAUSE |
                    PlaybackStateCompat.ACTION_PLAY |
                    PlaybackStateCompat.ACTION_REWIND |
                    PlaybackStateCompat.ACTION_STOP |
                    next
            );
        } else if (state == PlaybackStateCompat.STATE_BUFFERING) {
            playbackSpeed = 0f;
            playbackstateBuilder.setActions(
                    PlaybackStateCompat.ACTION_STOP |
                    next
            );
        } else if (state == PlaybackStateCompat.STATE_SKIPPING_TO_NEXT) {
            playbackSpeed = 0f;
            playbackstateBuilder.setActions(
                    PlaybackStateCompat.ACTION_STOP |
                    next
            );
        } else if (state == PlaybackStateCompat.STATE_REWINDING) {
            playbackSpeed = -1.0f;
            playbackstateBuilder.setActions(
                    PlaybackStateCompat.ACTION_REWIND |
                    PlaybackStateCompat.ACTION_STOP |
                    next
            );
        }

        Bundle bundle = new Bundle();
        playbackstateBuilder.setExtras(bundle);
        playbackstateBuilder.setState(state,
                currentMedia == null ? 0 : currentMedia.attributes.rating.elapsed.get() * 1000,
                playbackSpeed
        );
        mediaSessionCompat.setPlaybackState(playbackstateBuilder.build());
    }

    private void showBufferingNotification() {
        MediaStyleHelper mediaStyleHelper = new MediaStyleHelper();
        NotificationCompat.Builder builder = mediaStyleHelper.from(mediaSessionCompat);
        if (builder == null) {
            return;
        }

        builder.addAction(new NotificationCompat.Action(
                        R.drawable.ic_replay_10_white_40dp,
                        getString(R.string.rewind),
                        null
                )
        );
        builder.addAction(new NotificationCompat.Action(
                        R.drawable.ic_more_horiz_white_24dp,
                        getString(R.string.buffering),
                        null
                )
        );
        if (hasNextMedia()) {
            builder.addAction(new NotificationCompat.Action(
                            R.drawable.ic_skip_next_white_24dp,
                            getString(R.string.next),
                            MediaButtonReceiver.buildMediaButtonPendingIntent(
                                    this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                            )
                    )
            );
        }
        builder.addAction(new NotificationCompat.Action(
                        R.drawable.ic_close_white_24dp,
                        getString(R.string.close),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                this, PlaybackStateCompat.ACTION_STOP
                        )
                )
        );
        builder.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle().
                setShowActionsInCompactView(1).setMediaSession(mediaSessionCompat.getSessionToken()));
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setOngoing(true);

        Intent notificationIntent = new Intent(getApplicationContext(), Navigate.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 0,
                notificationIntent, 0);
        builder.setContentIntent(intent);
        if (!foregroundStarted) {
            startForeground(NOTIFICATION_ID, builder.build());
            foregroundStarted = true;
        } else {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build());
        }
    }

    private void showPlayingNotification() {
        MediaStyleHelper mediaStyleHelper = new MediaStyleHelper();
        NotificationCompat.Builder builder = mediaStyleHelper.from(mediaSessionCompat);
        if (builder == null) {
            return;
        }

        builder.addAction(new NotificationCompat.Action(
                        R.drawable.ic_replay_10_white_40dp,
                        getString(R.string.rewind),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                // NOTE: Rewind rewinds 10 seconds
                                // intent only supports certain actions, check out docs
                                this, PlaybackStateCompat.ACTION_REWIND
                        )
                )
        );
        builder.addAction(new NotificationCompat.Action(
                R.drawable.ic_pause_white_24dp,
                getString(R.string.pause),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this, PlaybackStateCompat.ACTION_PLAY_PAUSE
                )
            )
        );
        if (hasNextMedia()) {
            builder.addAction(new NotificationCompat.Action(
                            R.drawable.ic_skip_next_white_24dp,
                            getString(R.string.next),
                            MediaButtonReceiver.buildMediaButtonPendingIntent(
                                    this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                            )
                    )
            );
        }
        builder.addAction(new NotificationCompat.Action(
                        R.drawable.ic_close_white_24dp,
                        getString(R.string.close),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                this, PlaybackStateCompat.ACTION_STOP
                        )
                )
        );
        builder.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle().
                setShowActionsInCompactView(1).setMediaSession(mediaSessionCompat.getSessionToken()));
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setOngoing(true);

        Intent notificationIntent = new Intent(getApplicationContext(), Navigate.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 0,
                notificationIntent, 0);
        builder.setContentIntent(intent);
        if (!foregroundStarted) {
            startForeground(NOTIFICATION_ID, builder.build());
            foregroundStarted = true;
        } else {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build());
        }
    }

    private void showPausedNotification() {
        MediaStyleHelper mediaStyleHelper = new MediaStyleHelper();
        NotificationCompat.Builder builder = mediaStyleHelper.from(mediaSessionCompat);
        if (builder == null) {
            return;
        }

        builder.addAction(new NotificationCompat.Action(
                        R.drawable.ic_replay_10_white_40dp,
                        getString(R.string.rewind),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                this, PlaybackStateCompat.ACTION_REWIND
                        )
                )
        );
        builder.addAction(new NotificationCompat.Action(
                        R.drawable.ic_play_arrow_white_24dp,
                        getString(R.string.pause),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                this, PlaybackStateCompat.ACTION_PLAY_PAUSE
                        )
                )
        );
        if (hasNextMedia()) {
            builder.addAction(new NotificationCompat.Action(
                            R.drawable.ic_skip_next_white_24dp,
                            getString(R.string.next),
                            MediaButtonReceiver.buildMediaButtonPendingIntent(
                                    this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                            )
                    )
            );
        }
        builder.addAction(new NotificationCompat.Action(
                        R.drawable.ic_close_white_24dp,
                        getString(R.string.close),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                this, PlaybackStateCompat.ACTION_STOP
                        )
                )
        );
        builder.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1).setMediaSession(mediaSessionCompat.getSessionToken()));
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setOngoing(false);

        Intent notificationIntent = new Intent(getApplicationContext(), Navigate.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 0,
                notificationIntent, 0);
        builder.setContentIntent(intent);
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build());
    }

    /**
     * Sets the media session metadata based off of the current media that is playing
     */
    private void setMediaSessionMetadata() {
        // set the queue
        mediaSessionCompat.setQueue(mediaQueueManager.getMediaQueue());

        // update metadata
        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();

        if (currentMedia != null) {
            //Notification icon in card

            // load in actual image
            Bitmap displayImage = fileCache.getImageSync(currentMedia.links.getValidImage().href);
            if (displayImage == null) {
                displayImage = BitmapFactory.decodeResource(getResources(), R.drawable.if_radio_scaled_600);
            }

            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, displayImage);
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, displayImage);
            //lock screen icon for pre lollipop
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, displayImage);
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE,
                    currentMedia.attributes.audioTitle);
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                    currentMedia.attributes.audioTitle);
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,
                    currentMedia.attributes.title);
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM,
                    currentMedia.attributes.album);
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_AUTHOR,
                    currentMedia.attributes.program);
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                    currentMedia.attributes.program);
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST,
                    currentMedia.attributes.program);
            metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 1);
            metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, 1);
            metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getMediaDuration());
            metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_ADVERTISEMENT,
                    isMediaSkippable() ? 1 : 0);
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                    currentMedia.href);
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION,
                    currentMedia.attributes.description);
            metadataBuilder.putString(METADATA_KEY_IMAGE_HREF, getMediaImage().href);
        } else {
            //Notification icon in card
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON,
                    BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                    BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
            //lock screen icon for pre lollipop
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART,
                    BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
            String unknown = "YES";//getString(R.string.unknown);
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE,
                    unknown);
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                    unknown);
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,
                    unknown);
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM,
                    unknown);
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_AUTHOR,
                    unknown);
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                    unknown);
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST,
                    unknown);
            metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 1);
            metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, 1);
            metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getMediaDuration());
            metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_ADVERTISEMENT, 0);
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "");
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION,
                    unknown);
            metadataBuilder.putString(METADATA_KEY_IMAGE_HREF, null);
        }

        mediaSessionCompat.setMetadata(metadataBuilder.build());
    }

    public class MediaStyleHelper {
        public NotificationCompat.Builder from(MediaSessionCompat mediaSession) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("default",
                        getString(R.string.app_name),
                        NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription(getString(R.string.app_name));
                ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE))
                        .createNotificationChannel(channel);
            }
            MediaControllerCompat controllerCompat = mediaSession.getController();
            MediaDescriptionCompat description = controllerCompat.getMetadata().getDescription();

            return new NotificationCompat.Builder(getApplicationContext(), "default")
                    .setContentTitle(description.getTitle())
                    .setContentIntent(controllerCompat.getSessionActivity())
                    .setAutoCancel(false)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }
    }


    /**
     * Seeks to a given part in the media. Is safegaurded about going past the end and going
     * negative.
     * @param millisecond
     */
    private void seekMedia(int millisecond) {
        if (mediaPlayer != null && isPrepared) {
            int seekVal = millisecond;
            if (seekVal < 0 ) { seekVal = 0; }
            else if (seekVal > mediaPlayer.getDuration()) {
                seekVal = mediaPlayer.getDuration();
            }
            synchronized (lock) {
                mediaPlayer.seekTo(seekVal);
            }
        }
        currentMedia.attributes.rating.elapsed.getAndSet(millisecond/1000);
        mediaQueueManager.forceSaveQueue();
    }

    /**
     * Pauses the current media if it exits. This will also update the notification and
     * PlaybackStateCompat
     */
    public void pauseMedia() {
        if (mediaPlayer != null) {
            synchronized (lock) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    isPlaying = false;
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                    showPausedNotification();
                }
            }
        } else {
            Log.d(TAG, "pauseMedia: media player is null");
        }
    }

    /**
     * Plays the current media if exists. Calls requestAudioFocusAndPlay(), which sets up the
     * proper information for an audio track that is already loaded into the media player.
     * If the audio track is not loaded yet it will tell the media player to load the track async,
     * the media players async has a callback OnPreparedListener which will handle the playback
     * once loaded.
     */
    private void playMedia() {
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

    /**
     * Method requests audio focus and then starts to play music if gained. Will also set
     * PlaybackState properly to be used by other resources.
     *
     * @return true if successfully started to play music, false else
     */
    private boolean requestAudioFocusAndPlay() {
        // Request audio focus for playback
        int result = audioManager.requestAudioFocus(this,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);
        switch (result) {
            case AudioManager.AUDIOFOCUS_REQUEST_GRANTED:
                // set media session metadata again
                setMediaSessionMetadata();

                mediaSessionCompat.setActive(true);
                synchronized (lock) {
                    mediaPlayer.start();
                }
                isPlaying = true;
                setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                showPlayingNotification();
                break;
            default:
                Log.e(TAG, "requestAudioFocusAndPlay: error requesting focus[" + result + "]");
                return false;
        }
        return true;
    }

    /**
     * Swaps the one position for another position in the media queue
     *
     * @param fromPosition the start position to swap
     * @param toPosition the end position to swap against
     */
    private void swap(int fromPosition, int toPosition) {
        mediaQueueManager.swap(fromPosition, toPosition);
        if (toPosition == 0) {
            nextMediaHelper(true, false);
        } else if (fromPosition == 0) {
            nextMediaHelper(true, false);
        }
    }

    /**
     * Removes a given track from the queue
     *
     * @param position index of queue item to remove
     */
    private void remove(int position) {

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

        downloadStopAndRemove(mediaQueueManager.getAPIQueueTrack(position));
        mediaQueueManager.remove(position);
        if (position == 0) {
            nextMediaHelper(true, false);
        }
        if (mediaQueueManager.queueSize() <= minimalQueueSize) {
            autoLoadNextUpQueue();
        }
    }

    /**
     * Removes an item
     * @param itemJSON
     * @return
     */
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

    @Override
    public void onPrepared(MediaPlayer mp) {
        isPrepared = true;
        isCompleted = false;

        //update the position, if previously saved
        if (currentMedia != null) {
            seekMedia(currentMedia.attributes.rating.elapsed.get() * 1000);
        }

        //if the play media is not set, then do not attempt to play media...
        if (!playMedia) {
            //call to update frontend and proper media features
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        } else {
            //play the media
            playMedia();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "onError: [" + what + "] [" + extra + "]");
        mp.reset();
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        currentMedia.attributes.rating.elapsed.getAndSet(mp.getCurrentPosition()/1000);
        if (isPlaying) {
            setMediaPlaybackState(PlaybackState.STATE_PLAYING);
        } else {
            setMediaPlaybackState(PlaybackState.STATE_PAUSED);
        }
    }

    private float getVolume(int level) {
        if (level > MAX_AUDIO_LEVEL) {
            level = MAX_AUDIO_LEVEL;
        }
        final float max = (float) 1.0;
        return max-((float)(Math.log(MAX_AUDIO_LEVEL-level)/Math.log(MAX_AUDIO_LEVEL)));
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

            //Remove last track
            if (currentMedia != null && removeTrack) {
                mediaQueueManager.remove(currentMedia);
                //remove item from download
                downloadStopAndRemove(currentMedia);
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

            //sets the metadata for this next track
            setMediaSessionMetadata();
            if (foregroundStarted) {
                showBufferingNotification();
            }
            setMediaPlaybackState(PlaybackState.STATE_SKIPPING_TO_NEXT);

            //grab next media if <= 2 songs
            if (mediaQueueManager.queueSize() <= minimalQueueSize) {
                autoLoadNextUpQueue();
            }

            if (currentMedia == null) {
                Log.e(TAG, "nextMedia: failed to peek the next track");
            } else {
                downloadAndPlayItem(currentMedia, prepareAndPlay);
            }
        } else {
            Log.d(TAG, "nextMedia: media player is null");
        }
    }

    private void downloadAndPlayItem(APIRecommendations.ItemJSON itemJSON, boolean prepareAndPlay) {
        APIRecommendations.AudioJSON audioJSON = itemJSON.links.getValidAudio();
        ProgressCallback progressCallback = (progress, total, speed, progressCallbackType) -> {
            //this is progress for audio loading
            Log.d(TAG, "startDownload: progress loading audio: "
                    + audioJSON.href + " at "
                    + " progress [" + progress + "] total [" + total + "] "
                    + " percent [" + ((double)progress)/((double)total)
                    + " progressCallbackType [" + progressCallbackType.name() + "]"
            );
            audioJSON.progressTracker.setProgress(progress);
            audioJSON.progressTracker.setTotal(total);

            //force save
            mediaQueueManager.forceSaveQueue();

            Bundle bundleMediaProgress = new Bundle();
            bundleMediaProgress.putString(
                    ACTION,
                    BackgroundAudioService.Action.MEDIA_DOWNLOADING_PROGRESS.name()
            );
            double percentage = audioJSON.progressTracker.getPercentage();
            bundleMediaProgress.putInt(
                    ActionExtras.MEDIA_DOWNLOADING_PROGRESS_SPEED.name(),
                    speed //in nanoseconds
            );
            bundleMediaProgress.putInt(
                    ActionExtras.MEDIA_DOWNLOADING_PROGRESS_PERCENTAGE.name(),
                    (int)(percentage*100) //get percentage
            );
            bundleMediaProgress.putString(
                    ActionExtras.MEDIA_DOWNLOADING_PROGRESS_TYPE.name(),
                    progressCallbackType.name()
            );
            bundleMediaProgress.putString(
                    BackgroundAudioService.ActionExtras.MEDIA_PREPARED_HREF.name(),
                    itemJSON.href
            );
            mediaSessionCompat.sendSessionEvent(
                    BackgroundAudioService.Action.MEDIA_DOWNLOADING_PROGRESS.name(),
                    bundleMediaProgress
            );
        };
        fileCache.getAudio(itemJSON,
                progressCallback,
                (FileInputStream fileInputStream, String url) -> {
                    if (fileInputStream == null) {
                        //todo todo todo set return error type if failed
                        //something failed set full downloaded to false
                        audioJSON.progressTracker.setIsFullyDownloaded(false);
                        Bundle bundleMediaError = new Bundle();
                        bundleMediaError.putString(
                                ACTION, BackgroundAudioService.Action.MEDIA_ERROR_LOADING.name()
                        );
                        bundleMediaError.putString(
                                BackgroundAudioService.Action.MEDIA_ERROR_LOADING.name(),
                                getApplicationContext().getString(R.string.error_media_not_found)
                        );
                        bundleMediaError.putSerializable(
                                BackgroundAudioService.ActionExtras.MEDIA_ERROR_LOADING_REMOVE_ITEM.name(),
                                itemJSON
                        );
                        mediaSessionCompat.sendSessionEvent(
                                BackgroundAudioService.Action.MEDIA_ERROR_LOADING.name(),
                                bundleMediaError
                        );
                    } else {
                        //success set the fully downloaded and file input stream

                        //update progress to complete
                        //doesnt matter, download is complete successfully notify frontend
                        progressCallback.updateProgress(1, 1, 0, ProgressCallback.Type.COMPLETE);

                        //set fully downloaded
                        audioJSON.progressTracker.setIsFullyDownloaded(true);

                        //Reset the audio to be able to set to the new track
                        synchronized (lock) {
                            mediaPlayer.reset();
                            //set the audio source
                            try {
                                mediaPlayer.setDataSource(fileInputStream.getFD());
                                if (prepareAndPlay) {
                                    playMedia = true;
                                    currentMedia = itemJSON;
                                    setMediaSessionMetadata();
                                    setMediaPlaybackState(PlaybackState.STATE_BUFFERING);
                                    mediaPlayer.prepareAsync();
                                } else if (currentMedia != null && itemJSON.href.equals(currentMedia.href)) {
                                    playMedia = false;
                                    setMediaSessionMetadata();
                                    setMediaPlaybackState(PlaybackState.STATE_BUFFERING);
                                    mediaPlayer.prepareAsync();
                                }
                            } catch (IOException e) {
                                //failed to play audio
                                Log.e(TAG, "mediaDownloadSuccess: failed to play audio", e);
                                Toast.makeText(this, R.string.error_playing, Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                }
        );
    }

    /**
     * Called once at init. Starts downloads of all items in queue that are not fully
     * downloaded.
     */
    private void setupQueueDownloads() {
        List<MediaSessionCompat.QueueItem> itemJSONList = mediaQueueManager.getMediaQueue();
        for (int i = 0; i < itemJSONList.size(); i++) {
            APIRecommendations.ItemJSON itemJSON = (APIRecommendations.ItemJSON) itemJSONList.get(i)
                    .getDescription().getExtras().getSerializable(LineUpQueue.ApiItem.API_ITEM.name());
            APIRecommendations.AudioJSON audioJSON = itemJSON.links.getValidAudio();
            Shared.Progress progress = audioJSON.progressTracker;
            if (!progress.isFullyDownloaded()) {
                //isnt fully downloaded setup download
                Bundle bundleMediaProgress = new Bundle();
                bundleMediaProgress.putString(
                        ACTION,
                        BackgroundAudioService.Action.MEDIA_DOWNLOADING_PROGRESS.name()
                );
                bundleMediaProgress.putIntArray(
                        BackgroundAudioService.Action.MEDIA_DOWNLOADING_PROGRESS.name(),
                        new int[] {
                                0, //in nanoseconds
                                0, //get percentage
                        }
                );
                bundleMediaProgress.putString(
                        BackgroundAudioService.ActionExtras.MEDIA_PREPARED_HREF.name(),
                        itemJSON.href
                );
                mediaSessionCompat.sendSessionEvent(
                        BackgroundAudioService.Action.MEDIA_DOWNLOADING_PROGRESS.name(),
                        bundleMediaProgress
                );
                downloadAndPlayItem(itemJSON, false);
            }
        }
    }

    private void downloadStopAndRemove(APIRecommendations.ItemJSON itemJSON) {
        DownloadPoolExecutor.getInstance(AUDIO).stopAndRemove(
                itemJSON.links.getValidAudio().href
        );
        fileCache.deleteFileIfExists(itemJSON.links.getValidAudio().href, FileCache.Type.AUDIO);
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
            recommendations.updateData(recommendationsApiDataResponse, false);
        }
    }

    private boolean isMediaSkippable() {
        boolean skippable = true;
        if (currentMedia != null) {
            skippable = currentMedia.attributes.skippable;
        }
        return skippable;
    }

    private int getMediaDuration() {
        if (isPrepared) {
            return mediaPlayer.getDuration();
        } else if (currentMedia != null) {
            return currentMedia.attributes.duration * 1000;
        }
        return 0;
    }

    public int getMediaCurrentPosition() {
        if (isPrepared) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
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
            downloadAndPlayItem(queueItem, false);
        }

        Bundle bundleMediaAdd = new Bundle();
        bundleMediaAdd.putString(ACTION, BackgroundAudioService.Action.MEDIA_ADDED_TO_QUEUE.name());
        bundleMediaAdd.putBooleanArray(
                BackgroundAudioService.Action.MEDIA_ADDED_TO_QUEUE.name(),
                new boolean[] {
                        hasMedia(),
                        hasNextMedia()
                }
        );
        mediaSessionCompat.sendSessionEvent(
                BackgroundAudioService.Action.MEDIA_ADDED_TO_QUEUE.name(),
                bundleMediaAdd
        );
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

    private void startSeekUpdateThread() {
        seekThread = new Thread(() -> {
            int lastSeek = getMediaCurrentPosition()/1000;
            int currentSeek;
            int i = 0;
            while(true) {
                currentSeek = getMediaCurrentPosition();
                if (currentSeek/1000 != lastSeek) {
                    lastSeek = currentSeek/1000;
                    //update elapsed
                    currentMedia.attributes.rating.elapsed.getAndSet(currentSeek/1000);
                    Bundle bundleMediaSeek = new Bundle();
                    bundleMediaSeek.putString(ACTION, BackgroundAudioService.Action.SEEK_CHANGE.name());
                    bundleMediaSeek.putInt(
                            BackgroundAudioService.Action.SEEK_CHANGE.name(),
                            currentSeek
                    );
                    mediaSessionCompat.sendSessionEvent(
                            BackgroundAudioService.Action.SEEK_CHANGE.name(),
                            bundleMediaSeek
                    );
                    if (isPlaying) {
                        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                    }
                }
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    Log.e(TAG, "startSeekUpdateThread: failed to sleep", e);
                }
            }
        });
        seekThread.start();
    }

    /**
     * Sends a rating update
     */
    public void sendRatingUpdate(RatingSender.Type type) {
        if (currentMedia != null) {
            //set the current media information
            currentMedia.attributes.rating.rating = type.name();
            if (type.equals(RatingSender.Type.COMPLETED)) {
                currentMedia.attributes.rating.elapsed.set(currentMedia.attributes.rating.duration);
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

    /**
     * Gets the media image for the current media object
     *
     * @return current media image
     */
    private @NonNull Shared.ImageJSON getMediaImage() {
        Shared.ImageJSON imageJSON;
        if (currentMedia != null && currentMedia.links.hasImage()) {
            imageJSON = currentMedia.links.getValidImage();
        } else {
            imageJSON = new Shared.ImageJSON();
        }
        return imageJSON;
    }
}
