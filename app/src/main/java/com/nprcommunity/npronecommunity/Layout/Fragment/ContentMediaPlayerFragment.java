package com.nprcommunity.npronecommunity.Layout.Fragment;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.nprcommunity.npronecommunity.API.Shared;
import com.nprcommunity.npronecommunity.Background.BackgroundAudioService;
import com.nprcommunity.npronecommunity.R;
import com.nprcommunity.npronecommunity.Store.FileCache;
import com.nprcommunity.npronecommunity.Util;

import java.io.FileInputStream;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ContentMediaPlayerFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ContentMediaPlayerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ContentMediaPlayerFragment extends Fragment {

    private static String TAG = "CONTENTMEDIAPLAYERFRAGMENT";
    private OnFragmentInteractionListener mListener;
    private static BackgroundAudioService backgroundAudioService;

    private TextView mediaPlayerSeekBarTextLeft, mediaPlayerSeekBarTextCenter, mediaPlayerSeekBarTextRight, mediaPlayerTitle, mediaPlayerDesc;
    private SeekBar mediaPlayerSeekBar;
    private ImageView mediaPlayerImageView;

    public ContentMediaPlayerFragment() {}

    public static ContentMediaPlayerFragment newInstance(BackgroundAudioService bas) {
        backgroundAudioService = bas;
        ContentMediaPlayerFragment fragment = new ContentMediaPlayerFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.content_media_player_fragment, container, false);
        if (backgroundAudioService != null) {
            //setup fragment
            mediaPlayerSeekBarTextLeft = view.findViewById(R.id.media_player_seek_bar_text_left);
            mediaPlayerSeekBarTextCenter = view.findViewById(R.id.media_player_seek_bar_text_center);
            mediaPlayerSeekBarTextRight = view.findViewById(R.id.media_player_seek_bar_text_right);
            mediaPlayerSeekBar = view.findViewById(R.id.media_player_seek_bar);
            mediaPlayerSeekBar.setMax(backgroundAudioService.getMediaDuration());
            mediaPlayerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        mediaPlayerSeekBarTextCenter.setText(
                                Util.millisecondToHoursMinutesSeconds(progress)
                        );
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    mediaPlayerSeekBarTextCenter.setVisibility(View.VISIBLE);
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    backgroundAudioService.seekMedia(seekBar.getProgress());
                    setSeek(seekBar.getProgress(), false);
                    mediaPlayerSeekBarTextCenter.setVisibility(View.INVISIBLE);
                }
            });
            mediaPlayerTitle = view.findViewById(R.id.media_player_title);
            mediaPlayerDesc = view.findViewById(R.id.media_player_description);
            mediaPlayerImageView = view.findViewById(R.id.media_player_image_view);
            updateMedia();
        }
        return view;
    }

    public void setSeek(int millisecond, boolean updateSeekBar) {
        if (mediaPlayerSeekBarTextLeft != null) {
            mediaPlayerSeekBarTextLeft.setText(
                    Util.millisecondToHoursMinutesSeconds(
                            millisecond
                    )
            );
        }
        if (updateSeekBar) {
            if (mediaPlayerSeekBar != null && !mediaPlayerSeekBar.isPressed()) {
                    mediaPlayerSeekBar.setProgress(millisecond);
            }
        }
    }

    public void updateMedia() {
        if (backgroundAudioService == null) {
            Log.e(TAG, "updateMedia: backgroundaudioservice is null");
            return;
        }

        if (mediaPlayerSeekBarTextLeft != null) {
            mediaPlayerSeekBarTextLeft.setText(
                    Util.millisecondToHoursMinutesSeconds(
                            backgroundAudioService.getMediaCurrentPosition()
                    )
            );
        }

        long duration = backgroundAudioService.getMediaDuration();

        if (mediaPlayerSeekBarTextRight != null) {
            mediaPlayerSeekBarTextRight.setText(
                    Util.millisecondToHoursMinutesSeconds(duration)
            );
        }

        if (mediaPlayerTitle != null) {
            mediaPlayerTitle.setText(backgroundAudioService.getMediaTitle());
        }
        if (mediaPlayerDesc != null) {
            mediaPlayerDesc.setText(backgroundAudioService.getMediaDescription());
        }
        if (mediaPlayerImageView != null ){
            setMediaPicture();
        }

        if (mediaPlayerSeekBar != null) {
            mediaPlayerSeekBar.setEnabled(backgroundAudioService.getMediaIsSkippable());
            mediaPlayerSeekBar.setMax((int)duration);
        }
    }

    private void setMediaPicture() {
        mediaPlayerImageView.setImageResource(R.drawable.blank_image);
        FileCache fileCache = FileCache.getInstances(this.getContext());
        Shared.ImageJSON imageJSON = backgroundAudioService.getMediaImage();
        //if the image is not a drawable (aka the media has its own image, then we load it)
        if (!imageJSON.isImageDrawable()) {
            fileCache.getImage(
                    imageJSON.href,
                    this.getContext(),
                    (FileInputStream fileInputStream, String url) -> {
                        if (fileInputStream == null) {
                            //TODO: put up blank image
                            Log.e(TAG, "onServiceConnected: failed " +
                                    "to get image. Check out other logs");
                        } else {
                            mediaPlayerImageView.setImageBitmap(
                                    BitmapFactory.decodeStream(fileInputStream)
                            );
                        }
                    },
                    (int progress, int total, int speed) -> {
                        Log.d(TAG, "nextMediaHelper: progress loading image content player: "
                            + imageJSON.href + " at "
                            + " progress [" + progress + "] total [" + total + "] "
                            + " percent [" + ((double)progress)/((double)total));
                    }
            );
        }
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
