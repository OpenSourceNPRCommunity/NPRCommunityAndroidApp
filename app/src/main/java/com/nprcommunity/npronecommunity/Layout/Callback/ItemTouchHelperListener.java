package com.nprcommunity.npronecommunity.Layout.Callback;

public interface ItemTouchHelperListener {

    void onItemMove(int fromPosition, int toPosition);

    void onItemDismiss(int position);

    void onItemDrop(int fromPosition, int toPosition);

}
