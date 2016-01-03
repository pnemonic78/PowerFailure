package net.sf.power.monitor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private static final long POLL_RATE = DateUtils.SECOND_IN_MILLIS;

    private static final int BATTER_PLUGGED_NONE = 0;

    private static final int LEVEL_UNPLUGGED = 0;
    private static final int LEVEL_PLUGGED = 1;

    private ImageView pluggedView;

    private static IntentFilter batteryFilter;
    private Handler handler;
    private Ringtone ringtone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        pluggedView = (ImageView) findViewById(R.id.plugged);

        handler = new MainHandler(this);
        stopAlarm(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(this);
    }

    private void registerReceiver(Context context) {
        IntentFilter filter = batteryFilter;
        if (filter == null) {
            filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            batteryFilter = filter;
        }

        printStatus(context);
        pollStatus();
    }

    private void unregisterReceiver(Context context) {
        // Sticky intent doesn't need to unregister.
        if (handler != null) {
            handler.removeMessages(MainHandler.CHECK_STATUS);
        }
    }

    protected Intent getBatteryIntent(Context context) {
        return context.registerReceiver(null, batteryFilter);
    }

    private void checkStatus() {
        Context context = this;
        Intent intent = getBatteryIntent(context);
        printStatus(intent);

        Bundle extras = intent.getExtras();
        int plugged = extras.getInt(BatteryManager.EXTRA_PLUGGED, BATTER_PLUGGED_NONE);
        onPlugged(plugged != BATTER_PLUGGED_NONE);

        pollStatus();
    }

    private void pollStatus() {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(MainHandler.CHECK_STATUS, POLL_RATE);
        }
    }

    private void printStatus(Context context) {
        printStatus(getBatteryIntent(context));
    }

    private void printStatus(Intent intent) {
        Bundle extras = intent.getExtras();
        int plugged = extras.getInt(BatteryManager.EXTRA_PLUGGED, -1);
        boolean present = extras.getBoolean(BatteryManager.EXTRA_PRESENT, false);
        int status = extras.getInt(BatteryManager.EXTRA_STATUS, -1);
        Log.i(TAG, "status=" + status + " plugged=" + plugged + " present=" + present);
    }

    private static class MainHandler extends Handler {

        private static final int CHECK_STATUS = 1;

        private final WeakReference<MainActivity> activity;

        public MainHandler(MainActivity activity) {
            this.activity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = this.activity.get();
            if (activity == null) {
                return;
            }

            switch (msg.what) {
                case CHECK_STATUS:
                    activity.checkStatus();
                    break;
            }
        }
    }

    /**
     * Notification that the battery is being been charged.
     *
     * @param plugged is plugged?
     */
    private void onPlugged(boolean plugged) {
        Context context = this;

        if (plugged) {
            pluggedView.setImageLevel(LEVEL_PLUGGED);
            stopAlarm(context);
        } else {
            pluggedView.setImageLevel(LEVEL_UNPLUGGED);
            playAlarm(context);
        }
    }

    private Ringtone getRingtone(Context context) {
        if (ringtone == null) {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            ringtone = RingtoneManager.getRingtone(context, uri);
            if (ringtone == null) {
                uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                ringtone = RingtoneManager.getRingtone(context, uri);
                if (ringtone == null) {
                    uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                    ringtone = RingtoneManager.getRingtone(context, uri);
                }
            }
        }
        return ringtone;
    }

    private void playAlarm(Context context) {
        Ringtone ringtone = getRingtone(context);
        if ((ringtone != null) && !ringtone.isPlaying()) {
            ringtone.play();
        }
    }


    private void stopAlarm(Context context) {
        Ringtone ringtone = getRingtone(context);
        if ((ringtone != null) && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }
}
