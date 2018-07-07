package com.nprcommunity.npronecommunity.Layout.Adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nprcommunity.npronecommunity.API.APIChannels;
import com.nprcommunity.npronecommunity.R;

import java.io.InputStream;

public class ChannelsAdapter extends RecyclerView.Adapter<ChannelsAdapter.ViewHolder>{
    private String TAG = "ChannelsAdapter";
    private APIChannels.ChannelsJSON channelsJSON;

    public ChannelsAdapter(APIChannels.ChannelsJSON channelsJSON) {
        this.channelsJSON = channelsJSON;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recommendation_tile, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(channelsJSON.items.get(position).attributes.fullName);
    }

    @Override
    public int getItemCount() {
        return channelsJSON.items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView textView;
        public ImageView imageView;

        public ViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.media_player_description);
        }
    }
}
