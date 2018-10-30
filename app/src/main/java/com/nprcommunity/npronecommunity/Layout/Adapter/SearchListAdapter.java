package com.nprcommunity.npronecommunity.Layout.Adapter;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import com.nprcommunity.npronecommunity.API.APIAggregations;
import com.nprcommunity.npronecommunity.API.APISearch;
import com.nprcommunity.npronecommunity.Layout.Fragment.ContentRecommendationsFragment;
import com.nprcommunity.npronecommunity.Layout.Fragment.TileDialogFragment;
import com.nprcommunity.npronecommunity.R;

import java.util.List;

public class SearchListAdapter extends ArrayAdapter<APISearch.ItemJSON> {
    private static final String TAG = "SearchListAdapter";
    private Activity activity;
    private ContentRecommendationsFragment.OnFragmentInteractionListener listener;

    public SearchListAdapter(Activity activity,
                             int resource,
                             List<APISearch.ItemJSON> items,
                             ContentRecommendationsFragment.OnFragmentInteractionListener listener) {
        super(activity, resource, items);
        this.activity = activity;
        this.listener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        APISearch.ItemJSON item = super.getItem(position);
        if (item == null) {
            Log.e(TAG, "getView: item at pos [" + position + "] is null");
            return convertView;
        }

        //create view
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        switch (item.type) {
            case audio:
                convertView = layoutInflater.inflate(R.layout.search_audio, null);
                convertView.findViewById(R.id.dialog_single_button_close).setVisibility(View.GONE);

                TileDialogFragment.setupNonAffiliationsComponents(
                        convertView,
                        item.ifTypeAudio,
                        activity,
                        listener
                );
                break;
            case aggregation:
                convertView = layoutInflater.inflate(R.layout.search_aggregation, null);

                APIAggregations.AggregationJSON aggregationJSON = item.ifTypeAggregation;

                LinearLayout showList = convertView.findViewById(R.id.dialog_multi_show_list);

                TileDialogFragment.setupAffiliationsComponents(
                        convertView,
                        aggregationJSON,
                        activity,
                        showList,
                        listener
                );
                break;
        }

        return convertView;
    }
}