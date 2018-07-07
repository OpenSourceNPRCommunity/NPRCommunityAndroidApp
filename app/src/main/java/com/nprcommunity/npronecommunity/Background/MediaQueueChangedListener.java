package com.nprcommunity.npronecommunity.Background;

import com.nprcommunity.npronecommunity.API.APIRecommendations;

public interface MediaQueueChangedListener {
    void addItem(int i, APIRecommendations.ItemJSON itemJSON);
    void removeItem(int i, APIRecommendations.ItemJSON itemJSON);
}
