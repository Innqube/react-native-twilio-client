package com.ngs.react.RNTwilioVoice;

import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;
import com.twilio.voice.*;
import java.util.List;
import androidx.annotation.NonNull;

/**
 * @author Enrique Viard.
 * Copyright © 2021 No Good Software Inc. All rights reserved.
 */

public class TwilioMos {
    public static String TAG = "RNTwilioMos";
    private float minMos;
    private float maxMos;
    private float acc;
    private int counter;
    private Call call;
    private Timer timer;

    public TwilioMos(Call call) {
        this.minMos = 0;
        this.maxMos = 0;
        this.acc = 0;
        this.counter = 0;
        this.call = call;

        init();
    }

    private void init() {
        Log.d(TAG, "About to init Timer");
        timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                call.getStats(new StatsListener() {
                    @Override
                    public void onStats(@NonNull List<StatsReport> statsReports) {
                        for (StatsReport statsReport : statsReports) {
                            List<RemoteAudioTrackStats> remoteAudioStatsList = statsReport.getRemoteAudioTrackStats();
                            for (RemoteAudioTrackStats remoteAudioStats : remoteAudioStatsList) {
                                float mos = remoteAudioStats.mos;

                                acc += mos;
                                counter++;
                                Log.d(TAG, "onStats ACC: " + acc);
                                Log.d(TAG, "onStats COUNTER: " + counter);

                                if (minMos == 0 || minMos > mos) {
                                    minMos = mos;
                                    Log.d(TAG, "onStats MIN_MOS: " + minMos);
                                }

                                if (maxMos == 0 || maxMos < mos) {
                                    maxMos = mos;
                                    Log.d(TAG, "onStats MAX_MOS: " + maxMos);
                                }
                            }
                        }
                    }
                });
            }
        }, 0, 5000);
    }

    public void stop() {
        timer.cancel();
    }

    public float getMinMos() {
        Log.d(TAG, "getMinMos: " + minMos);
        return minMos;
    }

    public float getMaxMos() {
        Log.d(TAG, "getMaxMos: " + maxMos);
        return maxMos;
    }

    public float getAverageMos() {
        if (counter == 0) {
            Log.d(TAG, "cannot get average MOS, counter is 0");
            return 0;
        }
        Log.d(TAG, "getAverageMos: " + acc / counter + " based on " + counter + " results");
        return acc / counter;
    }
}
