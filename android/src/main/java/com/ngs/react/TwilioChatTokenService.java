package com.ngs.react;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class TwilioChatTokenService extends Service {

    private static final String LOG_TAG = "[Twi-Push]";
    private final IBinder binder = new TwilioFCMListenerBinder();
    private String token;

    public TwilioChatTokenService() {
        Log.d(LOG_TAG, "TwilioFCMListenerService instantiated");
    }

    public class TwilioFCMListenerBinder extends Binder {
        public TwilioChatTokenService getService() {
            return TwilioChatTokenService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

}
