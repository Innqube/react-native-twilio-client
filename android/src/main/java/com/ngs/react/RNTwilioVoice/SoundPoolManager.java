package com.ngs.react.RNTwilioVoice;

import android.content.ContentResolver;
import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import com.ngs.react.R;

import java.util.Timer;
import java.util.TimerTask;

public class SoundPoolManager {

    private boolean playing = false;
    private static SoundPoolManager instance;
    private static final Long MAX_RING_TIME = 90 * 1000L;
    private Ringtone ringtone = null;
    private Timer timer;
    private Long startTime;

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
            startTime = System.currentTimeMillis();
            ringtone.play();
            playing = true;
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    long current = System.currentTimeMillis();
                    long timeRinging = current - startTime;
                    boolean timeExceeded = timeRinging >= MAX_RING_TIME;

                    if (!ringtone.isPlaying() && playing && !timeExceeded) {
                        ringtone.play();
                    }

                    if (timeExceeded) {
                        cancel();
                    }
                }
            }, 1000, 500);
        }
    }

    public void stopRinging() {
        if (playing) {
            ringtone.stop();
            playing = false;

            if (timer != null) {
                timer.cancel();
            }
        }
    }

    public void playDisconnect() {
        if (!playing) {
            ringtone.stop();
            playing = false;

            if (timer != null) {
                timer.cancel();
            }
        }
    }

}
