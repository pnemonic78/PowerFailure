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
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Power connection events service.
 * Created by moshe.w on 03/01/2016.
 */
public class PowerConnectionService extends Service implements BatteryListener {

    private static final String TAG = "PowerConnectionService";

    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    public static final int MSG_REGISTER_CLIENT = 1;

    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    public static final int MSG_UNREGISTER_CLIENT = 2;
    /**
     * Command to check the battery status.
     */
    public static final int MSG_CHECK_STATUS = 10;
    /**
     * Command to the clients that the battery status has been changed.
     */
    public static final int MSG_PLUGGED = 11;

    private static final long POLL_RATE = DateUtils.SECOND_IN_MILLIS;
    private static final long ALARM_THRESHOLD = DateUtils.SECOND_IN_MILLIS * 5;

    private Handler handler;
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private Messenger messenger;
    /**
     * Keeps track of all current registered clients.
     */
    private final List<Messenger> clients = new ArrayList<Messenger>();

    private Ringtone ringtone;
    private long unpluggedSince;

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Context context = this;
        handler = new PowerConnectionHandler(this);
        messenger = new Messenger(handler);

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
            handler.removeMessages(MSG_CHECK_STATUS);
        }
    }

    private void checkStatus() {
        Context context = this;
        BatteryUtils.printStatus(context);
        int plugged = BatteryUtils.getPlugged(context);

        Message msg = handler.obtainMessage(MSG_PLUGGED, plugged, 0);
        msg.sendToTarget();

        pollStatus();
    }

    private void pollStatus() {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(MSG_CHECK_STATUS, POLL_RATE);
        }
    }

    private void printStatus(Context context) {
        BatteryUtils.printStatus(context);
    }

    private static class PowerConnectionHandler extends Handler {

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
                case MSG_REGISTER_CLIENT:
                    service.clients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    service.clients.remove(msg.replyTo);
                    break;
                case MSG_CHECK_STATUS:
                    service.checkStatus();
                    break;
                case MSG_PLUGGED:
                    service.onBatteryPlugged(msg.arg1);
                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    public void onBatteryPlugged(int plugged) {
        Context context = this;
        long now = SystemClock.uptimeMillis();

        if (plugged != BATTERY_PLUGGED_NONE) {
            unpluggedSince = now;
            stopAlarm(context);
        } else {
            if ((now - unpluggedSince) >= ALARM_THRESHOLD) {
                playAlarm(context);
            } else {
                stopAlarm(context);
            }
        }

        Message msg;
        for (int i = clients.size() - 1; i >= 0; i--) {
            try {
                msg = Message.obtain(null, MSG_PLUGGED, plugged, 0);
                clients.get(i).send(msg);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to send status update", e);
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                clients.remove(i);
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
