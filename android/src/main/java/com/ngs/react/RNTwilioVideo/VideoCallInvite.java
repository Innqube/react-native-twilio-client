package com.ngs.react.RNTwilioVideo;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import com.ngs.react.RNLocalizedStrings.LocalizedKeys;

import java.util.HashMap;
import java.util.Map;

public class VideoCallInvite implements Parcelable, CallInvite {
    private static final String TAG = "RNTwilioVideo";
    private Map<String, String> data;

    static VideoCallInvite create(Map<String, String> data) {
        return new VideoCallInvite(data);
    }

    public VideoCallInvite(Parcel in) {
        int size = in.readInt();
        this.data = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = in.readString();
            String value = in.readString();
            data.put(key, value);
        }
    }

    public VideoCallInvite(Map<String, String> data) {
        this.data = data;
    }

    public static final Creator<VideoCallInvite> CREATOR = new Creator<VideoCallInvite>() {
        @Override
        public VideoCallInvite createFromParcel(Parcel in) {
            return new VideoCallInvite(in);
        }

        @Override
        public VideoCallInvite[] newArray(int size) {
            return new VideoCallInvite[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.data.size());
        for(Map.Entry<String,String> entry : this.data.entrySet()){
            dest.writeString(entry.getKey());
            dest.writeString(entry.getValue());
        }
    }

    public Map<String, String> getData() {
        return data;
    }

    @Override
    public String getSession() {
        return this.data != null ? this.data.get("session") : null;
    }

    @Override
    public String getFrom(String separator, SharedPreferences sharedPref) {
        if (data != null) {
            String customer = this.data.get("customerName") != null ? this.data.get("customerName") : "";
            String language = this.data.get("languageName") != null ? this.data.get("languageName") : "";
            String estimatedDuration = this.data.get("estimatedDuration") != null ?
                    this.getCallDurationString(this.data.get("estimatedDuration"), sharedPref) :
                    "";
            return language + separator + estimatedDuration + separator + customer;
        }
        return null;
    }

    private String getCallDurationString(String duration, SharedPreferences sharedPref) {
        if (sharedPref == null) {
            return duration.substring(0, 2) + " minutes";
        }
        try {
            Integer minutes = Double.valueOf(duration).intValue();
            if (minutes.equals(120)) {
                return "2 " + sharedPref.getString(LocalizedKeys.HOURS, "hours (or more)");
            }
            if (minutes.equals(60)) {
                return "1 " + sharedPref.getString(LocalizedKeys.HOUR, "hour");
            }
            return minutes + " " + sharedPref.getString(LocalizedKeys.MINUTES, "minutes");
        } catch (Exception ex) {
            Log.i(TAG, "getCallDurationString error: " + ex.getMessage());
            return "";
        }
    }

    public String getTaskAttributes() { return data.get("taskAttributes"); }

    public String getAction() { return data.get("action"); }

    @Override
    public String toString() {
        return "VideoCallInvite{" +
                "data=" + data +
                '}';
    }
}
