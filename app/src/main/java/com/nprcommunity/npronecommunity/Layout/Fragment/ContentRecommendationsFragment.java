package com.nprcommunity.npronecommunity.Layout.Fragment;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nprcommunity.npronecommunity.API.APIDataResponse;
import com.nprcommunity.npronecommunity.API.APIChannels;
import com.nprcommunity.npronecommunity.API.APIRecommendations;
import com.nprcommunity.npronecommunity.Background.BackgroundAudioService;
import com.nprcommunity.npronecommunity.R;
import com.nprcommunity.npronecommunity.Layout.Adapter.RecommendationsAdapter;
import com.nprcommunity.npronecommunity.Store.CacheStructures.ChannelCache;
import com.nprcommunity.npronecommunity.Store.CacheStructures.RecommendationCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ContentRecommendationsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ContentRecommendationsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ContentRecommendationsFragment extends Fragment {

    private static BackgroundAudioService backgroundAudioService;

    private static String TAG = "LAYOUT.FRAGMENT.CONTENTRECOMMENDATIONSFRAGMENT";

    private OnFragmentInteractionListener mListener;

    public ContentRecommendationsFragment() {
        // Required empty public constructor
    }

    public static ContentRecommendationsFragment newInstance(BackgroundAudioService bas) {
        ContentRecommendationsFragment fragment = new ContentRecommendationsFragment();
        backgroundAudioService = bas;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.content_recommendations_fragment,
                container,
                false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstancState) {
        APIChannels APIChannels = new APIChannels(getContext());
        APIDataResponse channelApiDataResponse = () -> {
            ChannelCache channelsData = APIChannels.getData();
            if(channelsData == null || channelsData.data == null) {
                Log.e(TAG, "onCreate: APIChannels data is null");
                //TODO put error up on screen
            } else {
                Log.d(TAG, "onCreate: APIChannels got data!");

                List<APIChannels.ItemJSON> channelItems = new ArrayList<>();
                //TODO add check for channel and recommendations validation
                for (APIChannels.ItemJSON channelItem : channelsData.data.items) {
                    if (channelItem.isChecked ||
                            APIChannels.DEFAULT_CHANNELS_MAP.containsKey(channelItem.attributes.id)) {
                        //is checked in channels section or is part of the default channels
                        channelItems.add(channelItem);
                    }
                }
                //sort list
                Collections.sort(channelItems, (APIChannels.ItemJSON t1, APIChannels.ItemJSON t2) -> {
                    return t1.attributes.fullName.compareTo(t2.attributes.fullName);
                });

                //init it
                for (APIChannels.ItemJSON channelItem : channelItems) {
                    //add each channel into it
                    initRecommendationRow(channelItem, getActivity());
                }
            }
        };
        APIChannels.updateData(channelApiDataResponse);
    }

    private static void initRecommendationRow(APIChannels.ItemJSON channelItem, Activity activity) {
        APIRecommendations APIRecommendations = new APIRecommendations(activity, channelItem.href);
        APIDataResponse recommendationsApiDataResponse = () -> {
            RecommendationCache recommendationsData = APIRecommendations.getData();
            if(recommendationsData == null || recommendationsData.data == null) {
                Log.e(TAG, "onCreate: APIRecommendations data is null");
            } else {
                Log.d(TAG, "onCreate: APIRecommendations got data!");

                //Grabs the linear layout where the APIRecommendations are stored
                LinearLayout navigateRootLayout = activity.findViewById(R.id.content_recommendations);

                //inflates the custom APIRecommendations row (where the 'tiles' are kept)
                View recommendationRowView = View.inflate(activity, R.layout.recomendations_row, null);

                //sets text for the APIRecommendations row image
                ((TextView)(recommendationRowView.findViewById(R.id.recommendations_tile_text)))
                        .setText(channelItem.attributes.fullName);

                //Adds the APIRecommendations row to the parent layout
                navigateRootLayout.addView(recommendationRowView);

                //gets the recycler view
                RecyclerView tmpView = recommendationRowView.findViewById(R.id.recommendations_tile_recycler_view);
                if (recommendationsData.data.items.size() == 0) {
                    //Hides the recycler view and says its empty
                    tmpView.setVisibility(View.GONE);

                    //gets empty string
                    String emptyRecommendation = activity.getString(R.string.recommendations_empty_string)
                            .concat(" ").concat(channelItem.attributes.fullName);
                    TextView recommendationsEmptyText =
                            recommendationRowView.findViewById(R.id.recommendations_empty_text);
                    recommendationsEmptyText.setText(emptyRecommendation);
                    recommendationsEmptyText.setVisibility(View.VISIBLE);
                } else {
                    //The rest inflates the other views of the row and sets upt he adapters.
                    LinearLayoutManager tmpLinearLayout = new LinearLayoutManager(activity,
                            LinearLayoutManager.HORIZONTAL,
                            false);
                    tmpView.setLayoutManager(tmpLinearLayout);
                    RecommendationsAdapter tmpRecommendationsAdapter = new RecommendationsAdapter(
                            ((APIRecommendations.RecommendationsJSON)recommendationsData.data).items,
                            activity,
                            backgroundAudioService);
                    tmpView.setAdapter(tmpRecommendationsAdapter);
                }
            }
        };
        APIRecommendations.updateData(recommendationsApiDataResponse);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
