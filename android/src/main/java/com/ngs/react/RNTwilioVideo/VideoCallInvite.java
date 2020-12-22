package com.ngs.react.RNTwilioVideo;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

public class VideoCallInvite implements Parcelable, CallInvite {

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
        return this.data != null ? this.data.get("teamSession") : null;
    }

    @Override
    public String getFrom() {
        return this.data != null ? this.data.get("displayName") : null;
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
