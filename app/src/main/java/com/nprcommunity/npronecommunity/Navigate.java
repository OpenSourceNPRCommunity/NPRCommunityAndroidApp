package com.nprcommunity.npronecommunity;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentTransaction;
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
import com.nprcommunity.npronecommunity.API.RatingSender;
import com.nprcommunity.npronecommunity.Background.BackgroundAudioService;
import com.nprcommunity.npronecommunity.Layout.Adapter.ContentPageAdapter;
import com.nprcommunity.npronecommunity.Layout.Adapter.ContentQueueRecyclerViewAdapter;
import com.nprcommunity.npronecommunity.Layout.Fragment.ContentMediaPlayerFragment;
import com.nprcommunity.npronecommunity.Layout.Fragment.ContentViewPagerFragmentHolder;
import com.nprcommunity.npronecommunity.Layout.Fragment.ContentQueueFragment;
import com.nprcommunity.npronecommunity.Layout.Fragment.ContentRecommendationsFragment;
import com.nprcommunity.npronecommunity.Store.FileCache;
import com.nprcommunity.npronecommunity.Store.SettingsAndTokenManager;

import java.util.Observer;

import static com.nprcommunity.npronecommunity.Background.BackgroundAudioService.ActionExtras.MEDIA_NEXT_IS_SKIPPABLE;
import static com.nprcommunity.npronecommunity.Background.BackgroundAudioService.ActionExtras.MEDIA_PREPARED_IS_SKIPPABLE;

public class Navigate extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        ContentRecommendationsFragment.OnFragmentInteractionListener,
        ContentQueueFragment.OnListFragmentInteractionListener,
        ContentMediaPlayerFragment.OnFragmentInteractionListener {

    private static final String TAG = "NAVIGATE";

    private IBinder serviceBinder;
    private ServiceConnection serverConn;
    private Button buttonNext, buttonRewind, buttonPausePlay, buttonMediaMaxMin;
    private ProgressBar progressBarButtonPausePlay;
    private TextView currentMediaTextView;
    private NavigationView navigationView;
    private ContentViewPagerFragmentHolder contentViewPagerFragmentHolder;
    private ContentMediaPlayerFragment contentMediaPlayerFragment;
    private SettingsAndTokenManager settingsAndTokenManager;

    //used for keeping track if up or down
    private boolean isButtonMediaMax = false;

    public final Observer serviceObserver = (o, arg) -> {
        Navigate.this.runOnUiThread(() -> {
            updateAction((Bundle) arg);
        });
    };

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

        // last thing should be to setup the server connection
        setServerConn();
    }

    private void setupMediaButtons() {
        buttonNext = findViewById(R.id.button_next);
        buttonNext.setOnClickListener((View v) -> {
            if (serviceBinder.isBinderAlive()) {
                BackgroundAudioService.LocalBinder  localBinder
                    = (BackgroundAudioService.LocalBinder) serviceBinder;
                BackgroundAudioService backgroundAudioService = localBinder.getService();
                backgroundAudioService.nextMedia(true);
            } else {
                //TODO: error out or something?
            }
        });

        buttonRewind = findViewById(R.id.button_rewind);
        buttonRewind.setOnClickListener((View v) -> {
            if (serviceBinder.isBinderAlive()) {
                BackgroundAudioService.LocalBinder  localBinder
                        = (BackgroundAudioService.LocalBinder) serviceBinder;
                BackgroundAudioService backgroundAudioService = localBinder.getService();
                backgroundAudioService.seekMedia(
                        backgroundAudioService.getMediaCurrentPosition()-(int)(10*Util.MILLI_SECOND)
                );
            } else {
                //TODO: error out or something?
            }
        });

        buttonPausePlay = findViewById(R.id.button_pause_play);
        buttonPausePlay.setOnClickListener((View v) -> {
            if (serviceBinder.isBinderAlive()) {
                BackgroundAudioService.LocalBinder  localBinder
                        = (BackgroundAudioService.LocalBinder) serviceBinder;
                BackgroundAudioService backgroundAudioService = localBinder.getService();
                if (backgroundAudioService.getIsPlaying()) {
                    backgroundAudioService.pauseMedia();
                } else {
                    backgroundAudioService.playMedia();
                }
            } else {
                //TODO: error out or something?
            }
        });

        buttonMediaMaxMin = findViewById(R.id.button_media_max_min);
        buttonMediaMaxMin.setOnClickListener((View v) -> {
            isButtonMediaMax = !isButtonMediaMax;

            if (isButtonMediaMax) {
                // Create fragment
                if (contentMediaPlayerFragment == null) {
                    BackgroundAudioService.LocalBinder  localBinder
                            = (BackgroundAudioService.LocalBinder) serviceBinder;
                    BackgroundAudioService backgroundAudioService = localBinder.getService();
                    if (backgroundAudioService != null) {
                        contentMediaPlayerFragment = ContentMediaPlayerFragment.newInstance(
                                backgroundAudioService
                        );
                    }
                }

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
                    //todo error out
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
    public void onFragmentInteraction(Uri uri) {
        Log.i(TAG, "HEY IM A URI: " + uri.toString());
    }

    @Override
    public void onDestroy() {
        //remove the observer on destroy
        BackgroundAudioService.LocalBinder  localBinder
                = (BackgroundAudioService.LocalBinder) serviceBinder;
        BackgroundAudioService backgroundAudioService = localBinder.getService();
        backgroundAudioService.removeObserver(serviceObserver);

        unbindService(serverConn);
        stopService(new Intent(this, BackgroundAudioService.class));
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void setServerConn() {
        serverConn = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.d(TAG, "onServiceConnected");

                serviceBinder = binder;

                //get background service
                BackgroundAudioService.LocalBinder  localBinder
                        = (BackgroundAudioService.LocalBinder) serviceBinder;
                BackgroundAudioService backgroundAudioService = localBinder.getService();

                if (backgroundAudioService == null) {
                    //todo error out
                    return;
                }

                //setup content page adapter
                contentViewPagerFragmentHolder = ContentViewPagerFragmentHolder.newInstance(
                        backgroundAudioService
                );
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.content_container_fragment, contentViewPagerFragmentHolder).commit();

                //setup observer
                backgroundAudioService.addObserver(serviceObserver);

                //set the current title
                currentMediaTextView.setText(
                        backgroundAudioService.getMediaTitle()
                );

                //set the button text
                if (backgroundAudioService.getIsPlaying()) {
                    buttonPausePlay.setBackground(getDrawable(R.drawable.ic_pause_white_24dp));
                } else {
                    buttonPausePlay.setBackground(getDrawable(R.drawable.ic_play_arrow_white_24dp));
                }

                //set the next button and play button
                buttonPausePlay.setEnabled(backgroundAudioService.hasMedia());
                if (backgroundAudioService.getMediaIsSkippable()) {
                    //if it is skippable then we check if has next media
                    buttonNext.setEnabled(backgroundAudioService.hasNextMedia());
                } else {
                    buttonNext.setEnabled(false);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "onServiceDisconnected");
                //TODO: Error or something about service
            }
        };

        Intent intent = new Intent(this, BackgroundAudioService.class);
        bindService(intent, serverConn, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    private void updateAction(Bundle bundle) {
        BackgroundAudioService.Action action = BackgroundAudioService.Action.valueOf(
                bundle.getString(BackgroundAudioService.ACTION)
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
            case PLAY_BUTTON:
                buttonPausePlay.setBackground(getDrawable(R.drawable.ic_play_arrow_white_24dp));
                break;
            case PAUSE_BUTTON:
                buttonPausePlay.setBackground(getDrawable(R.drawable.ic_pause_white_24dp));
                break;
            case MEDIA_TITLE:
                currentMediaTextView.setText(
                        bundle.getString(BackgroundAudioService.Action.MEDIA_TITLE.name())
                );
                break;
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

                //send update about error loading
                BackgroundAudioService.LocalBinder  localBinder
                        = (BackgroundAudioService.LocalBinder) serviceBinder;
                BackgroundAudioService backgroundAudioService = localBinder.getService();
                if (backgroundAudioService != null) {
                    backgroundAudioService.sendRatingUpdate(RatingSender.Type.TIMEOUT);
                }
                break;
            case MEDIA_NEXT:
                boolean[] bools = bundle.getBooleanArray(
                        BackgroundAudioService.Action.MEDIA_NEXT.name()
                );
                /*
                 * [0] has media
                 * [1] has next media
                 */
                if (bools != null && bools.length == 2) {
                    if (bools[0]) {
                        swapPlayLoading(true);
                    } else {
                        currentMediaTextView.setText(R.string.nothing_to_play);
                        swapPlayLoading(false);
                    }

                    if (bundle.getBoolean(MEDIA_NEXT_IS_SKIPPABLE.name(), true)) {
                        //if it is skippable then load in whatever information is needed for this
                        buttonPausePlay.setEnabled(bools[0]);
                        buttonNext.setEnabled(bools[1]);
                    } else {
                        //if not skipplable then set button pause and button next to false
                        buttonPausePlay.setEnabled(true);
                        buttonNext.setEnabled(false);
                    }
                }
                if (contentMediaPlayerFragment != null) {
                    contentMediaPlayerFragment.updateMedia();
                }
                //Gets the
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                break;
            case SEEK_CHANGE:
                if (contentMediaPlayerFragment != null) {
                    contentMediaPlayerFragment.setSeek(
                            bundle.getInt(BackgroundAudioService.Action.SEEK_CHANGE.name()),
                            true
                    );
                }
                break;
            case MEDIA_PREPARED:
                swapPlayLoading(false);
                buttonPausePlay.setEnabled(true);
                buttonNext.setEnabled(bundle.getBoolean(MEDIA_PREPARED_IS_SKIPPABLE.name(), true));
                if (contentMediaPlayerFragment != null) {
                    contentMediaPlayerFragment.updateMedia();
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
                                String percent = "100% Downloaded";
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
                //re-enable button next for users without auto play
                if (!buttonNext.isEnabled()) {
                    buttonNext.setEnabled(true);
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
    public void onListFragmentInteraction(String queueItemURL) {
        Log.d(TAG, "onListFragmentInteraction: " + queueItemURL);
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
}
