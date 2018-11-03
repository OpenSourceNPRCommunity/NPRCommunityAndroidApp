package com.nprcommunity.npronecommunity.Layout.Fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nprcommunity.npronecommunity.API.APIRecommendations;
import com.nprcommunity.npronecommunity.Layout.Adapter.ContentQueueRecyclerViewAdapter;
import com.nprcommunity.npronecommunity.Layout.Callback.ContentQueueCallback;
import com.nprcommunity.npronecommunity.R;
import com.nprcommunity.npronecommunity.Store.SettingsAndTokenManager;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class ContentQueueFragment extends Fragment {

    private OnListFragmentInteractionListener listener;
    private RecyclerView recyclerView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ContentQueueFragment() {}

    public static ContentQueueFragment newInstance() {
        ContentQueueFragment fragment = new ContentQueueFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
//            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.content_queue_fragment_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            recyclerView = (RecyclerView) view;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));

            //Setup the drag callback and movable
            ContentQueueRecyclerViewAdapter contentQueueRecyclerViewAdapter =
                    new ContentQueueRecyclerViewAdapter(
                            listener,
                            context,
                            ContentQueueFragment.this.getActivity()
                    );

            SettingsAndTokenManager settingsAndTokenManager;
            boolean swipeEnabled = false;
            if (this.getContext() != null) {
                    settingsAndTokenManager = new SettingsAndTokenManager(this.getContext());
                    swipeEnabled = settingsAndTokenManager.getConfigBoolean(
                            SettingsAndTokenManager.SettingsKey.SWIPE_REMOVE_ENABLED,
                            false
                    );
            }
            ItemTouchHelper.Callback callback =
                    new ContentQueueCallback(contentQueueRecyclerViewAdapter, swipeEnabled);
            ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
            touchHelper.attachToRecyclerView(recyclerView);

            recyclerView.setAdapter(contentQueueRecyclerViewAdapter);
        }
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            listener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public View getView(int position) {
        return recyclerView.getLayoutManager().findViewByPosition(position);
    }

    public View getView(String href) {
        for (int i = 0; i < recyclerView.getAdapter().getItemCount(); i++) {
            View tmpView = recyclerView.getLayoutManager().findViewByPosition(i);
            if (tmpView != null) {
                TextView titleTextView = tmpView.findViewById(R.id.queue_rec_href);
                if (titleTextView != null && titleTextView.getText().equals(href)) {
                    return tmpView;
                }
            }
        }
        return null;
    }

    public RecyclerView.Adapter getQueueAdapter() {
        return recyclerView.getAdapter();
    }

    public interface OnListFragmentInteractionListener {
        void remove(int position);
        void remove(APIRecommendations.ItemJSON itemJSON);
        void swap(int fromPosition, int toPosition);
    }
}
