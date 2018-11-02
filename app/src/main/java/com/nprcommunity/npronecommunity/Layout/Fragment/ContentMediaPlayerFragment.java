package com.nprcommunity.npronecommunity.Layout.Fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaDescriptionCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.nprcommunity.npronecommunity.R;
import com.nprcommunity.npronecommunity.Store.FileCache;
import com.nprcommunity.npronecommunity.Util;

import java.io.FileInputStream;

public class ContentMediaPlayerFragment extends Fragment {

    private static String TAG = "CONTENTMEDIAPLAYERFRAGMENT";
    private OnFragmentInteractionListener listener;

    private TextView mediaPlayerSeekBarTextLeft, mediaPlayerSeekBarTextCenter, mediaPlayerSeekBarTextRight, mediaPlayerTitle, mediaPlayerDesc;
    private SeekBar mediaPlayerSeekBar;
    private ImageView mediaPlayerImageView;

    public ContentMediaPlayerFragment() {}

    public static ContentMediaPlayerFragment newInstance() {
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
        //setup fragment
        mediaPlayerSeekBarTextLeft = view.findViewById(R.id.media_player_seek_bar_text_left);
        mediaPlayerSeekBarTextCenter = view.findViewById(R.id.media_player_seek_bar_text_center);
        mediaPlayerSeekBarTextRight = view.findViewById(R.id.media_player_seek_bar_text_right);
        mediaPlayerSeekBar = view.findViewById(R.id.media_player_seek_bar);
        mediaPlayerSeekBar.setMax(listener == null ? 0 : listener.getDuration());
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
                if (listener != null) {
                    listener.seekMedia(seekBar.getProgress());
                }
                setSeek(seekBar.getProgress(), false);
                mediaPlayerSeekBarTextCenter.setVisibility(View.INVISIBLE);
            }
        });
        mediaPlayerTitle = view.findViewById(R.id.media_player_title);
        mediaPlayerDesc = view.findViewById(R.id.media_player_description);
        mediaPlayerImageView = view.findViewById(R.id.media_player_image_view);
        updateMedia();
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

    public void enableSeekBar(boolean bool) {
        if (mediaPlayerSeekBar != null) {
            mediaPlayerSeekBar.setEnabled(bool);
        }
    }

    public void clearData() {
        if (mediaPlayerSeekBarTextLeft != null) {
            mediaPlayerSeekBarTextLeft.setText(
                    Util.millisecondToHoursMinutesSeconds(0)
            );
        }

        if (mediaPlayerSeekBarTextRight != null) {
            mediaPlayerSeekBarTextRight.setText(
                    Util.millisecondToHoursMinutesSeconds(0)
            );
        }

        if (mediaPlayerTitle != null) {
            mediaPlayerTitle.setText(R.string.nothing_to_play);
        }
        if (mediaPlayerDesc != null) {
            mediaPlayerDesc.setText("");
        }
        if (mediaPlayerImageView != null ){
            mediaPlayerImageView.setImageResource(R.mipmap.ic_launcher);
        }

        if (mediaPlayerSeekBar != null) {
            mediaPlayerSeekBar.setEnabled(false);
            mediaPlayerSeekBar.setMax(0);
        }
    }

    public void updateMedia() {
        if (mediaPlayerSeekBarTextLeft != null) {
            mediaPlayerSeekBarTextLeft.setText(
                    Util.millisecondToHoursMinutesSeconds(
                            listener == null ? 0 : listener.getCurrentPosition()
                    )
            );
        }

        long duration = listener == null ? 0 : listener.getDuration();

        if (mediaPlayerSeekBarTextRight != null) {
            mediaPlayerSeekBarTextRight.setText(
                    Util.millisecondToHoursMinutesSeconds(duration)
            );
        }

        if (mediaPlayerTitle != null) {
            if (listener != null) {
                mediaPlayerTitle.setText(listener.getMediaDescription().getTitle());
            } else {
                mediaPlayerTitle.setText(R.string.nothing_to_play);
            }
        }
        if (mediaPlayerDesc != null) {
            mediaPlayerDesc.setText(
                    listener == null ? "" : listener.getMediaDescription().getDescription()
            );
        }
        if (mediaPlayerImageView != null ){
            setMediaPicture();
        }

        if (mediaPlayerSeekBar != null) {
            mediaPlayerSeekBar.setEnabled(listener == null || listener.isMediaSkippable());
            mediaPlayerSeekBar.setMax((int)duration);
        }
    }

    private void setMediaPicture() {
        mediaPlayerImageView.setImageResource(R.drawable.blank_image);
        if (listener != null) {
            FileCache fileCache = FileCache.getInstances(this.getContext());
            String hrefImage = listener.getMediaImage();
            //if the image is not a drawable (aka the media has its own image, then we load it)
            if (hrefImage != null) {
                fileCache.getImage(
                        hrefImage,
                        (Bitmap bitmap) -> {
                            if (bitmap == null) {
                                Log.e(TAG, "onServiceConnected: failed " +
                                        "to get image. Check out other logs");
                            } else {
                                mediaPlayerImageView.setImageBitmap(bitmap);
                            }
                        },
                        (int progress, int total, int speed) -> {
                            Log.d(TAG, "nextMediaHelper: progress loading image content player: "
                                    + hrefImage + " at "
                                    + " progress [" + progress + "] total [" + total + "] "
                                    + " percent [" + ((double)progress)/((double)total));
                        }
                );
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            listener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public interface OnFragmentInteractionListener {
        void seekMedia(int pos);
        @NonNull MediaDescriptionCompat getMediaDescription();
        int getDuration();
        int getCurrentPosition();
        boolean isMediaSkippable();
        String getMediaImage();
    }
}
