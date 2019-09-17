package com.ngs.react.RNTwilioChat;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;

public class TwilioChatChannelModule extends ReactContextBaseJavaModule {

    public TwilioChatChannelModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "RNTwilioChatChannel";
    }

}
