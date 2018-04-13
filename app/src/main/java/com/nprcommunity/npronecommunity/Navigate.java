package com.nprcommunity.npronecommunity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nprcommunity.npronecommunity.API.APIDataResponse;
import com.nprcommunity.npronecommunity.API.Channels;
import com.nprcommunity.npronecommunity.API.Recommendations;
import com.nprcommunity.npronecommunity.Background.BackgroundAudioService;
import com.nprcommunity.npronecommunity.Layout.Adapter.ContentPageAdapter;
import com.nprcommunity.npronecommunity.Layout.Fragment.ContentQueueFragment;
import com.nprcommunity.npronecommunity.Layout.Fragment.ContentRecommendationsFragment;
import com.orhanobut.hawk.Hawk;

public class Navigate extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        ContentRecommendationsFragment.OnFragmentInteractionListener,
        ContentQueueFragment.OnFragmentInteractionListener {

    private static String TAG = "NAVIGATE";

    private RecyclerView channelsRecyclerView;
    private RecyclerView.Adapter channelsAdapter;
    private RecyclerView.LayoutManager channelsLayoutManager;

    private IBinder serviceBinder;
    private ServiceConnection serverConn;

//    private HashMap<String, RecyclerView> recommendationsRecyclerView;
//    private HashMap<String, RecyclerView.Adapter> recommendationsAdapter = new HashMap<>();
//    private HashMap<String, RecyclerView.LayoutManager> recommendationsLayoutManager = new HashMap<>();
//    private RecyclerView recommendationsRecyclerView;
//    private RecyclerView.Adapter recommendationsAdapter;
//    private RecyclerView.LayoutManager recommendationsLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigate);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        Letter box thing icon
//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //Set up channels
//        channelsRecyclerView = (RecyclerView) findViewById(R.id.channels);
//        channelsLayoutManager = new LinearLayoutManager(this,
//                LinearLayoutManager.HORIZONTAL,
//                false);
//        channelsRecyclerView.setLayoutManager(channelsLayoutManager);
//
        //Old fragment code
//        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
//        android.support.v4.app.FragmentTransaction ft = fm.beginTransaction();
//        ft.add(R.id.content_pager, contentRecommendationsFragment);
//        ft.addToBackStack("CONTENTRECOMMENDATIONSFRAGMENT");
//        ft.commit();

        setServerConn();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

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
        unbindService(serverConn);
        super.onDestroy();
    }

    private void setServerConn() {
        serverConn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.d(TAG, "onServiceConnected");

                serviceBinder = binder;

                //Adapter used for the content page
                //will have queue and recommendations
                ContentPageAdapter contentPageAdapter = new ContentPageAdapter(getSupportFragmentManager());
                ViewPager viewPager = (ViewPager) findViewById(R.id.content_pager);
                viewPager.setAdapter(contentPageAdapter);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "onServiceDisconnected");
            }
        };

        Intent intent = new Intent(this, BackgroundAudioService.class);
        bindService(intent, serverConn, Context.BIND_AUTO_CREATE);
        startService(intent);
    }
}
