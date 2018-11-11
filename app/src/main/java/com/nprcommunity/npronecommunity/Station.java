package com.nprcommunity.npronecommunity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.nprcommunity.npronecommunity.API.APIStation;
import com.nprcommunity.npronecommunity.Layout.Adapter.StationListAdapter;
import com.nprcommunity.npronecommunity.Store.SettingsAndTokenManager;

public class Station extends AppCompatActivity {
    public static final int RESULT_CODE = 1001;
    public static final String TAG = "STATION";
    private SettingsAndTokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station);
        handleIntent(getIntent());
        tokenManager = new SettingsAndTokenManager(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.station, menu);

        SearchManager searchManager = (SearchManager)
                getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchMenuItem = menu.findItem(R.id.search_station);
        searchMenuItem.expandActionView();
        searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                return false;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                //finish this activity on collapse
                Station.this.finish();
                return false;
            }
        });
        SearchView searchView = (SearchView) searchMenuItem.getActionView();

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setSubmitButtonEnabled(true);

        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        //handle search
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            // Do work using string
            APIStation search = new APIStation(this, query);
            search.updateData(() -> {
                APIStation.StationJSON stationData = search.getData();
                if(stationData == null) {
                    Log.e(TAG, "handleIntent: APISearch data is null for channel adapter");
                    //TODO put error up on screen
                } else {
                    Log.d(TAG, "handleIntent: APISearch got data!");

                    ListView searchList = Station.this.findViewById(R.id.station_list);
                    searchList.setAdapter(new StationListAdapter(
                            Station.this,
                            R.id.station_list,
                            stationData.items,
                            tokenManager.getToken()
                    ));
                }
            },
                    false);
        }
    }
}
