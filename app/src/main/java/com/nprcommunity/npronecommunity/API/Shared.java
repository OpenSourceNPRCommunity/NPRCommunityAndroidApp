package com.nprcommunity.npronecommunity.API;

import com.squareup.moshi.Json;

import java.io.Serializable;

public class Shared {

    public static class ImageJSON implements Serializable {
        public String href;
        public @Json(name="content-type") String content_type;
        public String image,
                        rel,
                        producer,
                        provider;

        public boolean isImageDrawable() {
            return href == null;
        }
        public Shared.Progress progressTracker;

        public ImageJSON() {
            if (progressTracker == null) {
                progressTracker = new Shared.Progress();
            }
        }
    }

    public static class WebJSON implements Serializable {
        public String href;
        public @Json(name="content-type") String content_type;
    }

    public static class Progress {
        // values for tracking the progress and total for download audio
        private int progress = -1,
                total = -1;
        private boolean fullyDownloaded = false;

        public boolean isPartiallyDownloaded() {
            return progress >= 0 || total >= 0;
        }

        public void setIsFullyDownloaded(boolean downloaded) {
            fullyDownloaded = downloaded;
        }

        public boolean isFullyDownloaded() {
            return fullyDownloaded;
        }

        public void setProgress(int progress) {
            this.progress = progress;
        }

        public int getProgress() {
            return progress;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getTotal() {
            return total;
        }

        public double getPercentage() {
            if (progress <= 0 || total <= 0) {
                return 0;
            }
            return ((double)progress)/((double)total);
        }
    }
}
