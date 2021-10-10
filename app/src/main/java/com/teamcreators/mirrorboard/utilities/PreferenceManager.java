package com.teamcreators.mirrorboard.utilities;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * A class that can be used to store and retrieve small amounts of
 * primitive data as key/value pairs to a file on the device storage
 *
 * @author Jianwei Li
 */
public class PreferenceManager {
    private SharedPreferences sharedPreferences;

    // constructor
    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(
                Constants.KEY_PREFERENCE_NAME,
                Context.MODE_PRIVATE);
    }

    /**
     * Store Boolean data as key/value pairs to the device storage
     * @param key the key of a value determines where in the table the value will be stored
     * @param value the Boolean data to be stored
     */
    public void putBoolean(String key, Boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    /**
     * Retrieve Boolean data based on the given key from the device storage
     * @param key the key of a value determines where in the table the value was stored
     * @return the Boolean data retrieved
     */
    public Boolean getBoolean(String key) {
        return sharedPreferences.getBoolean(key, false);
    }

    /**
     * Store String data as key/value pairs to the device storage
     * @param key the key of a value determines where in the table the value will be stored
     * @param value the String data to be stored
     */
    public void putString(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * Retrieve String data based on the given key from the device storage
     * @param key the key of a value determines where in the table the value was stored
     * @return the String data retrieved
     */
    public String getString(String key) {
        return sharedPreferences.getString(key, null);
    }

    /**
     * Store Set data as key/value pairs to the device storage
     * @param key the key of a value determines where in the table the value will be stored
     * @param value the Set data to be stored
     */
    public void putStringSet(String key, HashSet<String> value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String set = value.toString().replaceAll("\\[|]|\\s", "");
        editor.putString(key, set);
        editor.apply();
    }

    /**
     * Retrieve Set data based on the given key from the device storage
     * @param key the key of a value determines where in the table the value was stored
     * @return the Set data retrieved
     */
    public HashSet<String> getStringSet(String key) {
        String set = sharedPreferences.getString(key, "");
        if (set.equals("")) {
            return new HashSet<>();
        }
        String[] setParts = set.split(",");
        List<String> listParts = Arrays.asList(setParts);
        return new HashSet<>(listParts);
    }

    /**
     * Clear all key/value pairs of primitive data stored in the file on the device storage
     */
    public void clearPreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Delete the corresponding value according to the given key
     * @param key the key of a value determines where in the table the value was stored
     */
    public void clearString(String key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(key);
        editor.apply();
    }
}
