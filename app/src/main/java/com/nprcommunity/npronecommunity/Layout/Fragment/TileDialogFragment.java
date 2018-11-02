package com.nprcommunity.npronecommunity.Layout.Fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nprcommunity.npronecommunity.API.APIRecommendations;
import com.nprcommunity.npronecommunity.API.APIAggregations;
import com.nprcommunity.npronecommunity.Background.MediaQueueManager;
import com.nprcommunity.npronecommunity.R;
import com.nprcommunity.npronecommunity.Store.FileCache;

import java.io.FileInputStream;
import java.util.Observable;
import java.util.Observer;

public class TileDialogFragment extends DialogFragment implements Observer {

    private ContentRecommendationsFragment.OnFragmentInteractionListener onFragmentInteractionListener;
    private static String ARG_QUEUE_ITEM = "QUEUE_ITEM",
                            ARG_AFFILIATIONS = "AFFILIATIONS",
                            ARG_HAS_AFFILIATIONS = "HAS_AFFILIATIONS";
    private static String TAG = "TILEDIALOGFRAGMENT";
    private LinearLayout tileRecommendationsList;
    private boolean hasAffiliations;
    private View dialogView;
    private Observable tileObservable;

    public static TileDialogFragment newInstance(@NonNull APIRecommendations.ItemJSON itemJSON,
                                                 @NonNull ContentRecommendationsFragment.OnFragmentInteractionListener onFragmentInteractionListener,
                                                 APIAggregations.AggregationJSON aggregationJSON,
                                                 Observable tileObservable) {

        TileDialogFragment fragment = new TileDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_QUEUE_ITEM, itemJSON);
        if (aggregationJSON != null) {
            args.putSerializable(ARG_AFFILIATIONS, aggregationJSON);
            args.putBoolean(ARG_HAS_AFFILIATIONS, true);
        } else {
            args.putBoolean(ARG_HAS_AFFILIATIONS, false);
        }
        fragment.setFragment(onFragmentInteractionListener, tileObservable);

        fragment.setArguments(args);
        return fragment;
    }

    private void setFragment(ContentRecommendationsFragment.OnFragmentInteractionListener onFragmentInteractionListener,
        Observable tileObservable) {
        this.onFragmentInteractionListener = onFragmentInteractionListener;
        this.tileObservable = tileObservable;
        tileObservable.addObserver(this);
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = null;
        Bundle arguments = getArguments();
        if (arguments != null) {
            APIRecommendations.ItemJSON queueItem = (APIRecommendations.ItemJSON) arguments.getSerializable(ARG_QUEUE_ITEM);
            APIAggregations.AggregationJSON aggregationsJSON = null;
            boolean hasAffiliations = arguments.getBoolean(ARG_HAS_AFFILIATIONS);
            if (hasAffiliations) {
                //has affiliations create the correct dialog
                aggregationsJSON = (APIAggregations.AggregationJSON) arguments.getSerializable(ARG_AFFILIATIONS);
                if (aggregationsJSON != null && aggregationsJSON.items.size() > 0) {
                    //successfully has the correct affiliations
                    dialog = setupAffiliationsDialog(aggregationsJSON);
                } else {
                    Log.e(TAG, "onCreateDialog: should have had affiliations but doesnt");
                    if (queueItem != null) {
                        Log.e(TAG, "onCreateDialog: affiliations should be " + queueItem);
                    }
                }
            }
            if (queueItem != null && dialog == null) {
                //go in here if the dialog was not set and there is a queue item
                //setups dialog with shows information
                dialog = setupNonAffiliationsDialog(queueItem);
            }
        } else {
            Log.e(TAG, "onCreateDialog: error getting args");
        }
        if (dialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.error_title)
                    .setMessage(R.string.error_message)
                    .setNegativeButton(R.string.dialog_row_close, (dialogInterface, i) -> {
                        dialogInterface.cancel();
                    });
            dialog = builder.create();
        }
        return dialog;
    }

    private Dialog setupNonAffiliationsDialog(@NonNull APIRecommendations.ItemJSON queueItem) {
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();

        dialogView = layoutInflater.inflate(R.layout.dialog_single_audio, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogView);
        Dialog dialog = builder.create();

        setupNonAffiliationsComponents(dialogView, queueItem, getActivity(), onFragmentInteractionListener);

        dialogView.findViewById(R.id.dialog_single_button_close).setOnClickListener((View v) -> {
            dialog.cancel();
        });

        return dialog;
    }

    public static void setupNonAffiliationsComponents(@NonNull View view,
                                                      @NonNull APIRecommendations.ItemJSON queueItem,
                                                      @NonNull Activity activity,
                                                      @NonNull ContentRecommendationsFragment.OnFragmentInteractionListener onFragmentInteractionListener) {
        //setup unique href id for this row item
        ((TextView)view.findViewById(R.id.dialog_single_href_id))
                .setText(queueItem.href);

        //setup title and description
        TextView textView = view.findViewById(R.id.dialog_single_title);
        textView.setText(queueItem.attributes.title);
        textView = view.findViewById(R.id.dialog_single_description);
        textView.setText(queueItem.attributes.description);

        //set add to queue button
        setAddToQueueButton(
                view.findViewById(R.id.dialog_single_button_add_to_queue),
                queueItem,
                activity,
                onFragmentInteractionListener
        );

        //set play now button
        setPlayNowButton(
                view.findViewById(R.id.dialog_single_button_play_now),
                queueItem,
                !queueItem.href.equals(onFragmentInteractionListener.getMediaHref()),
                view.findViewById(R.id.dialog_single_button_add_to_queue),
                activity,
                onFragmentInteractionListener
        );
    }

    private Dialog setupAffiliationsDialog(APIAggregations.AggregationJSON aggregationJSON) {
        hasAffiliations = true;

        LayoutInflater layoutInflater = getActivity().getLayoutInflater();

        //dialog
        dialogView = layoutInflater.inflate(R.layout.dialog_multi_show, null);
        Dialog dialog = new Dialog(
            this.getActivity(),
            android.R.style.Theme_Light_NoTitleBar_Fullscreen
        );
        dialog.setContentView(dialogView);

        //recommendations list
        tileRecommendationsList = dialogView.findViewById(R.id.dialog_multi_show_list);

        setupAffiliationsComponents(dialogView, aggregationJSON, getActivity(),
                tileRecommendationsList, onFragmentInteractionListener);

        return dialog;
    }

    public static void setupAffiliationsComponents(@NonNull View view,
                                                   @NonNull APIAggregations.AggregationJSON aggregationJSON,
                                                   @NonNull Activity activity,
                                                   @NonNull LinearLayout recommendationsList,
                                                   @NonNull ContentRecommendationsFragment.OnFragmentInteractionListener onFragmentInteractionListener) {
        //setup dialog shows info
        ((TextView)view.findViewById(R.id.dialog_multi_show_title)).setText(
                aggregationJSON.attributes.getTitle()
        );

        ((TextView)view.findViewById(R.id.dialog_multi_show_description)).setText(
                aggregationJSON.attributes.getDescription()
        );

        ImageView imageView = view.findViewById(R.id.dialog_multi_show_image);
        imageView.setImageResource(R.drawable.blank_image);

        if (aggregationJSON.links.hasImage()) {
            //only run if has an image
            FileCache fileCache = FileCache.getInstances(activity);
            // Loads image async, checks storage, if not found, downloads, saves and then returns the input stream
            fileCache.getImage(
                    aggregationJSON.links.getValidImage().href,
                    (Bitmap bitmap) -> {
                        if (bitmap == null) {
                            Log.e(TAG, "onBindViewHolder: failed to get image. Check out other logs");
                        } else {
                            activity.runOnUiThread(() -> {
                                imageView.setImageBitmap(bitmap);
                            });
                        }
                    },
                    (int progress, int total, int speed) -> {
                        //this is image load for
                        Log.d(TAG, "onBindViewHolder: progress loading aggregation image: "
                                + aggregationJSON.links.getValidImage().href + " at "
                                + " progress [" + progress + "] total [" + total + "] "
                                + " percent [" + ((double)progress)/((double)total));
                    }
            );
        }

        //go through all recommendations
        for (APIRecommendations.ItemJSON tmpQueueItem : aggregationJSON.items) {
            View recommendation = activity.getLayoutInflater().inflate(
                    R.layout.dialog_multi_row,
                    recommendationsList,
                    false);

            //setup unique href id for this row item
            ((TextView)recommendation.findViewById(R.id.dialog_row_href_id))
                    .setText(tmpQueueItem.href);

            //setup title and description
            ((TextView)recommendation.findViewById(R.id.dialog_row_title)).setText(
                    tmpQueueItem.attributes.title
            );

            //setup unique href id for this row item
            ((TextView)recommendation.findViewById(R.id.dialog_row_href_id))
                    .setText(tmpQueueItem.href);
            ((TextView)recommendation.findViewById(R.id.dialog_row_description)).setText(
                    tmpQueueItem.attributes.description
            );
            recommendationsList.addView(recommendation);

            //set add to queue button
            setAddToQueueButton(
                    recommendation.findViewById(R.id.dialog_row_button_add_to_queue),
                    tmpQueueItem,
                    activity,
                    onFragmentInteractionListener
            );

            //set play now button
            setPlayNowButton(
                    recommendation.findViewById(R.id.dialog_row_button_play_now),
                    tmpQueueItem,
                    !tmpQueueItem.href.equals(onFragmentInteractionListener.getMediaHref()),
                    recommendation.findViewById(R.id.dialog_row_button_add_to_queue),
                    activity,
                    onFragmentInteractionListener
            );
        }
    }

    private static void setAddToQueueButton(Button addToQueue,
                                            APIRecommendations.ItemJSON queueItem,
                                            Activity activity,
                                            ContentRecommendationsFragment.OnFragmentInteractionListener onFragmentInteractionListener) {
        //setup the queue if enabled
        setAddToQueueEnabled(addToQueue, queueItem.href, activity);

        addToQueue.setOnClickListener((View v) -> {
            onFragmentInteractionListener.addToQueue(queueItem);
            Button button = (Button) v;
            button.setEnabled(false);
            String checked = activity.getString(R.string.checkmark)
                    .concat(activity.getString(R.string.add_to_queue));
            button.setText(checked);
            button.getBackground().setColorFilter(ContextCompat.getColor(
                activity,
                R.color.colorAlreadyPartOfQueue
            ), PorterDuff.Mode.MULTIPLY);
        });
    }

    private static void setAddToQueueEnabled(Button addToQueue,
                                            @NonNull String href,
                                            Activity activity) {
        String addToQueueText;
        //get the count of recommendations so far to add checkmarks
        if (existsInQueue(href, activity)) {
            addToQueueText = activity.getString(R.string.checkmark) +
                    activity.getString(R.string.add_to_queue);
            addToQueue.getBackground().setColorFilter(
                    activity.getResources().getColor(R.color.colorAlreadyPartOfQueue),
                    PorterDuff.Mode.MULTIPLY);
            addToQueue.setEnabled(false);
        } else {
            addToQueueText = activity.getString(R.string.add_to_queue);
            addToQueue.getBackground().clearColorFilter();
            addToQueue.setEnabled(true);
        }

        //Set up the add to queue check mark
        addToQueue.setText(addToQueueText);
    }

    private static void setPlayNowButton(Button playNow,
                                         APIRecommendations.ItemJSON queueItem,
                                         boolean enabled,
                                         Button addToQueue,
                                         Activity activity,
                                         ContentRecommendationsFragment.OnFragmentInteractionListener onFragmentInteractionListener) {
        playNow.setOnClickListener((View v) -> {
            onFragmentInteractionListener.playMediaNow(queueItem);
            playNow.setEnabled(false);
            addToQueue.setEnabled(false);
        });
        playNow.setEnabled(enabled);
    }

    private static boolean existsInQueue(@NonNull String href,
                                        @NonNull Activity activity) {
        //get the count of recommendations so far to add checkmarks
        for (MediaSessionCompat.QueueItem itemJSON :
                MediaQueueManager.getInstance(activity).getMediaQueue()) {
            if (href.equals(itemJSON.getDescription().getMediaId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        tileObservable.deleteObserver(this);
    }

    @Override
    public void update(Observable o, Object arg) {
        final String lastMediaHref = (String)arg;
        final String currentTileHref = onFragmentInteractionListener.getMediaHref();
        if (hasAffiliations) {
            final int childCount = tileRecommendationsList.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View v = tileRecommendationsList.getChildAt(i);
                Button playNow = v.findViewById(R.id.dialog_row_button_play_now);
                Button addToQueue = v.findViewById(R.id.dialog_row_button_add_to_queue);
                String rowHref = ((TextView)v.findViewById(R.id.dialog_row_href_id))
                        .getText().toString();

                //setup the queue if enabled
                setAddToQueueEnabled(addToQueue, rowHref, getActivity());

                if (rowHref.equals(currentTileHref)) {
                    //media found disable play
                    playNow.setEnabled(false);
                } else {
                    playNow.setEnabled(true);
                }
            }
        } else {
            View v = dialogView;
            Button playNow = v.findViewById(R.id.dialog_single_button_play_now);
            Button addToQueue = v.findViewById(R.id.dialog_single_button_add_to_queue);
            String singleHref = ((TextView)v.findViewById(R.id.dialog_single_href_id))
                    .getText().toString();

            //setup the queue if enabled
            setAddToQueueEnabled(addToQueue, singleHref, getActivity());

            if (singleHref.equals(currentTileHref)) {
                //media found disable play
                playNow.setEnabled(false);
            } else {
                playNow.setEnabled(true);
            }
        }
    }
}
