package com.ngs.react.RNAudioManager;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.util.Log;
import com.facebook.react.bridge.*;
import com.ngs.react.RNTwilioVoice.AudioDeviceManager;
import com.ngs.react.RNTwilioVoice.AudioDeviceType;
import com.ngs.react.RNTwilioVoice.EventManager;


/**
 * @author Enrique Viard.
 * Copyright © 2021 No Good Software Inc. All rights reserved.
 */

public class AudioManagerModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
    public static String TAG = "RNAudioManagerModule";
    private AudioManager audioManager;
    private EventManager eventManager;
    private AudioDeviceManager audioDeviceManager;

    public AudioManagerModule(ReactApplicationContext reactContext) {
        super(reactContext);

        reactContext.addLifecycleEventListener(this);

        eventManager = new EventManager(reactContext);
        audioManager = (AudioManager) reactContext.getSystemService(Context.AUDIO_SERVICE);
        audioDeviceManager = new AudioDeviceManager(audioManager, eventManager);
        register();
    }

    private void register() {
        audioDeviceManager.register(getReactApplicationContext());
    }

    private void unRegister() {

    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public void onHostResume() {
        register();
    }

    @Override
    public void onHostPause() {
    }

    @Override
    public void onHostDestroy() {
        unRegister();
    }

    @ReactMethod
    public void getAvailableAudioInputs(Promise promise) {
        Log.d(TAG, "getAvailableAudioInputs called");
        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);

        WritableMap json = new WritableNativeMap();

        for (AudioDeviceInfo adi : devices) {
            WritableMap device = new WritableNativeMap();
            String type = AudioDeviceManager.getAudioDeviceType(adi.getType());
            Boolean enabled = AudioDeviceManager.isDeviceEnabled(audioManager, type);
            device.putString("name", String.valueOf(adi.getProductName()));
            device.putBoolean("enabled", enabled);
            device.putInt("type", adi.getType());
            json.putMap(type, device);
        }
        promise.resolve(json);
    }

    @ReactMethod
    public void switchAudioInput(String type, Promise promise) {
        switch (type) {
            case AudioDeviceType.BUILTIN_EARPIECE:
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                audioManager.setWiredHeadsetOn(false);
                audioManager.stopBluetoothSco();
                audioManager.setBluetoothScoOn(false);
                audioManager.setSpeakerphoneOn(false);
                promise.resolve(AudioDeviceType.BUILTIN_EARPIECE);
                break;
            case AudioDeviceType.BUILTIN_SPEAKER:
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                audioManager.setWiredHeadsetOn(false);
                audioManager.stopBluetoothSco();
                audioManager.setBluetoothScoOn(false);
                audioManager.setSpeakerphoneOn(true);
                promise.resolve(AudioDeviceType.BUILTIN_SPEAKER);
                break;
            case AudioDeviceType.BLUETOOTH_HEADSET:
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                audioManager.setWiredHeadsetOn(false);
                audioManager.startBluetoothSco();
                audioManager.setBluetoothScoOn(true);
                audioManager.setSpeakerphoneOn(false);
                promise.resolve(AudioDeviceType.BLUETOOTH_HEADSET);
                break;
            case AudioDeviceType.WIRED_HEADSET:
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                audioManager.setWiredHeadsetOn(true);
                audioManager.stopBluetoothSco();
                audioManager.setBluetoothScoOn(false);
                audioManager.setSpeakerphoneOn(false);
                promise.resolve(AudioDeviceType.WIRED_HEADSET);
                break;
        }
        promise.reject(new AssertionException("Invalid type"));
    }

}
