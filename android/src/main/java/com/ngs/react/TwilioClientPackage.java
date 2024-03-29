package com.ngs.react;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.ngs.react.RNAudioManager.AudioManagerModule;
import com.ngs.react.RNLocalizedStrings.LocalizedStringsModule;
import com.ngs.react.RNLogHelper.LogHelperModule;
import com.ngs.react.RNNotifications.NotificationsModule;
import com.ngs.react.RNTwilioChat.TwilioChatChannelModule;
import com.ngs.react.RNTwilioChat.TwilioChatModule;
import com.ngs.react.RNTwilioVideo.TwilioVideoModule;
import com.ngs.react.RNTwilioVoice.TwilioVoiceModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TwilioClientPackage implements ReactPackage {

    private boolean mShouldAskForPermission;
    public TwilioClientPackage() {
        mShouldAskForPermission = true;
    }

    public TwilioClientPackage(boolean shouldAskForPermissions) {
        mShouldAskForPermission = shouldAskForPermissions;
    }
    // Deprecated in RN 0.47.0
    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return Collections.emptyList();
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Collections.emptyList();
    }

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        List<NativeModule> modules = new ArrayList<>();
        modules.add(new TwilioVoiceModule(reactContext, mShouldAskForPermission));
        modules.add(new TwilioChatModule(reactContext));
        modules.add(new TwilioChatChannelModule(reactContext));
        modules.add(new TwilioVideoModule(reactContext));
        modules.add(new LogHelperModule(reactContext));
        modules.add(new NotificationsModule(reactContext));
        modules.add(new AudioManagerModule(reactContext));
        modules.add(new LocalizedStringsModule(reactContext));
        return modules;
    }
}
