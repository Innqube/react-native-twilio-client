package com.ngs.react.RNTwilioVideo;

import android.content.SharedPreferences;

public interface CallInvite {

    String getSession();

    String getFrom(String separator, SharedPreferences sharedPref);

}
