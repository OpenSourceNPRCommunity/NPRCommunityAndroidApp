package com.nprcommunity.npronecommunity.Layout.Adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nprcommunity.npronecommunity.API.APIRecommendations;
import com.nprcommunity.npronecommunity.API.APIAggregations;
import com.nprcommunity.npronecommunity.Layout.Fragment.ContentRecommendationsFragment;
import com.nprcommunity.npronecommunity.Layout.Fragment.TileDialogFragment;
import com.nprcommunity.npronecommunity.R;
import com.nprcommunity.npronecommunity.Store.CacheStructures.AggregationsCache;
import com.nprcommunity.npronecommunity.Store.FileCache;

import java.io.FileInputStream;
import java.util.List;
import java.util.Observable;

public class RecommendationsAdapter extends RecyclerView.Adapter<RecommendationsAdapter.ViewHolder> {
    private String TAG = "RecommendationsAdapter";
    private List<APIRecommendations.ItemJSON> itemsJSON;
    private Context context;
    private Activity activity;
    private ContentRecommendationsFragment.OnFragmentInteractionListener onFragmentInteractionListener;
    private Observable tileObservable;

    public RecommendationsAdapter(List<APIRecommendations.ItemJSON> itemsJSON, Activity activity,
                                  ContentRecommendationsFragment.OnFragmentInteractionListener onFragmentInteractionListener,
                                  Observable tileObservable) {
        this.itemsJSON = itemsJSON;
        this.context = activity.getApplicationContext();
        this.activity = activity;
        this.onFragmentInteractionListener = onFragmentInteractionListener;
        this.tileObservable = tileObservable;
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
        APIRecommendations.ItemJSON itemJSON = itemsJSON.get(position);

        holder.view.setOnClickListener((View view) -> {
            Log.i(TAG, "onCreateViewHolder: selected:" + itemJSON.attributes.type);

            if (itemJSON.attributes.rating.hasAffiliations()) {
                //has affiliations load it
                APIAggregations APIAggregations = new APIAggregations(
                    context,
                    itemJSON.attributes.rating.getAffiliations().get(0)
                );

                //get aggregation data for creating the display
                APIAggregations.updateData(() -> {
                    AggregationsCache aggregationsData = APIAggregations.getData();
                    if (aggregationsData == null || aggregationsData.data == null) {
                        Log.e(TAG, "onBindViewHolder: APIAggregations data is null");
                        //todo display error with toast
                    } else {
                        Log.d(TAG, "onBindViewHolder: APIAggregations got data!");

                        //create dialog to add audio
                        TileDialogFragment tileDialogFragment = TileDialogFragment.newInstance(
                                itemJSON,
                                onFragmentInteractionListener,
                                aggregationsData.data,
                                tileObservable
                        );
                        if (tileDialogFragment == null) {
                            Log.e(TAG, "onBindViewHolder: error null");
                            //TODO make toast or something or error
                        } else {
                            tileDialogFragment.show(activity.getFragmentManager(), "tiledialog");
                        }
                    }
                });
            } else {
                //create dialog to add audio
                TileDialogFragment tileDialogFragment = TileDialogFragment.newInstance(
                        itemJSON,
                        onFragmentInteractionListener,
                        null,
                        tileObservable);
                tileDialogFragment.show(activity.getFragmentManager(), "tiledialog");
            }
        });

        holder.textView.setText(itemJSON.attributes.title);

        holder.imageView.setImageResource(R.drawable.blank_image);

        if (itemJSON.links.hasImage()) {
            //only run if has an image
            FileCache fileCache = FileCache.getInstances(this.context);
            // Loads image async, checks storage, if not found, downloads, saves and then returns the input stream
            fileCache.getImage(
                    itemJSON.links.getValidImage().href,
                    (Bitmap bitmap) -> {
                        if (bitmap == null) {
                            Log.e(TAG, "onBindViewHolder: failed to get image. Check out other logs");
                        } else {
                            activity.runOnUiThread(() -> {
                                holder.imageView.setImageBitmap(bitmap);
                            });
                        }
                    },
                    (int progress, int total, int speed) -> {
                        //this is progress for image loading for recommendation
                        Log.d(TAG, "onBindViewHolder: progress loading recommendation image: "
                            + itemJSON.links.getValidImage().href + " at "
                            + " progress [" + progress + "] total [" + total + "] "
                            + " percent [" + ((double)progress)/((double)total));
                    }
            );
        }
    }

    @Override
    public int getItemCount() {
        return itemsJSON.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public final TextView textView;
        public final ImageView imageView;
        public final View view;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            textView = view.findViewById(R.id.recommendation_tile_text_view);
            imageView = view.findViewById(R.id.recommendation_tile_image_view);
        }
    }
}
