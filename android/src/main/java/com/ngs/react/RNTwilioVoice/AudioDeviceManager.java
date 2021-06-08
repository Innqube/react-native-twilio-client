package com.ngs.react.RNTwilioVoice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;
import com.facebook.react.bridge.*;

import static com.ngs.react.RNTwilioVoice.EventManager.EVENT_AUDIO_ROUTE_CHANGED;

/**
 * @author Enrique Viard.
 * Copyright © 2021 No Good Software Inc. All rights reserved.
 */

public class AudioDeviceManager {
    public static String TAG = "RNAudioDeviceManager";
    private AudioManager audioManager;
    private EventManager eventManager;
    private BroadcastReceiver receiver;
    private boolean isAlreadyRegistered;

    public AudioDeviceManager(AudioManager audioManager, EventManager eventManager) {
        this.audioManager = audioManager;
        this.eventManager = eventManager;
        this.initReceiver();
    }

    private void initReceiver() {
        this.receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                WritableMap params = Arguments.createMap();
                params.putString("reason", action);
                params.putString("current", AudioDeviceManager.getCurrentAudioDevice(audioManager));

                Log.d(TAG, "onReceive " + action);
                eventManager.sendEvent(EVENT_AUDIO_ROUTE_CHANGED, params);
            }
        };
    }

    public void register(ReactApplicationContext reactContext) {
        Log.d(TAG, "register " + this.isAlreadyRegistered);
        if (!this.isAlreadyRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
            intentFilter.addAction(AudioManager.ACTION_HEADSET_PLUG);
            intentFilter.addAction(AudioManager.EXTRA_AUDIO_PLUG_STATE);
            reactContext.registerReceiver(this.receiver, intentFilter);

            this.isAlreadyRegistered = true;
        }
    }

    public void unregister(ReactApplicationContext reactContext) {
        Log.d(TAG, "unregister " + this.isAlreadyRegistered);
        if (this.isAlreadyRegistered) {
            reactContext.unregisterReceiver(this.receiver);
            this.isAlreadyRegistered = false;
        }
    }

    public static boolean isDeviceEnabled(AudioManager audioManager, String type) {
        switch (type) {
            case AudioDeviceType.BUILTIN_SPEAKER:
                return audioManager.isSpeakerphoneOn();
            case AudioDeviceType.BLUETOOTH_HEADSET:
                return audioManager.isBluetoothScoOn();
            case AudioDeviceType.WIRED_HEADSET:
                return audioManager.isWiredHeadsetOn();
            default:
                return true;
        }
    }

    public static String getCurrentAudioDevice(AudioManager audioManager) {
        return audioManager.isSpeakerphoneOn() ? AudioDeviceType.BUILTIN_SPEAKER :
                audioManager.isBluetoothScoOn() ? AudioDeviceType.BLUETOOTH_HEADSET :
                        audioManager.isWiredHeadsetOn() ? AudioDeviceType.WIRED_HEADSET : AudioDeviceType.BUILTIN_EARPIECE;
    }

    public static String getAudioDeviceType(int type) {
        switch (type) {
            case 1:
                return AudioDeviceType.BUILTIN_EARPIECE;
            case 2:
                return AudioDeviceType.BUILTIN_SPEAKER;
            case 3:
                return AudioDeviceType.WIRED_HEADSET;
            case 7:
            case 8:
                return AudioDeviceType.BLUETOOTH_HEADSET;
            default:
                return AudioDeviceType.UNKNOWN;
        }
    }

}
