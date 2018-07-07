package com.nprcommunity.npronecommunity.Store;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

public class SettingsAndTokenManager {
    private static final String KEY_ACCOUNT_TOKEN = "ACCOUNT_TOKEN",
                                KEY_ACCOUNT_DATA = "ACCOUNT_DATA";
    private SharedPreferences sharedPreferences;

    public enum SettingsKey {
        SWIPE_REMOVE_ENABLED, AUTO_PLAY_ENABLED
    }

    public SettingsAndTokenManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(KEY_ACCOUNT_DATA, Context.MODE_PRIVATE);
    }

    public String getToken() {
        return sharedPreferences.getString(KEY_ACCOUNT_TOKEN, null);
    }

    public void deleteToken() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_ACCOUNT_TOKEN);
        editor.commit();
    }

    public void setToken(String token) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_ACCOUNT_TOKEN, token);
        editor.commit();
    }

    public void setConfig(@NonNull SettingsKey settings, @NonNull Object value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (value instanceof Boolean) {
            editor.putBoolean(settings.name(), (Boolean) value);
        } else if (value instanceof String) {
            editor.putString(settings.name(), (String) value);
        } else {
            editor.putInt(settings.name(), (Integer) value);
        }
        editor.apply();
    }

    public boolean getConfigBoolean(@NonNull SettingsKey settingsKey, boolean def) {
        return sharedPreferences.getBoolean(settingsKey.name(), def);
    }

    public String getConfigString(@NonNull SettingsKey settingsKey, String def) {
        return sharedPreferences.getString(settingsKey.name(), def);
    }

    public int getConfigInt(@NonNull SettingsKey settingsKey, int def) {
        return sharedPreferences.getInt(settingsKey.name(), def);
    }
}
