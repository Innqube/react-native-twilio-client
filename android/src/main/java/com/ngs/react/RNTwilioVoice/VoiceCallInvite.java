package com.ngs.react.RNTwilioVoice;

import android.os.Parcel;
import android.os.Parcelable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class VoiceCallInvite implements Parcelable {

    private Map<String, String> data;

    static VoiceCallInvite create(Map<String, String> data) {
        return new VoiceCallInvite(data);
    }

    public VoiceCallInvite(Parcel in) {
        int size = in.readInt();
        this.data = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = in.readString();
            String value = in.readString();
            data.put(key, value);
        }
    }

    public VoiceCallInvite(Map<String, String> data) {
        this.data = data;
    }

    public static final Creator<VoiceCallInvite> CREATOR = new Creator<VoiceCallInvite>() {
        @Override
        public VoiceCallInvite createFromParcel(Parcel in) {
            return new VoiceCallInvite(in);
        }

        @Override
        public VoiceCallInvite[] newArray(int size) {
            return new VoiceCallInvite[size];
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

    public String getSession() {
        if (getTaskAttributes() == null) {
            return null;
        }
        try {
            JSONObject taskAttributes = new JSONObject(getTaskAttributes());
            return taskAttributes.getString("session");
        } catch (JSONException e) {
            return null;
        }
    }

    public String getFrom(String separator) {
        if (this.data != null) {
            String customer = this.data.get("customerName") != null ? this.data.get("customerName") : "";
            String language = this.data.get("languageName") != null ? this.data.get("languageName") : "";
            String estimatedDuration = this.data.get("estimatedDuration") != null ?
                    (this.data.get("estimatedDuration").substring(0, this.data.get("estimatedDuration").length() -2) + " minutes") :
                    "";
            return language + " / " + estimatedDuration + separator + customer;
        }
        return null;
    }

    public String getTaskAttributes() { return data.get("taskAttributes"); }

    public String getCallSid() {
        if (getTaskAttributes() == null) {
            return null;
        }
        try {
            JSONObject taskAttributes = new JSONObject(getTaskAttributes());
            return taskAttributes.getString("callSid");
        } catch (JSONException e) {
            return null;
        }
    }

    public String getAction() { return data.get("action"); }

    @Override
    public String toString() {
        return "VoiceCallInvite{" +
                "data=" + data +
                '}';
    }

}
