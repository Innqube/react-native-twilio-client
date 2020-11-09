package com.ngs.react;

import android.util.Log;
import com.facebook.react.bridge.Promise;
import com.twilio.chat.CallbackListener;
import com.twilio.chat.ErrorInfo;

public abstract class PromiseCallbackListener<T> extends CallbackListener<T> {

    private static final String LOG_TAG = "[Twi-Chat]";
    protected Promise promise;

    public PromiseCallbackListener(Promise promise) {
        this.promise = promise;
    }

    @Override
    public void onError(ErrorInfo errorInfo) {
        Log.d(LOG_TAG, "Promise rejected: " + errorInfo.getCode() + ": " + errorInfo.getMessage());
        promise.reject(Integer.valueOf(errorInfo.getCode()).toString(), errorInfo.getMessage());
    }

}
