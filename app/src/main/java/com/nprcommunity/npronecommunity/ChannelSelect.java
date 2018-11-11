package com.nprcommunity.npronecommunity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.nprcommunity.npronecommunity.API.APIChannels;
import com.nprcommunity.npronecommunity.Layout.Adapter.ChannelSelectAdapter;
import com.nprcommunity.npronecommunity.Store.CacheStructures.ChannelCache;
import com.nprcommunity.npronecommunity.Store.JSONCache;

import java.util.Collections;

public class ChannelSelect extends AppCompatActivity {
    public static final int RESULT_CODE = 1000;
    public static final String TAG = "CHANNELSELECT";

    private ListView channelSelectListView;

    //will be filled with loaded data
    private ChannelCache channelsData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_select);

        //setup list view
        channelSelectListView = (ListView) findViewById(R.id.channel_select_list_view);
        channelSelectListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        channelSelectListView.setItemsCanFocus(false);
        channelSelectListView.setOnItemClickListener(
                (AdapterView<?> parent, View view, int position, long id) -> {
                    //on item clicked
                }
        );

        //Setup APIChannels adapater and channel
        APIChannels APIChannels = new APIChannels(this);
        APIChannels.updateData(() -> {
            channelsData = APIChannels.getData();
            if(channelsData == null || channelsData.data == null) {
                Log.e(TAG, "onCreate: APIChannels data is null for channel adapter");
                //TODO put error up on screen
            } else {
                Log.d(TAG, "onCreate: APIChannels got data!");

                //sort APIChannels list by fullname
                Collections.sort(channelsData.data.items,
                        (APIChannels.ItemJSON o1, APIChannels.ItemJSON o2) -> {
                            return o1.attributes.fullName.compareTo(o2.attributes.fullName);
                        });

                //Set the channelsJSON items up for the list
                channelSelectListView.setAdapter(new ChannelSelectAdapter(
                                this,
                                R.id.channel_select_list_view,
                                channelsData.data.items
                        )
                );
            }
        },
                false);

        //setup save button
        Button button = (Button) findViewById(R.id.channel_select_save);
        button.setOnClickListener((View v) -> {
            if (channelsData == null) {
                //TOdo error of toast or something
                Log.e(TAG, "onCreate: selected_channels_save channelsData is null");
            } else {
                JSONCache.putObject(channelsData.urlParent, channelsData);
            }
            setResultCode();
            finish();
        });
    }

    private void setResultCode() {
        Intent previousScreen = new Intent(getApplicationContext(), Navigate.class);
        setResult(RESULT_CODE, previousScreen);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResultCode();
    }
}
