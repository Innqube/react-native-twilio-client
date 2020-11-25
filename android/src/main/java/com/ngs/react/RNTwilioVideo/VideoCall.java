package com.ngs.react.RNTwilioVideo;

public class VideoCall {

    private String callSid;
    private String from;

    public VideoCall(String callSid, String from) {
        this.callSid = callSid;
        this.from = from;
    }

    public String getCallSid() {
        return callSid;
    }

    public void setCallSid(String callSid) {
        this.callSid = callSid;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
