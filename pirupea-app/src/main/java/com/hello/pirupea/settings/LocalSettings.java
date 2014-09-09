package com.hello.pirupea.settings;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hello.pirupea.core.SharedApplication;
import com.hello.suripu.algorithm.core.Segment;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by pangwu on 7/11/14.
 */
public class LocalSettings {
    private static final String PILL_ADDRESS = "pill_address";
    private static final String ALARM = "alarm_time";
    private static final String OAUTH_TOKEN = "current_token";
    private static final String LAST_LOGIN_USER = "last_login_user";
    private static final String SLEEP_CYCLES = "sleep_cycles";


    public static String getPillAddress(){
        return PreferenceManager.getDefaultSharedPreferences(SharedApplication.getAppContext()).getString(PILL_ADDRESS, null);
    }

    public static void setPillAddress(final String value){
        final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(SharedApplication.getAppContext()).edit();
        editor.putString(PILL_ADDRESS, value);

        editor.commit();
    }


    public static List<Segment> getSleepCycles(){
        final String segmentString = PreferenceManager.getDefaultSharedPreferences(SharedApplication.getAppContext()).getString(SLEEP_CYCLES, null);
        final ObjectMapper mapper = new ObjectMapper();
        List<Segment> segments = Collections.EMPTY_LIST;

        try {
            if(segmentString != null) {
                segments = mapper.readValue(segmentString, new TypeReference<List<Segment>>() {
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return segments;
    }

    public static void setSleepCycles(final List<Segment> value){
        final ObjectMapper mapper = new ObjectMapper();
        try {
            final String segmentString = mapper.writeValueAsString(value);
            final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(SharedApplication.getAppContext()).edit();
            editor.putString(SLEEP_CYCLES, segmentString);

            editor.commit();

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }


    }


    public static long getAlarmTime(){
        return PreferenceManager.getDefaultSharedPreferences(SharedApplication.getAppContext()).getLong(ALARM, 0);
    }

    public static void setAlarmTime(final long value){
        final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(SharedApplication.getAppContext()).edit();
        editor.putLong(ALARM, value);

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
