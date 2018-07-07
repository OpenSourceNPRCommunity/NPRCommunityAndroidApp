package com.nprcommunity.npronecommunity.Layout.Adapter;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import com.nprcommunity.npronecommunity.API.APIChannels;
import com.nprcommunity.npronecommunity.R;

import java.util.List;

public class ChannelSelectAdapter extends ArrayAdapter<APIChannels.ItemJSON> {
    public static final String TAG = "ChannelSelectAdapter";

    public ChannelSelectAdapter(Context context, int resource, List<APIChannels.ItemJSON> items) {
        super(context, resource, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //create view
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(R.layout.channel_checked_item, null);
        }

        //setup the checked item
        APIChannels.ItemJSON item = super.getItem(position);
        if (item == null) {
            Log.e(TAG, "getView: item at pos [" + position + "] is null");
            return convertView;
        }
        ConstraintLayout constraintLayout = (ConstraintLayout) convertView;
        CheckedTextView checkedTextView = (CheckedTextView)
                constraintLayout.getViewById(R.id.channels_selected_text_view);
        checkedTextView.setText(item.attributes.fullName);
        boolean isADefaultChannel = APIChannels.DEFAULT_CHANNELS_MAP.containsKey(item.attributes.id);
        checkedTextView.setChecked(item.isChecked || isADefaultChannel);
        if (isADefaultChannel) {
            //if is a default channel then disable checkable
            checkedTextView.setEnabled(false);
        } else {
            checkedTextView.setEnabled(true);
        }
        checkedTextView.setOnClickListener((View v) -> {
            CheckedTextView tmpCheckedView = ((CheckedTextView)v);
            tmpCheckedView.setChecked(!tmpCheckedView.isChecked());
            item.isChecked = tmpCheckedView.isChecked();
        });

        return convertView;
    }
}
