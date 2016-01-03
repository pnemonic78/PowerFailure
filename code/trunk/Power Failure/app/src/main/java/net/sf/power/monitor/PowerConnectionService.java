package net.sf.power.monitor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.text.format.DateUtils;

import java.lang.ref.WeakReference;

/**
 * Power connection events service.
 * Created by moshe.w on 03/01/2016.
 */
public class PowerConnectionService extends Service {

    private static final String TAG = "PowerConnectionService";

    private static final long POLL_RATE = DateUtils.SECOND_IN_MILLIS;
    private static final long ALARM_THRESHOLD = DateUtils.SECOND_IN_MILLIS * 5;

    private Handler handler;
    private Ringtone ringtone;
    private long unpluggedTime;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Context context = this;
        handler = new PowerConnectionHandler(this);
        stopAlarm(context);
        startPolling(context);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Context context = this;
        stopPolling(context);
    }


    private void startPolling(Context context) {
        printStatus(context);
        pollStatus();
    }

    private void stopPolling(Context context) {
        // Sticky intent doesn't need to unregister.
        if (handler != null) {
            handler.removeMessages(PowerConnectionHandler.CHECK_STATUS);
        }
    }

    private void checkStatus() {
        Context context = this;
        BatteryUtils.printStatus(context);
        onPlugged(BatteryUtils.isPlugged(context));

        pollStatus();
    }

    private void pollStatus() {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(PowerConnectionHandler.CHECK_STATUS, POLL_RATE);
        }
    }

    private void printStatus(Context context) {
        BatteryUtils.printStatus(context);
    }

    private static class PowerConnectionHandler extends Handler {

        private static final int CHECK_STATUS = 1;

        private final WeakReference<PowerConnectionService> service;

        public PowerConnectionHandler(PowerConnectionService service) {
            this.service = new WeakReference<PowerConnectionService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            PowerConnectionService service = this.service.get();
            if (service == null) {
                return;
            }

            switch (msg.what) {
                case CHECK_STATUS:
                    service.checkStatus();
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
        long now = SystemClock.uptimeMillis();

        if (plugged) {
            unpluggedTime = now;
            stopAlarm(context);
        } else {
            if ((now - unpluggedTime) >= ALARM_THRESHOLD) {
                playAlarm(context);
            } else {
                stopAlarm(context);
            }
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
