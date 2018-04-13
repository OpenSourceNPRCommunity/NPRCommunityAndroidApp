package com.nprcommunity.npronecommunity.Layout.Adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nprcommunity.npronecommunity.API.Recommendations;
import com.nprcommunity.npronecommunity.R;
import com.nprcommunity.npronecommunity.Store.FileCache;
import com.nprcommunity.npronecommunity.Store.DownloadMediaTask;

import java.util.List;

public class RecommendationsAdapter extends RecyclerView.Adapter<RecommendationsAdapter.ViewHolder> {
    private String TAG = "RecommendationsAdapter";
    private List<Recommendations.ItemJSON> itemsJSON;
    private Context context;
    private int IMAGE_WIDTH = 120,
                    IMAGE_HEIGHT = 120;

    public RecommendationsAdapter(List<Recommendations.ItemJSON> itemsJSON, Context context) {
        this.itemsJSON = itemsJSON;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recommendation_tile, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recommendations.ItemJSON itemJSON = itemsJSON.get(position);
        String text = "";
        if(itemJSON.attributes == null) {
            //TODO change error message
            text = "Error Loading: Contact Support";
        } else {
            text = itemJSON.attributes.title;
        }
        holder.textView.setText(text);
        if(itemJSON.links == null || itemJSON.links.image == null || itemJSON.links.image.size() == 0) {
            //TODO error image
            Log.e(TAG, String.format("onBindViewHolder: error href: %s", itemJSON.href));
            holder.imageView.setBackgroundColor(Color.BLACK);
        } else {
            new DownloadMediaTask(context,
                    FileCache.Type.IMAGE,
                    (fileInputStream)->{
                        if(fileInputStream == null) {
                            //failed to get data, use default image
                            //TODO default image place here
                            Log.e(TAG,
                                    String.format("onBindViewHolder: error loading: %s",
                                            itemJSON.links.image.get(0).href));
                            holder.imageView.setBackgroundColor(Color.BLACK);
                        } else {
                            holder.imageView.setImageBitmap(
                                    Bitmap.createScaledBitmap(BitmapFactory.decodeStream(fileInputStream),
                                            IMAGE_WIDTH,
                                            IMAGE_HEIGHT,
                                            false)
                            );
                        }
                    }).execute(itemJSON.links.image.get(0).href);
        }
    }

    @Override
    public int getItemCount() {
        return itemsJSON.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView textView;
        public ImageView imageView;

        public ViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.text_view);
            imageView = view.findViewById(R.id.image_view);
        }
    }
}
