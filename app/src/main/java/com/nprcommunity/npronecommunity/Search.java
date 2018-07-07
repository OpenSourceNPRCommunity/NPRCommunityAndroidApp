package com.nprcommunity.npronecommunity;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.nprcommunity.npronecommunity.API.APISearch;
import com.nprcommunity.npronecommunity.Background.BackgroundAudioService;
import com.nprcommunity.npronecommunity.Layout.Adapter.SearchListAdapter;

public class Search extends AppCompatActivity {

    public static final int RESULT_CODE = 1002;
    private String TAG = "Search";
    private IBinder serviceBinder;
    private ServiceConnection serverConn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
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
                    handleSearch(query, backgroundAudioService);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    Log.d(TAG, "onServiceDisconnected");
                    //TODO: Error or something about service
                }
            };

            Intent backgroundServiceIntent = new Intent(this, BackgroundAudioService.class);
            bindService(backgroundServiceIntent, serverConn, Context.BIND_AUTO_CREATE);
            startService(backgroundServiceIntent);
        }
    }

    @Override
    public void onDestroy() {
        if (serverConn != null) {
            unbindService(serverConn);
        }
        super.onDestroy();
    }

    @Override
    public boolean onSearchRequested() {
        Bundle appData = new Bundle();
        startSearch(null, false, appData, false);
        return true;
    }

    private void handleSearch(String query, BackgroundAudioService backgroundAudioService) {
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
                        backgroundAudioService
                ));
                //hide the progress bar show the search list
                Search.this.findViewById(R.id.search_progress_bar).setVisibility(View.GONE);
                Search.this.findViewById(R.id.search_list).setVisibility(View.VISIBLE);
            }
        });
    }
}
