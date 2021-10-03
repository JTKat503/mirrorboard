package com.teamcreators.mirrorboard.utilities;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class PreferenceManager {

    private SharedPreferences sharedPreferences;

    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(Constants.KEY_PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    public void putBoolean(String key, Boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public Boolean getBoolean(String key) {
        return sharedPreferences.getBoolean(key, false);
    }

    public void putString(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public String getString(String key) {
        return sharedPreferences.getString(key, null);
    }

    public void putStringSet(String key, HashSet<String> value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String set = value.toString().replaceAll("\\[|]|\\s", "");
        editor.putString(key, set);
        editor.apply();
    }

    public HashSet<String> getStringSet(String key) {
        String set = sharedPreferences.getString(key, "");
        if (set.equals("")) {
            return new HashSet<>();
        }
        String[] setParts = set.split(",");
        List<String> listParts = Arrays.asList(setParts);
        return new HashSet<>(listParts);
    }

    public void clearPreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}
