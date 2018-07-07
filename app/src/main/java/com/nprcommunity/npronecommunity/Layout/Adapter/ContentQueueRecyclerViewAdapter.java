package com.nprcommunity.npronecommunity.Layout.Adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nprcommunity.npronecommunity.API.APIRecommendations;
import com.nprcommunity.npronecommunity.Background.MediaQueueManager;
import com.nprcommunity.npronecommunity.Layout.Callback.ContentQueuePlayingListener;
import com.nprcommunity.npronecommunity.Layout.Callback.ItemTouchHelperListener;
import com.nprcommunity.npronecommunity.Layout.Fragment.ContentQueueFragment.OnListFragmentInteractionListener;
import com.nprcommunity.npronecommunity.R;
import com.nprcommunity.npronecommunity.Store.FileCache;

import java.io.FileInputStream;

public class ContentQueueRecyclerViewAdapter
        extends RecyclerView.Adapter<ContentQueueRecyclerViewAdapter.ViewHolder>
        implements ItemTouchHelperListener {

    private final OnListFragmentInteractionListener listener;
    private final Context context;
    private final static String TAG = "CONTENTQUEUERECYCLER";
    private MediaQueueManager mediaQueueManager;
    private ContentQueuePlayingListener contentQueuePlayingListener;
    private Activity activity;

    public ContentQueueRecyclerViewAdapter(
            OnListFragmentInteractionListener listener,
            ContentQueuePlayingListener contentQueuePlayingListener,
            Context context,
            Activity activity) {
        this.listener = listener;
        this.context = context;
        this.mediaQueueManager = MediaQueueManager.getInstance(context);
        this.contentQueuePlayingListener = contentQueuePlayingListener;
        this.activity = activity;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.content_queue_fragment_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        APIRecommendations.ItemJSON tmpQueueItem = mediaQueueManager.getQueueTrack(position);
        holder.setSkippable(tmpQueueItem.attributes.skippable);

        holder.titleView.setText(tmpQueueItem.attributes.audioTitle);
        holder.hrefView.setText(tmpQueueItem.href);

        holder.imageView.setImageResource(R.drawable.blank_image);
        if (tmpQueueItem.links.hasImage()) {
            //only run if has an image
            FileCache fileCache = FileCache.getInstances(this.context);
            // Loads image async, checks storage, if not found, downloads, saves and then returns the input stream
            fileCache.getImage(
                    tmpQueueItem.links.getValidImage().href,
                    context,
                    (FileInputStream fileInputStream, String url) -> {
                        if (fileInputStream == null) {
                            Log.e(TAG, "onBindViewHolder: failed to get image. Check out other logs");
                        } else {
                            activity.runOnUiThread(() -> {
                                holder.imageView.setImageBitmap(BitmapFactory.decodeStream(fileInputStream));
                            });
                        }
                    },
                    (int progress, int total, int speed) -> {
                        Log.d(TAG, "nextMediaHelper: progress loading content queue image: "
                            + tmpQueueItem.links.getValidImage().href + " at "
                            + " progress [" + progress + "] total [" + total + "] "
                            + " percent [" + ((double)progress)/((double)total));
                    }
            );
        }

        // setup progress bar
        if (tmpQueueItem.links.audio.get(0).progressTracker.getPercentage() == 1.0) {
            //finished downloading setup the view
            holder.progressSpeed.setVisibility(View.GONE);
            //TODO replace all 100% Downloaded (there are two of them) with R.string...
            String percentage = "100% Downloaded";
            holder.progressPercent.setText(percentage);
            holder.progressBar.setVisibility(View.GONE);
        }

        //setup on close
        if (tmpQueueItem.attributes.skippable) {
            //only enable if skippable
            holder.closeImageButton.setOnClickListener((View v) -> {
                int removedIndex = contentQueuePlayingListener.remove(tmpQueueItem);
                if (removedIndex >= 0) {
                    notifyItemRemoved(removedIndex);
                }
            });
        } else {
            //hide button else
            holder.closeImageButton.setVisibility(View.GONE);
        }

        holder.view.setOnClickListener((View v) -> {
            if (null != listener) {
                // Notify the active callbacks interface (the activity, if the
                // fragment is attached to one) that an item has been selected.
                listener.onListFragmentInteraction(tmpQueueItem.href);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mediaQueueManager.queueSize();
    }

    public void removeItem(APIRecommendations.ItemJSON itemJSON) {
        int position = contentQueuePlayingListener.remove(itemJSON);
        if (position >= 0) {
            notifyItemRemoved(position);
        } else {
            Log.e(TAG, "removeItem: could not remove item [" + itemJSON + "]");
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View view;
        public final TextView titleView;
        public final ImageView imageView;
        public final TextView hrefView;
        public final TextView progressSpeed;
        public final ProgressBar progressBar;
        public final TextView progressPercent;
        public final ImageButton closeImageButton;
        private boolean isSkippable;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            titleView = view.findViewById(R.id.queue_rec_title);
            imageView = view.findViewById(R.id.queue_rec_image);
            hrefView = view.findViewById(R.id.queue_rec_href);
            progressSpeed = view.findViewById(R.id.queue_progress_speed);
            progressBar = view.findViewById(R.id.queue_progress_bar);
            progressPercent = view.findViewById(R.id.queue_progress_percent);
            closeImageButton = view.findViewById(R.id.queue_close_image_button);
        }

        public boolean isSkippable() {
            return isSkippable;
        }

        public void setSkippable(boolean isSkippable) {
            this.isSkippable = isSkippable;
        }

        @Override
        public String toString() {
            return super.toString() + " '" + titleView.getText() + "'";
        }
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemDismiss(int position) {
        contentQueuePlayingListener.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public void onItemDrop(int fromPosition, int toPosition) {
        contentQueuePlayingListener.swap(fromPosition, toPosition);
    }
}