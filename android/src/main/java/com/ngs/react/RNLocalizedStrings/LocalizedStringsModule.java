package com.ngs.react.RNLocalizedStrings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.facebook.react.bridge.*;

/**
 * @author Enrique Viard.
 * Copyright © 2021 No Good Software Inc. All rights reserved.
 */

public class LocalizedStringsModule extends ReactContextBaseJavaModule {
    public static String TAG = "RNLocalizedStrings";
    private ReactApplicationContext reactContext;

    public LocalizedStringsModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return TAG;
    }

    @ReactMethod
    public void configure(String language, ReadableMap translations) {
        Log.d(TAG, "setTranslations called: " + language);
        SharedPreferences sharedPref = reactContext.getSharedPreferences("db", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("language", language);

        ReadableMapKeySetIterator iterator = translations.keySetIterator();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            editor.putString(key, translations.getString(key));
            Log.d(TAG, "putKey: " + key + " withValue: " + translations.getString(key));
        }
        editor.apply();
    }
}
