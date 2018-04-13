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

import com.nprcommunity.npronecommunity.API.Channels;
import com.nprcommunity.npronecommunity.R;

import java.io.InputStream;

public class ChannelsAdapter extends RecyclerView.Adapter<ChannelsAdapter.ViewHolder>{
    private String TAG = "ChannelsAdapter";
    private Channels.ChannelsJSON channelsJSON;

    public ChannelsAdapter(Channels.ChannelsJSON channelsJSON) {
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
//        new DownloadImageTask(holder.imageView)
//                .execute(channelsJSON.items.get(position).);
//        URL url = null;
//        try {
//            url = new URL(channelsJSON.items.get(position).href);
//            Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
//            holder.imageView.setImageBitmap(bmp);
//            holder.imageView.setBackground
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//            Log.e(TAG,"onBindViewHolder: wrong url for image.");
//        } catch (IOException e) {
//            e.printStackTrace();
//            Log.e(TAG,"onBindViewHolder: IO execption");
//        }
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
            textView = view.findViewById(R.id.textView);
//            imageView = view.findViewById(R.id.image_view);
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
}
