package com.nprcommunity.npronecommunity.Layout.Callback;

import com.nprcommunity.npronecommunity.API.APIRecommendations;

public interface ContentQueuePlayingListener {
    public void swap(int fromPosition, int toPosition);
    public void remove(int position);
    public int remove(APIRecommendations.ItemJSON itemJSON);
}
