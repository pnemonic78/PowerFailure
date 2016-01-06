package net.sf.power.monitor;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
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
    public static final int MSG_STATUS_CHANGED = 11;

    private static final long POLL_RATE = DateUtils.SECOND_IN_MILLIS;
    private static final long ALARM_DELAY = DateUtils.SECOND_IN_MILLIS * 15;
    private static final int ID_NOTIFY = R.string.start_monitor;

    private Handler handler;
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private Messenger messenger;
    /**
     * Keeps track of all current registered clients.
     */
    private final List<Messenger> clients = new ArrayList<Messenger>();
    private NotificationManager notificationManager;
    private Ringtone ringtone;
    private long unpluggedSince;
    private boolean polling;

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new PowerConnectionHandler(this);
        messenger = new Messenger(handler);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        stopAlarm();
        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification(R.string.polling_stopped, R.mipmap.ic_launcher);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopPolling();
        stopAlarm();
        hideNotification();
    }

    private void startPolling() {
        Context context = this;
        printStatus(context);
        pollStatus();
        polling = true;
        showNotification(R.string.polling_started, R.mipmap.ic_launcher);
    }

    private void stopPolling() {
        // Sticky intent doesn't need to unregister.
        polling = false;
        showNotification(R.string.polling_stopped, R.mipmap.ic_launcher);
    }

    private void checkStatus() {
        Context context = this;
        printStatus(context);

        if (polling && (handler != null)) {
            int plugged = BatteryUtils.getPlugged(context);

            Message msg = handler.obtainMessage(MSG_STATUS_CHANGED, plugged, 0);
            msg.sendToTarget();

            pollStatus();
        }
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
            Messenger client = msg.replyTo;

            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    if (!service.clients.contains(client)) {
                        service.clients.add(client);
                        service.startPolling();
                    }
                    break;
                case MSG_UNREGISTER_CLIENT:
                    service.clients.remove(msg.replyTo);
                    if (service.clients.isEmpty()) {
                        service.stopSelf();
                    }
                    break;
                case MSG_CHECK_STATUS:
                    service.checkStatus();
                    break;
                case MSG_STATUS_CHANGED:
                    service.onBatteryPlugged(msg.arg1);
                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    public void onBatteryPlugged(int plugged) {
        long now = SystemClock.uptimeMillis();

        if (plugged != BATTERY_PLUGGED_NONE) {
            unpluggedSince = now;
            stopAlarm();
        } else {
            if ((now - unpluggedSince) >= ALARM_DELAY) {
                playAlarm();
            } else {
                stopAlarm();
            }
        }

        switch (plugged) {
            case BATTERY_PLUGGED_NONE:
                showNotification(R.string.plugged_unplugged, R.mipmap.ic_launcher);
                break;
            case BATTERY_PLUGGED_AC:
                showNotification(R.string.plugged_ac, R.mipmap.ic_launcher);
                break;
            case BATTERY_PLUGGED_USB:
                showNotification(R.string.plugged_usb, R.mipmap.ic_launcher);
                break;
            case BATTERY_PLUGGED_WIRELESS:
                showNotification(R.string.plugged_wireless, R.mipmap.ic_launcher);
                break;
            default:
                showNotification(R.string.plugged_unknown, R.mipmap.ic_launcher);
                break;
        }

        Message msg;
        for (int i = clients.size() - 1; i >= 0; i--) {
            try {
                msg = Message.obtain(null, MSG_STATUS_CHANGED, plugged, 0);
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

    private void playAlarm() {
        Context context = this;
        Ringtone ringtone = getRingtone(context);
        if ((ringtone != null) && !ringtone.isPlaying()) {
            ringtone.play();
        }
    }

    private void stopAlarm() {
        Context context = this;
        Ringtone ringtone = getRingtone(context);
        if ((ringtone != null) && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }

    /**
     * Show a notification while this service is running.
     *
     * @param textId      the text resource id.
     * @param largeIconId the large icon resource id.
     */
    private void showNotification(int textId, int largeIconId) {
        Context context = this;
        Resources res = context.getResources();

        CharSequence title = res.getText(R.string.title_service);
        CharSequence text = res.getText(textId);

        // The PendingIntent to launch our activity if the user selects this notification.
        PendingIntent contentIntent = createActivityIntent(context);

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setOngoing(true)
                //.setLargeIcon(BitmapFactory.decodeResource(res, largeIconId))
                .setSmallIcon(R.drawable.stat_launcher)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(title)  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .getNotification();

        // Send the notification.
        notificationManager.notify(ID_NOTIFY, notification);
    }

    private PendingIntent createActivityIntent(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(context.getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, ID_NOTIFY, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    /**
     * Cancel the persistent notification.
     */
    private void hideNotification() {
        notificationManager.cancel(ID_NOTIFY);
    }
}
