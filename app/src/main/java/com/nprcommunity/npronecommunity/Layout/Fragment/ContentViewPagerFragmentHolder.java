package com.nprcommunity.npronecommunity.Layout.Fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nprcommunity.npronecommunity.Layout.Adapter.ContentPageAdapter;
import com.nprcommunity.npronecommunity.R;

import java.util.Observable;

public class ContentViewPagerFragmentHolder extends Fragment {

    private ContentPageAdapter contentPageAdapter;
//    private OnFragmentInteractionListener listener;
    private Observable observable = new Observable() {
        @Override
        public void notifyObservers(Object arg) {
            setChanged();
            super.notifyObservers(arg);
        }
    };

    public ContentViewPagerFragmentHolder() {}

    public static ContentViewPagerFragmentHolder newInstance() {
        ContentViewPagerFragmentHolder contentViewPagerFragmentHolder =
                new ContentViewPagerFragmentHolder();
        return contentViewPagerFragmentHolder;
    }

    public ContentPageAdapter getContentPageAdapter() {
        return contentPageAdapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.content_view_pager_holder_fragment,
                container, false);
        if (contentPageAdapter == null) {
            contentPageAdapter = new ContentPageAdapter(getChildFragmentManager(), observable);
        } else {
            contentPageAdapter.notifyDataSetChanged();
        }
        ((ViewPager)view.findViewById(R.id.content_pager)).setAdapter(contentPageAdapter);
        return view;
    }

    public void updateTiles() {
        observable.notifyObservers();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
//        if (context instanceof OnFragmentInteractionListener) {
//            listener = (OnFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
//        listener = null;
    }

//    public interface OnFragmentInteractionListener {
//        void onFragmentInteraction(Uri uri);
//    }
}
