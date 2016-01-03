package net.sf.power.monitor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateUtils;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

public class MainActivity extends Activity {

    private static final long POLL_RATE = DateUtils.SECOND_IN_MILLIS;

    private static final int LEVEL_UNPLUGGED = 0;
    private static final int LEVEL_PLUGGED = 1;

    private ImageView pluggedView;

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        pluggedView = (ImageView) findViewById(R.id.plugged);

        handler = new MainHandler(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        startPolling(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopPolling(this);
    }

    private void startPolling(Context context) {
        pollStatus();

        Intent service = new Intent(context, PowerConnectionService.class);
        context.startService(service);
    }

    private void stopPolling(Context context) {
        // Sticky intent doesn't need to unregister.
        if (handler != null) {
            handler.removeMessages(MainHandler.CHECK_STATUS);
        }

        Intent service = new Intent(context, PowerConnectionService.class);
        context.stopService(service);
    }

    private void checkStatus() {
        Context context = this;
        Intent intent = BatteryUtils.getBatteryIntent(context);
        Bundle extras = intent.getExtras();
        int plugged = extras.getInt(BatteryManager.EXTRA_PLUGGED, BatteryUtils.BATTERY_PLUGGED_NONE);
        onPlugged(plugged != BatteryUtils.BATTERY_PLUGGED_NONE);

        pollStatus();
    }

    private void pollStatus() {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(MainHandler.CHECK_STATUS, POLL_RATE);
        }
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
        if (plugged) {
            pluggedView.setImageLevel(LEVEL_PLUGGED);
        } else {
            pluggedView.setImageLevel(LEVEL_UNPLUGGED);
        }
    }
}
