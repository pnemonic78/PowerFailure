package net.sf.power.monitor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.TextView;

import java.lang.ref.WeakReference;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private static final long POLL_RATE = DateUtils.SECOND_IN_MILLIS;

    private TextView labelView;

    private static IntentFilter batteryFilter;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        labelView = (TextView) findViewById(R.id.label);

        handler = new MainHandler(this);
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
        printStatus(context);
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
            switch (msg.what) {
                case CHECK_STATUS:
                    activity.get().checkStatus();
                    break;
            }
        }
    }
}
