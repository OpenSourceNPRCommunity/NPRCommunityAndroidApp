package com.nprcommunity.npronecommunity.API;

import android.content.Context;
import android.util.Log;

import com.nprcommunity.npronecommunity.Store.CacheStructures.RecommendationCache;
import com.nprcommunity.npronecommunity.Store.JSONCache;
import com.nprcommunity.npronecommunity.Util;
import com.squareup.moshi.FromJson;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.ToJson;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import okio.Okio;
import okio.Source;

public class APIRecommendations extends API {
    private static String TAG = "API.RECOMMENDATIONS";

    public static final String BASE_URL = "https://listening.api.npr.org/v2/",
            DEFAULT_RECOMMENDATIONS_URL = BASE_URL + "recommendations";

    public APIRecommendations(Context context, String url) {
        super(context);
        URL = url;
    }

    public RecommendationCache getData() {
        return (RecommendationCache)data;
    }

    @Override
    public void executeFunc(String jsonData, Boolean success) {
        if(success) {
            Moshi moshi = new Moshi.Builder().add(new AtomicIntegerAdapter()).build();
            JsonAdapter<RecommendationsJSON> jsonAdapter = moshi.adapter(RecommendationsJSON.class);
            try {
                RecommendationsJSON recommendationsJSON = jsonAdapter.fromJson(jsonData);
                //TODO add clean methods for validation in all of the API calls
                if (recommendationsJSON != null && recommendationsJSON.isValidRecommendations()) {
                    cleanRecommendation(recommendationsJSON);
                } else {
                    Log.e(TAG, "executeFun: Error invalid recommendation");
                }
                //Set data
                RecommendationCache recommendationsCache = new RecommendationCache(recommendationsJSON, URL);
                JSONCache.putObject(URL, recommendationsCache);
                data = recommendationsCache;
            } catch (IOException e) {
                Log.e(TAG, "executeFunc: Error adapting json data to user", e);
            }
        }
    }

    private void cleanRecommendation(RecommendationsJSON recommendationsJSON) {
        cleanRecommendationItems(recommendationsJSON.items);
    }

    protected static void cleanRecommendationItems(List<ItemJSON> itemsJSON) {
        Iterator<ItemJSON> itemJSONIterator = itemsJSON.iterator();
        while (itemJSONIterator.hasNext()) {
            ItemJSON item = itemJSONIterator.next();
            if (!item.isValidItem()) {
                itemJSONIterator.remove();
            } else {
                item.attributes.clean();
            }
        }
    }

    public static class RecommendationsJSON implements Serializable {
        private static final String VERSION_1 = "1.0";
        public String version,
                href;
        public List<ItemJSON> items;

        public boolean isValidRecommendations() {
            return version != null &&
                    version.equals(VERSION_1) &&
                    href != null &&
                    !href.equals("");
        }
    }

    public static class ItemJSON implements Serializable {
        private static final String VERSION_1 = "1.0";

        public String version,
                href;
        public AttributesJSON attributes;
        public LinksJSON links;

        /**
         * Method for validating ItemJSON for application intake. This
         * will stop many problems like null pointers by invalidating it
         * early on.
         * @return
         */
        public boolean isValidItem() {
            return version != null &&
                    version.equals(VERSION_1) &&
                    href != null &&
                    !href.equals("") &&
                    links != null &&
                    attributes != null &&
                    attributes.hasDate() &&
                    attributes.hasType();
        }

        public String toString() {
            return "HREF: " + href;
        }
    }

    public static class AttributesJSON implements Serializable {
        private static final String DEFAULT_DESCRIPTION = "Unknown Description";
        private static final String DEFAULT_TITLE = "Unknown Title";

        public String type,
                uid,
                title,
                audioTitle;
        public boolean primary;
        public GeoFenceJSON geoFence;
        public OrganizationJSON organization;
//        public String button;
        public boolean skippable;
        public String rationale,
                        slug,
                        provider,
                        program;
        public int duration;
        public String date,
                        expires,
                        description,
                        song,
                        artist,
                        album,
                        label;
        public RatingJSON rating;

        public boolean isSkippable() {
            return skippable;
        }

        public boolean hasType() {
            return type != null && !type.equals("");
        }

        public boolean hasDate() {
            return date != null && !date.equals("");
        }

        public void clean() {
            if (description == null || description.equals("")) {
                description = DEFAULT_DESCRIPTION;
            }

            if (title == null || title.equals("")) {
                title = DEFAULT_TITLE;
            }

            if (audioTitle == null || audioTitle.equals("")) {
                audioTitle = title;
            }
        }

        public boolean hasRating() {
            return rating != null;
        }
    }

    public static class GeoFenceJSON implements Serializable{
        public boolean restricted;

        public List<String> countries;
    }

    public static class OrganizationJSON implements Serializable{
        public String name,
                        logoUrl,
                        homepageUrl,
                        donateUrl;
    }

    public static class AtomicIntegerAdapter {
        @FromJson AtomicInteger fromJson(String atomicIntegerJSON) {
            AtomicInteger atomicInteger = null;
            try {
                atomicInteger = new AtomicInteger(Integer.parseInt(atomicIntegerJSON));
            } catch (NumberFormatException e) {
                atomicInteger = new AtomicInteger();
            }
            return atomicInteger;
        }

        @ToJson String toJson(AtomicInteger atomicInteger) {
            return atomicInteger.toString();
        }
    }

    public static class RatingJSON implements Serializable{
        public String mediaId,
                        origin,
                        rating;
        public AtomicInteger elapsed;
        public int duration;
        public String timestamp,
                        channel,
                        cohort;
        public List<String> affiliations;

        private List<Integer> affiliationsCleaned = new ArrayList<>();

        public boolean hasAffiliations() {
            if (affiliations == null || affiliations.size() <= 0) {
                return false;
            }
            try {
                affiliationsCleaned.add(Integer.parseInt(affiliations.get(0)));
            } catch (NumberFormatException e) {
                return false;
            }
            return true;
        }

        public List<Integer> getAffiliations() {
            return affiliationsCleaned;
        }
    }

    public static class LinksJSON implements Serializable{
        public List<AudioJSON> audio;
        public List<Shared.ImageJSON> image;
        public List<Shared.WebJSON> web;
        public List<OnRampsJSON> onramps;
        public List<UpJSON> up;
        public List<AttributesRecommendationsJSON> recommendations;
        public List<RatingsJSON> ratings;

        public boolean hasAudio() {
            return audio != null && audio.size() > 0;
        }

        public boolean hasValidAudio() {
            return audio != null &&
                    audio.size() > 0 &&
                    Util.hasAudioJSON(audio);
        }

        public AudioJSON getValidAudio() {
            return Util.getAudioJSON(audio);
        }

        public boolean hasImage() {
            return image != null && image.size() > 0;
        }

        public Shared.ImageJSON getValidImage() {
            return image.get(0);
        }

        public boolean hasRecommendations() {
            return recommendations != null && recommendations.size() > 0;
        }

        public boolean hasUp() {
            return up != null && up.size() > 0;
        }
    }

    public static class AudioJSON implements Serializable{
        public String href;
        public @Json(name="content-type") String content_type;
        public Shared.Progress progressTracker;

        public AudioJSON() {
            if (progressTracker == null) {
                progressTracker = new Shared.Progress();
            }
        }
    }

    /**
     * ::NPR DOCUMENTATION::
     * This is the URL that should be POSTed with the ratings JSON when this audio starts to play
     * ::NPR DOCUMENTATION::
     */
    public static class AttributesRecommendationsJSON implements Serializable{
        public String href;
        public @Json(name="content-type") String content_type;
    }

    /**
     * ::NPR DOCUMENTATION::
     * This is an alternate URL to use to POST the ratings JSON. Difference between this
     * and ‘recommendations’ is that ‘ratings’ will NOT return back recommendations of audio
     * to play next.
     * ::NPR DOCUMENTATION::
     */
    public static class RatingsJSON implements Serializable {
        public String href;
        public @Json(name="content-type") String content_type;
    }

    public static class UpJSON implements Serializable{
        public String href;
        public @Json(name="content-type") String content_type;
    }

    /**
     * ::NPR DOCUMENTATION::
     * One or more shareable links for the item
     * ::NPR DOCUMENTATION::
     */
    public static class OnRampsJSON implements Serializable{
        public String href;
        public @Json(name="content-type") String content_type;
    }
}