package com.nprcommunity.npronecommunity.API;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.nprcommunity.npronecommunity.TokenManager;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class User extends API {
    private static String TAG = "API.USER";
    private UserJSON userJSON;

    public User(Context context) {
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
                Log.e(TAG, "executeFunc: Error adapting json data to user: " + jsonData);
            }
        }
    }

//    private static class DataLoaderJSON extends AsyncTask<String, Void, Boolean> {
//
//        protected Boolean doInBackground(String... urlAndToken) {
//            GenericUrl url = new GenericUrl(urlAndToken[0]);
//            HttpHeaders headers = new HttpHeaders();
//            List<String> list = new ArrayList<>();
//            list.add("Bearer " + urlAndToken[1]);
//            headers.set("Authorization", list);
//            HttpTransport transport = new NetHttpTransport();
//            try {
//                HttpRequest request = transport.createRequestFactory().buildGetRequest(url);
//                request.setHeaders(headers);
//                HttpResponse response = request.execute();
//                Moshi moshi = new Moshi.Builder().build();
//                JsonAdapter<UserJSON> jsonAdapter = moshi.adapter(UserJSON.class);
//                String responseString = response.parseAsString();
//                Log.v(TAG, responseString);
//                userJSON = jsonAdapter.fromJson(responseString);
//                return true;
//            } catch (IOException e) {
//                e.getStackTrace();
//                userJSON = null;
//            }
//            return false;
//        }
//
//        protected void onPostExecute(Boolean b) {
//            // TODO: check this.exception
//            // TODO: do something with the feed
//        }
//    }

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
