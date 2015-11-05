package com.cs5248.android.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AlertDialog;

import com.cs5248.android.R;

import net.steamcrafted.materialiconlib.MaterialDrawableBuilder;

import java.io.PrintWriter;
import java.io.StringWriter;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author lpthanh
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Util {

    public static void showErrorMessage(Context context, String prefix) {
        showErrorMessage(context, prefix, null);
    }

    public static void showErrorMessage(Context context, String prefix, Throwable throwable) {
        String message = throwable == null ? prefix : Util.exceptionToString(prefix, throwable);
        Drawable errorIcon = MaterialDrawableBuilder.with(context)
                .setIcon(MaterialDrawableBuilder.IconValue.ALERT)
                .setColor(Color.RED)
                .setToActionbarSize()
                .build();

        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(R.string.title_error);
        alert.setMessage(message);
        alert.setIcon(errorIcon);
        alert.setPositiveButton(R.string.text_ok, (ignored1, ignored2) -> {
        });
        alert.show();
    }

    public static String exceptionToString(String prefix, Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        if (prefix != null) {
            printWriter.println(prefix);
        }

        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }

}
