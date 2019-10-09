package com.ngs.react.RNNotifications;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationsPackage implements ReactPackage {

    private Class<? extends MessageReceivedDelegate> delegateClass;

    public NotificationsPackage(Class<? extends MessageReceivedDelegate> delegateClass) {
        this.delegateClass = delegateClass;
    }

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        List<NativeModule> list = new ArrayList<>();
        list.add(new NotificationsModule(reactContext, delegateClass));
        return list;
    }

    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return null;
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Collections.emptyList();
    }
}
