package com.nprcommunity.npronecommunity.API;

import android.content.Context;
import android.util.Log;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.List;

import okio.Okio;
import okio.Source;

public class APIUser extends API {
    private static String TAG = "API.USER";
    private UserJSON userJSON;

    public APIUser(Context context) {
        super(context);
        URL = URL_BASE + "/identity/v2/user";
    }

    public UserJSON getData() {
        return userJSON;
    }

    @Override
    public void executeFunc(String jsonData, Boolean success) {
        if(success) {
            Moshi moshi = new Moshi.Builder().build();
            JsonAdapter<UserJSON> jsonAdapter = moshi.adapter(UserJSON.class);
            try {
                userJSON = jsonAdapter.fromJson(jsonData);
            } catch (IOException e) {
                Log.e(TAG, "executeFun: Error adapting json data to user: ", e);
            }
        }
    }

    public static class UserJSON {
        public String version,
                href;
        public AttributesJSON attributes;
    }

    public static class AttributesJSON {
        public String id,
                email,
                firstName,
                lastName;
        public List<OrganizationsJSON> organizations;
    }

    public static class OrganizationsJSON {
        public String id,
                displayName,
                call,
                city,
                logo,
                smallLogo,
                donationUrl;
        public int totalListeningTime;
        public NotifOrgJSON notif_org;
        public MembershipJSON membership;
    }

    public static class NotifOrgJSON {
        public String ios;
    }

    public static class MembershipJSON {
        public String memberType;
    }
}
