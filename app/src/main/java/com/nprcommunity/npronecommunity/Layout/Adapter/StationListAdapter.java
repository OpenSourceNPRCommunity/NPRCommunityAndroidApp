package com.nprcommunity.npronecommunity.Layout.Adapter;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nprcommunity.npronecommunity.API.APIAggregations;
import com.nprcommunity.npronecommunity.API.APISearch;
import com.nprcommunity.npronecommunity.API.APIStation;
import com.nprcommunity.npronecommunity.API.StationSender;
import com.nprcommunity.npronecommunity.Background.BackgroundAudioService;
import com.nprcommunity.npronecommunity.Layout.Fragment.TileDialogFragment;
import com.nprcommunity.npronecommunity.R;
import com.nprcommunity.npronecommunity.Store.FileCache;

import java.io.FileInputStream;
import java.util.List;

public class StationListAdapter extends ArrayAdapter<APIStation.ItemJSON> {
    private static final String TAG = "StationListAdapter";
    private Activity activity;
    private String token;

    public StationListAdapter(Activity activity,
                              int resource,
                              List<APIStation.ItemJSON> items,
                              String token) {
        super(activity, resource, items);
        this.activity = activity;
        this.token = token;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        APIStation.ItemJSON item = super.getItem(position);
        if (item == null) {
            Log.e(TAG, "getView: item at pos [" + position + "] is null");
            return convertView;
        }

        //create view
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());

        convertView = layoutInflater.inflate(R.layout.station_item, null);

        convertView.setOnClickListener((View view) -> {
            Integer stationGuid;
            try {
                stationGuid = Integer.parseInt(item.attributes.orgId);
            } catch (NumberFormatException e) {
                Log.e(TAG, "getView: could not parse int [" + item.attributes.guid + "]", e);
                return;
            }

            //send the station update
            new StationSender(
                    token,
                    stationGuid
            ).sendAsyncStation((boolean success)-> {
                //send notification to ui
                activity.runOnUiThread(()->{
                        AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
                        alertDialog.setTitle("Alert");
                        if (success) {
                            alertDialog.setMessage("Success! Updated station!");
                        } else {
                            alertDialog.setMessage("Failure! Did not update station!");
                        }
                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                                (dialog, which) -> dialog.dismiss());
                        alertDialog.show();
                });
            });
        });

        //setup dialog shows info
        ((TextView)convertView.findViewById(R.id.station_text_view)).setText(
                item.attributes.brand.name
        );

        //sets default image
        ImageView imageView = convertView.findViewById(R.id.station_image_view);
        imageView.setImageResource(R.drawable.blank_image);

        if (item.links.hasImage()) {
            //only run if has an image
            FileCache fileCache = FileCache.getInstances(activity);
            // Loads image async, checks storage, if not found, downloads, saves and then returns the input stream
            fileCache.getImage(
                    item.links.getValidBrand().href,
                    (Bitmap bitmap) -> {
                        if (bitmap == null) {
                            Log.e(TAG, "getView: failed to get image. Check out other logs");
                        } else {
                            activity.runOnUiThread(() -> {
                                imageView.setImageBitmap(bitmap);
                            });
                        }
                    },
                    (int progress, int total, int speed) -> {
                        //this is image load for
                        Log.d(TAG, "getView: progress loading brand image: "
                                + item.links.getValidBrand().href + " at "
                                + " progress [" + progress + "] total [" + total + "] "
                                + " percent [" + ((double)progress)/((double)total));
                    }
            );
        }

        return convertView;
    }
}