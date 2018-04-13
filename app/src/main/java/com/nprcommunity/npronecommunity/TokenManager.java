package com.nprcommunity.npronecommunity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import java.io.IOException;

public class TokenManager {
    private static final String KEY_ACCOUNT_TOKEN = "ACCOUNT_TOKEN",
                                KEY_ACCOUNT_DATA = "ACCOUNT_DATA";
    private SharedPreferences sharedPreferences;

    public TokenManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(KEY_ACCOUNT_DATA, Context.MODE_PRIVATE);
    }

    public String GetToken() {
        return sharedPreferences.getString(KEY_ACCOUNT_TOKEN, null);
    }

    public void SetToken(String token) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_ACCOUNT_TOKEN, token);
        editor.commit();
    }
}
