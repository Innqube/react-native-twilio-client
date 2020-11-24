package com.ngs.react.RNTwilioVideo;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CanceledVideoCallInvite implements Parcelable, CallInvite {

    private Map<String, String> data;

    public CanceledVideoCallInvite(Parcel in) {
        List<String> values = in.createStringArrayList();
        Integer halfSize = values.size() / 2;

        this.data = new HashMap<>();
        for (int i = 0; i < halfSize; i = i + 2) {
            data.put(values.get(i), values.get(i + 1));
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
    public String getCallSid() {
        return null;
    }

    @Override
    public String getFrom() {
        return null;
    }

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
}
