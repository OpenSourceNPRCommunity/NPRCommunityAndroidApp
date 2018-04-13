package com.nprcommunity.npronecommunity.Layout.Adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.view.View;

import com.nprcommunity.npronecommunity.Layout.Fragment.ContentQueueFragment;
import com.nprcommunity.npronecommunity.Layout.Fragment.ContentRecommendationsFragment;
import com.nprcommunity.npronecommunity.R;

public class ContentPageAdapter extends FragmentPagerAdapter {

    public ContentPageAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        //Grab the recommendations fragment if position is 0
        if (position == 0) {
            return ContentRecommendationsFragment.newInstance("NONE1", "NONE2");
        }
        //Grab the content queue if position is anything else
        return ContentQueueFragment.newInstance("NONE1", "NONE2");
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        //TODO: Set name to be loaded from xml
        if (position == 0) {
            return "Listed Channels";
        }
        return "Queue";
    }
}
