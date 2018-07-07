package com.nprcommunity.npronecommunity;

import android.support.annotation.NonNull;

import com.nprcommunity.npronecommunity.API.APIRecommendations;

import java.util.List;
import java.util.Locale;

public class Util {

    public static final long MILLI_SECOND = 1000,
                        MILLI_MINUTE = MILLI_SECOND*60,
                        MILLI_HOUR = MILLI_MINUTE*60,
                        MILLI_DAY = MILLI_HOUR*24;

    public static final String MP3 = "audio/mp3";

    public static boolean correctAudioFormat(@NonNull String format) {
        return MP3.equals(format);
    }

    public static APIRecommendations.AudioJSON getAudioJSON(@NonNull List<APIRecommendations.AudioJSON> audioJSONS) {
        // add the first possible valid audio type
        for (APIRecommendations.AudioJSON audioJSON : audioJSONS) {
            // Only add one copy if correct type
            if (Util.correctAudioFormat(audioJSON.content_type)) {
                return audioJSON;
            }
        }
        return null;
    }

    public static boolean hasAudioJSON(@NonNull List<APIRecommendations.AudioJSON> audioJSONS) {
        // add the first possible valid audio type
        for (APIRecommendations.AudioJSON audioJSON : audioJSONS) {
            if (audioJSON.content_type != null &&
                    Util.correctAudioFormat(audioJSON.content_type) &&
                    audioJSON.href != null &&
                    !audioJSON.href.equals("")) {
                return true;
            }
        }
        return false;
    }

    public static String millisecondToHoursMinutesSeconds(long milliseconds) {
        long seconds = (long) (milliseconds / 1000) % 60 ;
        long minutes = (long) ((milliseconds / (1000*60)) % 60);
        long hours   = (long) ((milliseconds / (1000*60*60)) % 24);
        if (hours > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.US, "%d:%02d", minutes, seconds);
    }

    public static String getBytesString(int byteLength) {
        if (byteLength < 1000) {
            //B/s
            return byteLength + " B/s";
        } else if (byteLength/1000 < 1000) {
            //KB/s
            return String.format(Locale.US, "%.2f KB/s",
                    ((double)byteLength)/((double)1000));
        } else {
            //MB/s
            return String.format(Locale.US, "%.2f MB/s",
                    ((double)byteLength)/((double)1000000));
        }
    }
}
