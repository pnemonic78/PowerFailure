package net.sf.power.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Power connection events receiver.
 * Created by moshe.w on 28/12/2015.
 */
public class PowerConnectionReceiver extends BroadcastReceiver {

    private static final String TAG = "PowerConnectionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, intent.toString());
    }
}