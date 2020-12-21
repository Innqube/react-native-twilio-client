package com.ngs.react.RNTwilioVideo;

import android.content.ContentResolver;
import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import com.ngs.react.R;

public class SoundPoolManager {

    private boolean playing = false;
    private static SoundPoolManager instance;
    private static final Long MAX_RING_TIME = 90 * 1000L;
    private Ringtone ringtone = null;

    private SoundPoolManager(Context context) {
//        Uri ringtoneSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        Uri ringtoneSound = Uri.parse(
                ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.incoming
        );
        ringtone = RingtoneManager.getRingtone(context, ringtoneSound);
    }

    public static SoundPoolManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundPoolManager(context);
        }
        return instance;
    }

    public void playRinging() {
        if (!playing) {
            ringtone.play();
            playing = true;
            if (playing) {
                ringtone.play();
            }
        }
    }

    public void stopRinging() {
        if (playing) {
            ringtone.stop();
            playing = false;
        }
    }

    public void playDisconnect() {
        if (!playing) {
            ringtone.stop();
            playing = false;
        }
    }

}
