package com.hello.pirupea.settings;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.hello.pirupea.core.SharedApplication;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by pangwu on 7/11/14.
 */
public class LocalSettings {
    private static final String PILL_ADDRESS = "pill_address";
    private static final String OAUTH_TOKEN = "current_token";
    private static final String LAST_LOGIN_USER = "last_login_user";


    public static String getPillAddress(){
        return PreferenceManager.getDefaultSharedPreferences(SharedApplication.getAppContext()).getString(PILL_ADDRESS, null);
    }

    public static void setPillAddress(final String value){
        final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(SharedApplication.getAppContext()).edit();
        editor.putString(PILL_ADDRESS, value);

        editor.commit();
    }


    public static void saveOAuthToken(final String token){
        checkNotNull(token);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(SharedApplication.getAppContext()).edit();
        editor.putString(OAUTH_TOKEN, token);

        editor.commit();
    }

    public static String getOAuthToken(){
        return PreferenceManager.getDefaultSharedPreferences(SharedApplication.getAppContext()).getString(OAUTH_TOKEN, "");
    }

    public static void saveLastLoginUser(final String userName){
        checkNotNull(userName);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(SharedApplication.getAppContext()).edit();
        editor.putString(LAST_LOGIN_USER, userName);

        editor.commit();
    }

    public static String getLastLoginUser(){
        return PreferenceManager.getDefaultSharedPreferences(SharedApplication.getAppContext()).getString(LAST_LOGIN_USER, "");
    }
}
