package com.nprcommunity.npronecommunity.Layout.Fragment;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nprcommunity.npronecommunity.Background.BackgroundAudioService;
import com.nprcommunity.npronecommunity.Layout.Adapter.ContentPageAdapter;
import com.nprcommunity.npronecommunity.R;

public class ContentViewPagerFragmentHolder extends Fragment {

//    private OnFragmentInteractionListener mListener;

    private BackgroundAudioService backgroundAudioService;
    private ContentPageAdapter contentPageAdapter;

    public ContentViewPagerFragmentHolder() {}

    public static ContentViewPagerFragmentHolder newInstance(BackgroundAudioService backgroundAudioService) {
        ContentViewPagerFragmentHolder contentViewPagerFragmentHolder =
                new ContentViewPagerFragmentHolder();
        contentViewPagerFragmentHolder.setFragmentManager(backgroundAudioService);
        return contentViewPagerFragmentHolder;
    }

    public void setFragmentManager(BackgroundAudioService backgroundAudioService) {
        //inflate content page adapter
        this.backgroundAudioService = backgroundAudioService;
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
            contentPageAdapter = new ContentPageAdapter(
                    getChildFragmentManager(),
                    backgroundAudioService);
        } else {
            contentPageAdapter.notifyDataSetChanged();
        }
        ((ViewPager)view.findViewById(R.id.content_pager)).setAdapter(contentPageAdapter);
        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction(uri);
//        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
//        if (context instanceof OnFragmentInteractionListener) {
//            mListener = (OnFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
//        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
