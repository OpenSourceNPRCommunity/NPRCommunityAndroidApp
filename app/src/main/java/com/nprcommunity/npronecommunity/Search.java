package com.nprcommunity.npronecommunity;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.nprcommunity.npronecommunity.API.APIRecommendations;
import com.nprcommunity.npronecommunity.API.APISearch;
import com.nprcommunity.npronecommunity.Background.BackgroundAudioService;
import com.nprcommunity.npronecommunity.Background.Queue.LineUpQueue;
import com.nprcommunity.npronecommunity.Layout.Adapter.SearchListAdapter;
import com.nprcommunity.npronecommunity.Layout.Fragment.ContentRecommendationsFragment;

import static com.nprcommunity.npronecommunity.Background.BackgroundAudioService.CommandCompat.PLAY_MEDIA_NOW;
import static com.nprcommunity.npronecommunity.Background.BackgroundAudioService.CommandCompatExtras.PLAY_MEDIA_NOW_QUEUE_ITEM;

public class Search extends AppCompatActivity
    implements ContentRecommendationsFragment.OnFragmentInteractionListener {

    public static final int RESULT_CODE = 1002;
    private String TAG = "Search";

    private MediaControllerCompat mediaControllerCompat;
    private MediaBrowserCompat mediaBrowserCompat;
    private MediaBrowserCompat.ConnectionCallback mediaBrowserCompatConnectionCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            super.onConnected();
            try {
                mediaControllerCompat = new MediaControllerCompat(Search.this, mediaBrowserCompat.getSessionToken());
                MediaControllerCompat.setMediaController(Search.this, mediaControllerCompat);

            } catch (RemoteException e) {
                Log.e(TAG, "onConnected: error connecting to remote", e);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // setup to listen to media combat and get callbacks
        mediaBrowserCompat = new MediaBrowserCompat(this,
                new ComponentName(this, BackgroundAudioService.class),
                mediaBrowserCompatConnectionCallback, getIntent().getExtras());
        mediaBrowserCompat.connect();

        handleIntent(getIntent());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.search, menu);

        SearchManager searchManager = (SearchManager)
                getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchMenuItem = menu.findItem(R.id.search_search);
        searchMenuItem.expandActionView();
        searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                return false;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                //finish this activity on collapse
                Search.this.finish();
                return false;
            }
        });
        SearchView searchView = (SearchView) searchMenuItem.getActionView();

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Search.this.findViewById(R.id.search_progress_bar).setVisibility(View.VISIBLE);
                Search.this.findViewById(R.id.search_list).setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            handleSearch(query);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaBrowserCompat.disconnect();
    }

    @Override
    public boolean onSearchRequested() {
        Bundle appData = new Bundle();
        startSearch(null, false, appData, false);
        return true;
    }

    private void handleSearch(String query) {
        APISearch search = new APISearch(this, query);
        search.updateData(() -> {
            APISearch.SearchJSON searchData = search.getData();
            if(searchData == null) {
                Log.e(TAG, "handleSearch: APISearch data is null for channel adapter");
                //TODO put error up on screen
            } else {
                Log.d(TAG, "handleSearch: APISearch got data!");

                ListView searchList = Search.this.findViewById(R.id.search_list);
                searchList.setAdapter(new SearchListAdapter(
                        Search.this,
                        R.id.search_list,
                        searchData.items,
                        this
                ));
                //hide the progress bar show the search list
                Search.this.findViewById(R.id.search_progress_bar).setVisibility(View.GONE);
                Search.this.findViewById(R.id.search_list).setVisibility(View.VISIBLE);
            }
        },
                false);
    }

    @Override
    public String getMediaHref() {
        return mediaControllerCompat.getMetadata().getDescription().getMediaId();
    }

    @Override
    public void addToQueue(APIRecommendations.ItemJSON queueItem) {
        MediaSessionCompat.QueueItem mediaQueueItem = LineUpQueue.translateAPIQueueItem(queueItem);
        mediaControllerCompat.addQueueItem(mediaQueueItem.getDescription());
    }

    @Override
    public void playMediaNow(APIRecommendations.ItemJSON queueItem) {
        Bundle playMediaNow = new Bundle();
        playMediaNow.putSerializable(PLAY_MEDIA_NOW_QUEUE_ITEM.name(), queueItem);
        mediaControllerCompat.sendCommand(PLAY_MEDIA_NOW.name(), playMediaNow, null);
    }
}
