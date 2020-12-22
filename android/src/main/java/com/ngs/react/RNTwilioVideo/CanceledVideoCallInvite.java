package com.ngs.react.RNTwilioVideo;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

public class CanceledVideoCallInvite implements Parcelable, CallInvite {

    private Map<String, String> data;

    public CanceledVideoCallInvite(Parcel in) {
        int size = in.readInt();
        this.data = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = in.readString();
            String value = in.readString();
            data.put(key, value);
        }
    }

    public CanceledVideoCallInvite(Map<String, String> data) {
        this.data = data;
    }

    public static final Creator<CanceledVideoCallInvite> CREATOR = new Creator<CanceledVideoCallInvite>() {
        @Override
        public CanceledVideoCallInvite createFromParcel(Parcel in) {
            return new CanceledVideoCallInvite(in);
        }

        @Override
        public CanceledVideoCallInvite[] newArray(int size) {
            return new CanceledVideoCallInvite[size];
        }
    };

    @Override
    public String getSession() {
        return data.get("teamSession");
    }

    @Override
    public String getFrom() {
        return data.get("displayName");
    }

    public String getTaskAttributes() { return data.get("taskAttributes"); }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.data.size());
        for (Map.Entry<String, String> entry : this.data.entrySet()) {
            dest.writeString(entry.getKey());
            dest.writeString(entry.getValue());
        }
    }
}
