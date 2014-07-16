package com.hello.pirupea.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by pangwu on 7/11/14.
 */
public class LocalSettings {
    private static final String PILL_ADDRESS = "pill_address";


    public static String getPillAddress(final Context context){
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PILL_ADDRESS, null);
    }

    public static void setPillAddress(final Context context, final String value){
        final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(PILL_ADDRESS, value);

        editor.commit();
    }
}
