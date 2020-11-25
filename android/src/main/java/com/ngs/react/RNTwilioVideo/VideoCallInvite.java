package com.ngs.react.RNTwilioVideo;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VideoCallInvite implements Parcelable, CallInvite {

    private static final String TAG = "RNTwilioVideo";
    private Map<String, String> data;

    static VideoCallInvite create(Map<String, String> data) {
        return new VideoCallInvite(data);
    }

    protected VideoCallInvite(Parcel in) {
        List<String> values = in.createStringArrayList();
        Integer halfSize = values.size() / 2;

        this.data = new HashMap<>();
        for (int i = 0; i < halfSize; i = i + 2) {
            data.put(values.get(i), values.get(i + 1));
        }
//        String[] data = new String[4];
//        in.readStringArray(data);
//        from = data[0];
//        to = data[1];
//        callSid = data[2];
//        bridgeToken = data[3];
//        int size = in.readInt();
//        this.data = new HashMap<>(size);
//        for(int i = 0; i < size; i++){
//            String key = in.readString();
//            String value = in.readString();
//            this.data.put(key,value);
//        }
//        int sizeOfCustomParams = in.readInt();
//        customParameters = new HashMap<>(sizeOfCustomParams);
//        for(int i = 0; i < sizeOfCustomParams; i++){
//            String key = in.readString();
//            String value = in.readString();
//            customParameters.put(key,value);
//        }
//        publisher = null;
    }

    protected VideoCallInvite(Map<String, String> data) {
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
    public String getCallSid() {
        return this.data != null ? this.data.get("twi_call_sid") : null;
    }

    @Override
    public String getFrom() {
        return this.data != null ? this.data.get("displayName") : null;
    }

}
