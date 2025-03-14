package com.example.storia.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.storia.model.UserModel;
import com.google.gson.Gson;

public class SharedPrefHelper {
    private static final String PREF_NAME = "UserPrefs";
    private static final String KEY_USER = "User";

    public static void saveUser(Context context, UserModel user) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_USER, new Gson().toJson(user));
        editor.apply();
    }

    public static UserModel getUser(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String userJson = prefs.getString(KEY_USER, null);
        return userJson != null ? new Gson().fromJson(userJson, UserModel.class) : null;
    }

    public static void clearUser(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_USER).apply();
    }
}

