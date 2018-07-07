package com.nprcommunity.npronecommunity.Layout.Callback;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import com.nprcommunity.npronecommunity.Layout.Adapter.ContentQueueRecyclerViewAdapter;

public class ContentQueueCallback extends ItemTouchHelper.Callback {

    private ItemTouchHelperListener itemTouchHelperAdapter;
    private boolean swipeRemove;
    private int dragFrom = -1;
    private int dragTo = -1;


    public ContentQueueCallback(ItemTouchHelperListener itemTouchHelperAdapter, boolean swipeRemove) {
        this.itemTouchHelperAdapter = itemTouchHelperAdapter;
        this.swipeRemove = swipeRemove;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        ContentQueueRecyclerViewAdapter.ViewHolder currentViewHolder =
                (ContentQueueRecyclerViewAdapter.ViewHolder) viewHolder;
        int dragFlags = 0;
        int swipeFlags = 0;
        if (currentViewHolder.isSkippable()) {
            //only enable swipe if it is skippable
            swipeFlags = (swipeRemove ? ItemTouchHelper.START | ItemTouchHelper.END : 0);
            dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        }

        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView,
                          RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        int fromPosition = viewHolder.getAdapterPosition();
        int toPosition = target.getAdapterPosition();
        //check if going up or down in list
        if (toPosition < fromPosition) {
            //going up in list
            //must go through all of recycler view adapter items from position - 1
            for (int i = fromPosition-1; i >= toPosition; i--) {
                RecyclerView.ViewHolder tmpView =
                        recyclerView.findViewHolderForAdapterPosition(i);
                if (!((ContentQueueRecyclerViewAdapter.ViewHolder)tmpView).isSkippable()) {
                    //if any number of them are no skippable return false
                    return false;
                }
            }
        } else {
            //going down in list
            //must got through all recycler viewholders after this position to see if skippable
            for (int i = fromPosition+1; i <= toPosition; i++) {
                RecyclerView.ViewHolder tmpView =
                        recyclerView.findViewHolderForAdapterPosition(i);
                if (!((ContentQueueRecyclerViewAdapter.ViewHolder)tmpView).isSkippable()) {
                    //if any number of them are no skippable return false
                    return false;
                }
            }
        }

        if(dragFrom == -1) {
            dragFrom = fromPosition;
        }
        dragTo = toPosition;

        itemTouchHelperAdapter.onItemMove(fromPosition, toPosition);

        getMovementFlags(recyclerView, viewHolder);

        return true;
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        if(dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
            itemTouchHelperAdapter.onItemDrop(
                    dragFrom,
                    dragTo
            );
        }

        dragFrom = dragTo = -1;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        itemTouchHelperAdapter.onItemDismiss(viewHolder.getAdapterPosition());
    }
}
