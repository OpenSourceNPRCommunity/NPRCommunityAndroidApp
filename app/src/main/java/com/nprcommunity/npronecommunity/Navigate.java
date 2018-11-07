package com.nprcommunity.npronecommunity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.nprcommunity.npronecommunity.API.APIRecommendations;
import com.nprcommunity.npronecommunity.Background.BackgroundAudioService;
import com.nprcommunity.npronecommunity.Background.MediaQueueManager;
import com.nprcommunity.npronecommunity.Background.Queue.LineUpQueue;
import com.nprcommunity.npronecommunity.Layout.Adapter.ContentPageAdapter;
import com.nprcommunity.npronecommunity.Layout.Adapter.ContentQueueRecyclerViewAdapter;
import com.nprcommunity.npronecommunity.Layout.Fragment.ContentMediaPlayerFragment;
import com.nprcommunity.npronecommunity.Layout.Fragment.ContentViewPagerFragmentHolder;
import com.nprcommunity.npronecommunity.Layout.Fragment.ContentQueueFragment;
import com.nprcommunity.npronecommunity.Layout.Fragment.ContentRecommendationsFragment;
import com.nprcommunity.npronecommunity.Store.FileCache;
import com.nprcommunity.npronecommunity.Store.SettingsAndTokenManager;

import java.util.List;

import static android.support.v4.media.session.PlaybackStateCompat.STATE_BUFFERING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_CONNECTING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_ERROR;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_FAST_FORWARDING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_NONE;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_REWINDING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_NEXT;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED;
import static com.nprcommunity.npronecommunity.Background.BackgroundAudioService.ACTION;
import static com.nprcommunity.npronecommunity.Background.BackgroundAudioService.CommandCompat.PLAY_MEDIA_NOW;
import static com.nprcommunity.npronecommunity.Background.BackgroundAudioService.CommandCompatExtras.ADD_ITEM_OBJECT;
import static com.nprcommunity.npronecommunity.Background.BackgroundAudioService.CommandCompatExtras.PLAY_MEDIA_NOW_QUEUE_ITEM;
import static com.nprcommunity.npronecommunity.Background.BackgroundAudioService.CommandCompatExtras.REMOVE_INDEX_I;
import static com.nprcommunity.npronecommunity.Background.BackgroundAudioService.CommandCompatExtras.REMOVE_ITEM_OBJECT;
import static com.nprcommunity.npronecommunity.Background.BackgroundAudioService.CommandCompatExtras.SWAP_POS_ONE;
import static com.nprcommunity.npronecommunity.Background.BackgroundAudioService.CommandCompatExtras.SWAP_POS_TWO;
import static com.nprcommunity.npronecommunity.Background.BackgroundAudioService.METADATA_KEY_IMAGE_HREF;

public class Navigate extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        ContentRecommendationsFragment.OnFragmentInteractionListener,
        ContentQueueFragment.OnListFragmentInteractionListener,
        ContentMediaPlayerFragment.OnFragmentInteractionListener {

    private static final String TAG = "NAVIGATE";

    private Button buttonNext, buttonRewind, buttonPausePlay, buttonMediaMaxMin;
    private ProgressBar progressBarButtonPausePlay;
    private TextView currentMediaTextView;
    private NavigationView navigationView;
    private ContentViewPagerFragmentHolder contentViewPagerFragmentHolder;
    private ContentMediaPlayerFragment contentMediaPlayerFragment;
    private SettingsAndTokenManager settingsAndTokenManager;

    //used for keeping track if up or down
    private boolean isButtonMediaMax = false;

    private MediaControllerCompat.Callback mediaControllerCompatCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            super.onSessionEvent(event, extras);
            Navigate.this.runOnUiThread(() -> {
                updateAction(extras);
            });
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            if (state == null) {
                return;
            }

            ContentQueueRecyclerViewAdapter adapter = null;
            ContentQueueFragment contentQueueFragment = null;
            if (contentViewPagerFragmentHolder != null) {
                ContentPageAdapter contentPageAdapter = contentViewPagerFragmentHolder.getContentPageAdapter();
                if (contentPageAdapter != null) {
                    contentQueueFragment = contentPageAdapter.getCurrentContentQueueFragment();
                    if (contentQueueFragment != null) {
                        adapter = (ContentQueueRecyclerViewAdapter) contentQueueFragment.getQueueAdapter();
                    }
                }
            }

            switch (state.getState()) {
                case STATE_PLAYING:
                    buttonPausePlay.setEnabled(true);
                    buttonRewind.setEnabled(true);
                    setButtonNext();
                    swapPlayLoading(false);
                    contentMediaPlayerFragment.enableSeekBar(true);

                    buttonPausePlay.setBackground(getDrawable(R.drawable.ic_pause_white_24dp));
                    break;
                case STATE_PAUSED:
                    buttonPausePlay.setEnabled(true);
                    buttonRewind.setEnabled(true);
                    setButtonNext();
                    swapPlayLoading(false);
                    contentMediaPlayerFragment.enableSeekBar(true);

                    buttonPausePlay.setBackground(getDrawable(R.drawable.ic_play_arrow_white_24dp));
                    break;
                case STATE_BUFFERING:
                    //update more information fragment
                    //NOTE! must be before enableSeekBar() call below... method overwrites it
                    contentMediaPlayerFragment.updateMedia();

                    buttonPausePlay.setEnabled(false);
                    buttonRewind.setEnabled(false);
                    setButtonNext();
                    swapPlayLoading(true);
                    contentMediaPlayerFragment.enableSeekBar(false);

                    if (mediaControllerCompat.getQueue().size() == 0) {
                        // if it has media
                        currentMediaTextView.setText(R.string.nothing_to_play);
                    }

                    //set the current title
                    currentMediaTextView.setText(
                            mediaControllerCompat.getMetadata().getDescription().getTitle()
                    );

                    // update adapter about buffering change... may be loading in a new media
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                    contentViewPagerFragmentHolder.updateTiles();
                    break;
                case STATE_SKIPPING_TO_NEXT:
                    //update more information fragment
                    //NOTE! must be before enableSeekBar() call below... method overwrites it
                    contentMediaPlayerFragment.updateMedia();

                    buttonPausePlay.setEnabled(false);
                    buttonRewind.setEnabled(false);
                    setButtonNext();
                    swapPlayLoading(true);
                    contentMediaPlayerFragment.enableSeekBar(false);

                    if (mediaControllerCompat.getQueue().size() == 0) {
                        // if it has media
                        currentMediaTextView.setText(R.string.nothing_to_play);
                        contentMediaPlayerFragment.clearData();
                    }

                    // update adapter about buffering change... may be loading in a new media
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                    contentViewPagerFragmentHolder.updateTiles();
                    break;
                case STATE_SKIPPING_TO_QUEUE_ITEM:
                    throw new Error("Not implemented");
                case STATE_CONNECTING:
                    Log.w(TAG, "onPlaybackStateChanged: STATE_CONNECTING not implemented");
                    break;
                case STATE_ERROR:
                    Log.w(TAG, "onPlaybackStateChanged: STATE_ERROR not implemented");
                    break;
                case STATE_STOPPED:
                    Log.w(TAG, "onPlaybackStateChanged: STATE_STOPPED not implemented");
                    break;
                case STATE_NONE:
                    Log.w(TAG, "onPlaybackStateChanged: STATE_NONE not implemented");
                    break;
                case STATE_SKIPPING_TO_PREVIOUS:
                    Log.w(TAG, "onPlaybackStateChanged: STATE_NONE not implemented");
                    break;
                case STATE_REWINDING: // act as same as fast forward, just update where we are at
                case STATE_FAST_FORWARDING:
                    contentMediaPlayerFragment.setSeek(
                            (int)state.getPosition(),
                            true
                    );
                    break;
            }
        }
    };
    private MediaControllerCompat mediaControllerCompat;
    private MediaBrowserCompat mediaBrowserCompat;
    private MediaBrowserCompat.ConnectionCallback mediaBrowserCompatConnectionCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            super.onConnected();
            try {
                mediaControllerCompat = new MediaControllerCompat(Navigate.this, mediaBrowserCompat.getSessionToken());
                mediaControllerCompat.registerCallback(mediaControllerCompatCallback);
                MediaControllerCompat.setMediaController(Navigate.this, mediaControllerCompat);

                //setup content page adapter
                contentViewPagerFragmentHolder = ContentViewPagerFragmentHolder.newInstance();
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.content_container_fragment, contentViewPagerFragmentHolder).commit();

                //set the current title
                currentMediaTextView.setText(
                        mediaControllerCompat.getMetadata().getDescription().getTitle()
                );

                //set the button text
                if (mediaControllerCompat.getPlaybackState().getState() == STATE_PLAYING) {
                    buttonPausePlay.setBackground(getDrawable(R.drawable.ic_pause_white_24dp));
                } else {
                    buttonPausePlay.setBackground(getDrawable(R.drawable.ic_play_arrow_white_24dp));
                }

                //set the next button and play button
                buttonPausePlay.setEnabled(
                        mediaControllerCompat.getPlaybackState().getState() == STATE_PAUSED ||
                            mediaControllerCompat.getPlaybackState().getState() == STATE_PLAYING
                );
                setButtonNext();

            } catch (RemoteException e) {
                Log.e(TAG, "onConnected: error connecting to remote", e);
            }
        }
    };

    private void setButtonNext() {
        buttonNext.setEnabled(mediaControllerCompat.getQueue().size() > 1 && isMediaSkippable());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionDialog(this));

        setContentView(R.layout.activity_navigate);
        settingsAndTokenManager = new SettingsAndTokenManager(this);

        //for now only support portrait landscape
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //update the file usage
                FileCache fileCache = FileCache.getInstances(Navigate.this);
                String imageSize = " " + Util.getBytesString((int)fileCache.getDirSize(FileCache.Type.IMAGE));
                //image usage stat
                navigationView.getMenu().findItem(R.id.nav_image_cache).setTitle(
                    getString(R.string.nav_item_image_size) + imageSize
                );
                //audio usage stat
                String audioSize = " " + Util.getBytesString((int)fileCache.getDirSize(FileCache.Type.AUDIO));
                navigationView.getMenu().findItem(R.id.nav_audio_cache).setTitle(
                        getString(R.string.nav_item_audio_size) + audioSize
                );
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        contentMediaPlayerFragment = ContentMediaPlayerFragment.newInstance();

        //setup auto play button action
        Switch autoPlayEnableSwitch = navigationView.getMenu().findItem(R.id.nav_auto_play)
                .getActionView().findViewById(R.id.enable);
        autoPlayEnableSwitch.setChecked(settingsAndTokenManager.getConfigBoolean(
                SettingsAndTokenManager.SettingsKey.AUTO_PLAY_ENABLED,
                true
        ));
        autoPlayEnableSwitch.setOnClickListener((View v) ->{
            Switch aSwitch = (Switch) v;
            settingsAndTokenManager.setConfig(
                    SettingsAndTokenManager.SettingsKey.AUTO_PLAY_ENABLED,
                    Boolean.valueOf(aSwitch.isChecked())
            );
        });

        //setup swipe remove
        Switch queueSwipeRemoveSwitch = navigationView.getMenu().findItem(R.id.nav_queue_swipe_remove)
                .getActionView().findViewById(R.id.enable);
        queueSwipeRemoveSwitch.setChecked(settingsAndTokenManager.getConfigBoolean(
                SettingsAndTokenManager.SettingsKey.SWIPE_REMOVE_ENABLED,
                false
        ));
        queueSwipeRemoveSwitch.setOnClickListener((View v) ->{
            Switch aSwitch = (Switch) v;
            settingsAndTokenManager.setConfig(
                    SettingsAndTokenManager.SettingsKey.SWIPE_REMOVE_ENABLED,
                    Boolean.valueOf(aSwitch.isChecked())
            );
        });

        setupMediaButtons();

        //Sets the scrolling title for the song
        currentMediaTextView = findViewById(R.id.current_song_text);
        currentMediaTextView.setSelected(true);

        //schedule the job
        FileCache.createCacheJobService(this);

        // setup to listen to media combat and get callbacks
        mediaBrowserCompat = new MediaBrowserCompat(this,
                new ComponentName(this, BackgroundAudioService.class),
                mediaBrowserCompatConnectionCallback, getIntent().getExtras());
        mediaBrowserCompat.connect();
    }

    private void setupMediaButtons() {
        buttonNext = findViewById(R.id.button_next);
        buttonNext.setOnClickListener((View v) -> {
            mediaControllerCompat.getTransportControls().skipToNext();
        });

        buttonRewind = findViewById(R.id.button_rewind);
        buttonRewind.setOnClickListener((View v) -> {
            mediaControllerCompat.getTransportControls().seekTo(
                    mediaControllerCompat.getPlaybackState().getPosition()
                            -(int)(10*Util.MILLI_SECOND)
            );
        });

        buttonPausePlay = findViewById(R.id.button_pause_play);
        buttonPausePlay.setOnClickListener((View v) -> {
            if (mediaControllerCompat.getPlaybackState().getState() == STATE_PLAYING) {
                mediaControllerCompat.getTransportControls().pause();
            } else {
                mediaControllerCompat.getTransportControls().play();
            }
        });

        buttonMediaMaxMin = findViewById(R.id.button_media_max_min);
        buttonMediaMaxMin.setOnClickListener((View v) -> {
            isButtonMediaMax = !isButtonMediaMax;

            if (isButtonMediaMax) {
                // Create fragment
                contentMediaPlayerFragment = ContentMediaPlayerFragment.newInstance();

                //make transaction to replace current fragment
                //thanks to https://stackoverflow.com/a/40895000/5522992 for some help
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.setCustomAnimations(R.anim.slide_up, R.anim.fade_out,
                        R.anim.fade_in, R.anim.slide_down);
                transaction.replace(R.id.content_container_fragment, contentMediaPlayerFragment);
                transaction.addToBackStack(null);
                transaction.commit();
                v.animate().rotation(180).start();
            } else {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    //pop back the past fragment
                    getSupportFragmentManager().popBackStack();
                    v.animate().rotation(360).start();
                } else {
                    Log.e(TAG, "setupMediaButtons: Error stack is: " +
                            getSupportFragmentManager().getBackStackEntryCount());
                }
            }
        });

        progressBarButtonPausePlay = findViewById(R.id.progress_bar_pause_play);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            buttonMediaMaxMin.animate().rotation(0).start();
        } else {
            moveTaskToBack(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.navigate, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.navigate_search) {
            Intent intent = new Intent(this, Search.class);
            startActivityForResult(intent, Search.RESULT_CODE);
            return false;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_channels) {
            //start the select channel activity
            Intent intent = new Intent(this, ChannelSelect.class);
            startActivityForResult(intent, ChannelSelect.RESULT_CODE);
        } else if (id == R.id.nav_auto_play) {
            settingsAndTokenManager.setConfig(SettingsAndTokenManager.SettingsKey.AUTO_PLAY_ENABLED,
                    item.isChecked());
        } else if (id == R.id.nav_station) {
            //start the select select activity
            Intent intent = new Intent(this, Station.class);
            startActivityForResult(intent, Station.RESULT_CODE);
            return true;
        } else if (id == R.id.nav_logout) {
            //logout
            settingsAndTokenManager.deleteToken();
            //remove cookies
            CookieManager.getInstance().removeAllCookies(null);
            Intent i = new Intent(Navigate.this, Login.class);
            startActivity(i);
            finish();
        } else if (id == R.id.nav_image_cache) {
            AlertDialog alertDialog = new AlertDialog.Builder(Navigate.this).create();
            alertDialog.setTitle(R.string.nav_item_image_dialog_title);
            alertDialog.setMessage(getString(R.string.nav_item_image_dialog_message));
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", (dialogInterface, i) -> {
                dialogInterface.dismiss();
            });
            alertDialog.show();
        } else if (id == R.id.nav_audio_cache) {
            AlertDialog alertDialog = new AlertDialog.Builder(Navigate.this).create();
            alertDialog.setTitle(R.string.nav_item_audio_dialog_title);
            alertDialog.setMessage(getString(R.string.nav_item_audio_dialog_message));
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", (dialogInterface, i) -> {
                dialogInterface.dismiss();
            });
            alertDialog.show();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaControllerCompat.getTransportControls().stop();
        mediaBrowserCompat.disconnect();
    }

    @Override
    public void onResume() {
        super.onResume();
        //set the media controller to this activity
        MediaControllerCompat.setMediaController(this, mediaControllerCompat);
    }

    private void updateAction(Bundle bundle) {
        BackgroundAudioService.Action action = BackgroundAudioService.Action.valueOf(
                bundle.getString(ACTION)
        );
        ContentQueueRecyclerViewAdapter adapter = null;
        ContentQueueFragment contentQueueFragment = null;
        if (contentViewPagerFragmentHolder != null) {
            ContentPageAdapter contentPageAdapter = contentViewPagerFragmentHolder.getContentPageAdapter();
            if (contentPageAdapter != null) {
                contentQueueFragment = contentPageAdapter.getCurrentContentQueueFragment();
                if (contentQueueFragment != null) {
                    adapter = (ContentQueueRecyclerViewAdapter) contentQueueFragment.getQueueAdapter();
                }
            }
        }
        switch (action) {
            case MEDIA_ERROR_LOADING:
                Toast.makeText(
                        getApplicationContext(),
                        bundle.getString(BackgroundAudioService.Action.MEDIA_ERROR_LOADING.name()),
                        Toast.LENGTH_LONG
                ).show();

                APIRecommendations.ItemJSON itemJSON = (APIRecommendations.ItemJSON) bundle.getSerializable(
                        BackgroundAudioService.ActionExtras.MEDIA_ERROR_LOADING_REMOVE_ITEM.name()
                );
                if (adapter != null && itemJSON != null) {
                    //TODO change this to specify notify remove data item
                    adapter.removeItem(itemJSON);
                }
                mediaControllerCompat.sendCommand(
                        BackgroundAudioService.CommandCompat.SEND_RATING_TIMEOUT.name(),
                        null,
                        null
                );
                break;
            case SEEK_CHANGE:
                if (contentMediaPlayerFragment != null) {
                    contentMediaPlayerFragment.setSeek(
                            bundle.getInt(BackgroundAudioService.Action.SEEK_CHANGE.name()),
                            true
                    );
                }
                break;
            case MEDIA_ADDED_TO_QUEUE:
                boolean[] addToQueueBools = bundle.getBooleanArray(
                        BackgroundAudioService.Action.MEDIA_ADDED_TO_QUEUE.name()
                );
                /*
                 * [0] has media
                 * [1] has next media
                 */
                if (addToQueueBools != null && addToQueueBools.length == 2) {
                    if (addToQueueBools[0] && !addToQueueBools[1]) {
                        swapPlayLoading(true);
                    }
                    buttonPausePlay.setEnabled(addToQueueBools[0]);
                    if (buttonNext.isEnabled()) {
                        //only if it is already enabled to we reset it
                        buttonNext.setEnabled(addToQueueBools[1]);
                    }
                }
                //Gets the
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                contentViewPagerFragmentHolder.updateTiles();
                break;
            case MEDIA_DOWNLOADING_PROGRESS:
                int[] downloadProgressInts = bundle.getIntArray(
                        BackgroundAudioService.Action.MEDIA_DOWNLOADING_PROGRESS.name()
                );

                /*
                 * [0] the current position
                 * [1] speed in nanoseconds
                 * [2] percentage
                 */
                if (downloadProgressInts != null && downloadProgressInts.length == 3) {
                    if (contentQueueFragment != null) {
                        View tmpView = contentQueueFragment.getView(
                                bundle.getString(
                                        BackgroundAudioService.ActionExtras.MEDIA_PREPARED_HREF.name()
                                )
                        );

                        if (tmpView != null) {
                            if (downloadProgressInts[2] == 100) {
                                //if percentage is 100
                                String speed = Util.getBytesPerSecString(downloadProgressInts[1]);
                                tmpView.findViewById(R.id.queue_progress_speed)
                                        .setVisibility(View.GONE);
                                String percent = getString(R.string.one_hundred_pecent_downloaded);
                                ((TextView)tmpView.findViewById(R.id.queue_progress_percent)).setText(
                                        percent
                                );
                                tmpView.findViewById(R.id.queue_progress_bar)
                                        .setVisibility(View.GONE);
                            } else {
                                //when downloading update these views
                                String speed = Util.getBytesPerSecString(downloadProgressInts[1]);
                                TextView progressSpeed = tmpView.findViewById(R.id.queue_progress_speed);
                                progressSpeed.setText(
                                        speed
                                );
                                progressSpeed.setVisibility(View.VISIBLE);
                                String percent = downloadProgressInts[2] + "%";
                                ((TextView)tmpView.findViewById(R.id.queue_progress_percent)).setText(
                                        percent
                                );
                                ProgressBar progressBar = tmpView.findViewById(R.id.queue_progress_bar);
                                progressBar.setProgress(
                                        downloadProgressInts[2]
                                );
                                progressBar.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                }
                break;
            case MEDIA_COMPLETE:
                //re-enable button next for users without auto play and if skippable is disabled
                if (!buttonNext.isEnabled()) {
                    buttonNext.setEnabled(true);
                }
                buttonPausePlay.setBackground(getDrawable(R.drawable.ic_play_arrow_white_24dp));
                break;
            case MEDIA_REMOVED:
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                break;
        }
    }

    private void swapPlayLoading(boolean loading) {
        if (loading) {
            buttonPausePlay.setVisibility(View.GONE);
            progressBarButtonPausePlay.setVisibility(View.VISIBLE);
        } else {
            buttonPausePlay.setVisibility(View.VISIBLE);
            progressBarButtonPausePlay.setVisibility(View.GONE);
        }
    }

    @Override
    public void remove(int position) {
        Bundle bundleRemove = new Bundle();
        bundleRemove.putSerializable(REMOVE_INDEX_I.name(), position);
        mediaControllerCompat.sendCommand(
                BackgroundAudioService.CommandCompat.REMOVE_INDEX.name(),
                bundleRemove, null);
    }

    @Override
    public void remove(APIRecommendations.ItemJSON itemJSON) {
        Bundle bundleRemove = new Bundle();
        bundleRemove.putSerializable(REMOVE_ITEM_OBJECT.name(), itemJSON);
        mediaControllerCompat.sendCommand(
                BackgroundAudioService.CommandCompat.REMOVE_ITEM.name(),
                bundleRemove, null);
    }

    @Override
    public void swap(int fromPosition, int toPosition) {
        Bundle bundleSwap = new Bundle();
        bundleSwap.putInt(SWAP_POS_ONE.name(), fromPosition);
        bundleSwap.putInt(SWAP_POS_TWO.name(), toPosition);
        mediaControllerCompat.sendCommand(BackgroundAudioService.CommandCompat.SWAP.name(),
                bundleSwap, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case ChannelSelect.RESULT_CODE:
                //turn off check of menu item
                Menu menu = navigationView.getMenu();
                MenuItem menuItem = menu.findItem(R.id.nav_channels);
                menuItem.setChecked(false);

                //refresh the fragment views
                contentViewPagerFragmentHolder.getContentPageAdapter().notifyDataSetChanged();
                break;
        }
    }

    @Override
    public void seekMedia(int pos) {
        mediaControllerCompat.getTransportControls().seekTo(pos);
    }

    @NonNull
    @Override
    public MediaDescriptionCompat getMediaDescription() {
        return mediaControllerCompat.getMetadata().getDescription();
    }

    @Override
    public int getDuration() {
        return (int)mediaControllerCompat.getMetadata().getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
    }

    @Override
    public int getCurrentPosition() {
        return (int)mediaControllerCompat.getPlaybackState().getPosition();
    }

    @Override
    public boolean isMediaSkippable() {
        return mediaControllerCompat.getMetadata()
                .getLong(MediaMetadataCompat.METADATA_KEY_ADVERTISEMENT) != 0;
    }

    @Override
    public String getMediaImage() {
        return mediaControllerCompat.getMetadata()
                .getString(METADATA_KEY_IMAGE_HREF);

    }

    @Override
    public String getShareUrl() {
        if (MediaQueueManager.getInstance(this).queueSize() > 0) {
            Bundle bundle = MediaQueueManager.getInstance(this)
                    .getQueueTrack(0).getDescription().getExtras();
            if (bundle != null) {
                APIRecommendations.ItemJSON itemJSON = ((APIRecommendations.ItemJSON)bundle
                                .getSerializable(LineUpQueue.ApiItem.API_ITEM.name()));
                if (itemJSON != null && itemJSON.links.hasOnramp()) {
                    return itemJSON.links.onramps.get(0).href;
                }
            }
        }
        return getApplication().getString(R.string.unknown);
    }

    @Override
    public void sendRatingThumbsUp() {
        mediaControllerCompat.sendCommand(BackgroundAudioService.CommandCompat.THUMBS_UP_RATING.name(),
                null, null);
    }

    @Override
    public String getAudioTitle() {
        return mediaControllerCompat.getMetadata()
                .getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE);
    }

    @Override
    public String getMediaHref() {
        return mediaControllerCompat.getMetadata().getDescription().getMediaId();
    }

    @Override
    public void addToQueue(APIRecommendations.ItemJSON queueItem) {
        Bundle bundleAddToQueue = new Bundle();
        bundleAddToQueue.putSerializable(ADD_ITEM_OBJECT.name(), queueItem);
        mediaControllerCompat.sendCommand(BackgroundAudioService.CommandCompat.ADD_ITEM.name(),
                bundleAddToQueue, null);
    }

    @Override
    public void playMediaNow(APIRecommendations.ItemJSON queueItem) {
        Bundle playMediaNow = new Bundle();
        playMediaNow.putSerializable(PLAY_MEDIA_NOW_QUEUE_ITEM.name(), queueItem);
        mediaControllerCompat.sendCommand(PLAY_MEDIA_NOW.name(), playMediaNow, null);
    }
}
