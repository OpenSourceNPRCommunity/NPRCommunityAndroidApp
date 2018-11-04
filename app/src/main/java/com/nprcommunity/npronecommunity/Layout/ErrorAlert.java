package com.nprcommunity.npronecommunity.Layout;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;

import com.nprcommunity.npronecommunity.R;

public class ErrorAlert {
    public static AlertDialog.Builder CreateError(Context context) {
        return CreateError(context, context.getString(R.string.error_alert_title_default));
    }

    public static AlertDialog.Builder CreateError(Context context, String title) {
        return CreateError(context, title, context.getString(R.string.error_alert_message_default));
    }

    public static AlertDialog.Builder CreateError(Context context, String title, String message) {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(context);
        }
        builder.setTitle("Delete entry")
                .setMessage("Are you sure you want to delete this entry?")
        .setPositiveButton(android.R.string.yes, (dialog, which) -> dialog.dismiss())
        .setIcon(android.R.drawable.ic_dialog_alert);
        return builder;
    }
}
