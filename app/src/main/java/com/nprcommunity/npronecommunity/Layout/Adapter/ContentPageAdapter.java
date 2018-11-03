package com.nprcommunity.npronecommunity.Layout.Adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.nprcommunity.npronecommunity.Background.BackgroundAudioService;
import com.nprcommunity.npronecommunity.Layout.Fragment.ContentQueueFragment;
import com.nprcommunity.npronecommunity.Layout.Fragment.ContentRecommendationsFragment;

import java.util.Observable;

public class ContentPageAdapter extends FragmentPagerAdapter {

    //Should setup with getItem and be congruent, or things will break
    //Ex. index 0=recommendations so setup getItem to return recommendations adapter
    public enum PageItem {
        RECOMMENDATIONS,
        QUEUE
    }

    private Observable tileObservable;
    private ContentQueueFragment currentContentQueueFragment;

    public ContentPageAdapter(FragmentManager fm, Observable tileObservable) {
        super(fm);
        this.tileObservable = tileObservable;
    }

    @Override
    public Fragment getItem(int position) {
        //Grab the recommendations fragment if position is 0
        if (position == 0) {
            return ContentRecommendationsFragment.newInstance(tileObservable);
        }
        //Grab the content queue if position is anything else
        currentContentQueueFragment = ContentQueueFragment.newInstance();
        return currentContentQueueFragment;
    }

    public ContentQueueFragment getCurrentContentQueueFragment() {
        return currentContentQueueFragment;
    }

    @Override
    public int getItemPosition(Object object) {
        //Used to return if its been updated
        return POSITION_NONE;
    }

    @Override
    public int getCount() {
        return PageItem.values().length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        //TODO: Set name to be loaded from xml
        if (position == 0) {
            return "Shows";
        }
        return "Queue";
    }
}
