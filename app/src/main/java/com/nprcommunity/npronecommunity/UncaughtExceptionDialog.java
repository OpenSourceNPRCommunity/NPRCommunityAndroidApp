package com.nprcommunity.npronecommunity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;

public class UncaughtExceptionDialog implements Thread.UncaughtExceptionHandler {

    private Activity activity;

    public UncaughtExceptionDialog(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        String error = "Error: " + e.getLocalizedMessage();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("UncaughtException")
                .setNegativeButton(R.string.close, (DialogInterface dialog, int id) -> {
                        dialog.cancel();
                    }
                ).setMessage(error);
        builder.show();
    }
}
