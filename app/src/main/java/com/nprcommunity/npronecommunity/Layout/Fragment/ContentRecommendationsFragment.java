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
import com.nprcommunity.npronecommunity.API.Channels;
import com.nprcommunity.npronecommunity.API.Recommendations;
import com.nprcommunity.npronecommunity.R;
import com.nprcommunity.npronecommunity.Layout.Adapter.RecommendationsAdapter;
import com.nprcommunity.npronecommunity.Store.ChannelCache;
import com.nprcommunity.npronecommunity.Store.RecommendationCache;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ContentRecommendationsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ContentRecommendationsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ContentRecommendationsFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private static String TAG = "LAYOUT.FRAGMENT.CONTENTRECOMMENDATIONSFRAGMENT";

    private OnFragmentInteractionListener mListener;

    public ContentRecommendationsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ContentRecommendationsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ContentRecommendationsFragment newInstance(String param1, String param2) {
        ContentRecommendationsFragment fragment = new ContentRecommendationsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
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
        Channels channels = new Channels(getContext());
        APIDataResponse channelApiDataResponse = () -> {
            ChannelCache channelsData = channels.getData();
            if(channelsData == null || channelsData.data == null) {
                Log.e(TAG, "onCreate: Channels data is null");
                //TODO put error up on screen
            } else {
                Log.d(TAG, "onCreate: Channels got data!");

                int i = 0;
                for(Channels.ItemJSON channelItem : ((Channels.ChannelsJSON)channelsData.data).items) {
                    initRecommendationRow(channelItem, getActivity());
                    i++;
                    if(i > 5) {
                        break;
                    }
                }
            }
        };
        channels.updateData(channelApiDataResponse);
    }

    private static void initRecommendationRow(Channels.ItemJSON channelItem, Activity activity) {
        Recommendations recommendations = new Recommendations(activity, channelItem.href);
        APIDataResponse recommendationsApiDataResponse = () -> {
            RecommendationCache recommendationsData = recommendations.getData();
            if(recommendationsData == null || recommendationsData.data == null) {
                Log.e(TAG, "onCreate: APIRecommendations data is null");
            } else {
                Log.d(TAG, "onCreate: APIRecommendations got data!");

                //Grabs the linear layout where the recommendations are stored
                LinearLayout navigateRootLayout = activity.findViewById(R.id.content_recommendations);

                //inflates the custom recommendations row (where the 'tiles' are kept)
                View recommendationRowView = View.inflate(activity, R.layout.recomendations_row, null);

                //sets text for the recommendations row image
                ((TextView)(recommendationRowView.findViewById(R.id.recommendations_tile_text)))
                        .setText(channelItem.attributes.fullName);

                //Adds the recommendations row to the parent layout
                navigateRootLayout.addView(recommendationRowView);

                //The rest inflates the other views of the row and sets upt he adapters.
                RecyclerView tmpView = recommendationRowView.findViewById(R.id.recommendations_tile_recycler_view);
                LinearLayoutManager tmpLinearLayout = new LinearLayoutManager(activity,
                        LinearLayoutManager.HORIZONTAL,
                        false);
                tmpView.setLayoutManager(tmpLinearLayout);
                RecommendationsAdapter tmpRecommendationsAdapter = new RecommendationsAdapter(
                        ((Recommendations.RecommendationsJSON)recommendationsData.data).items,
                        activity);
                tmpView.setAdapter(tmpRecommendationsAdapter);
            }
        };
        recommendations.updateData(recommendationsApiDataResponse);
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
