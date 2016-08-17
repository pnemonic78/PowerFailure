/*
 * Source file of the Power Failure Monitor project.
 * Copyright (c) 2016. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/2.0
 *
 * Contributors can be contacted by electronic mail via the project Web pages:
 *
 * https://sourceforge.net/projects/power-failure
 *
 * Contributor(s):
 *   Moshe Waisberg
 *
 */
package net.sf.power.monitor;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Vibrator;
import android.text.format.DateUtils;
import android.util.Log;

import net.sf.power.monitor.preference.PowerPreferences;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Power connection events service.
 *
 * @author Moshe Waisberg
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
     * Command to the service to start monitoring.
     */
    public static final int MSG_START_MONITOR = 3;
    /**
     * Command to the service to stop monitoring.
     */
    public static final int MSG_STOP_MONITOR = 4;
    /**
     * Command to the service to query the monitoring status.
     */
    public static final int MSG_GET_STATUS_MONITOR = 5;
    /**
     * Command to the clients about the monitoring status.
     */
    public static final int MSG_SET_STATUS_MONITOR = 6;
    /**
     * Command to check the battery status.
     */
    public static final int MSG_CHECK_BATTERY = 10;
    /**
     * Command to the clients that the battery status has been changed.
     */
    public static final int MSG_BATTERY_CHANGED = 11;

    private static final long POLL_RATE = DateUtils.SECOND_IN_MILLIS;
    private static final int ID_NOTIFY = R.string.start_monitor;

    private static final long[] VIBRATE_PATTERN = {DateUtils.SECOND_IN_MILLIS, DateUtils.SECOND_IN_MILLIS};

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
    private int notificationTextId;
    private int notificationIconId;
    private Ringtone ringtone;
    private long unpluggedSince;
    private boolean logging;
    private PowerPreferences settings;
    private boolean vibrating;

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
        settings = new PowerPreferences(this);

        stopAlarm();
        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification(R.string.monitor_stopped, R.mipmap.ic_launcher);
        checkBatteryStatus();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopLogging();
        stopPolling();
        stopAlarm();
        hideNotification();
    }

    private void startPolling() {
        Context context = this;
        printBatteryStatus(context);
        if (!handler.hasMessages(MSG_CHECK_BATTERY)) {
            pollBattery();
        }
    }

    private void stopPolling() {
        handler.removeMessages(MSG_CHECK_BATTERY);
        if (clients.isEmpty()) {
            stopSelf();
        }
    }

    private void checkBatteryStatus() {
        Context context = this;
        printBatteryStatus(context);

        if (handler != null) {
            int plugged = BatteryUtils.getPlugged(context);
            Message msg = handler.obtainMessage(MSG_BATTERY_CHANGED, plugged, 0);
            msg.sendToTarget();

            pollBattery();
        }
    }

    private void pollBattery() {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(MSG_CHECK_BATTERY, POLL_RATE);
        }
    }

    private void printBatteryStatus(Context context) {
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
                    service.registerClient(client);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    service.unregisterClient(client);
                    break;
                case MSG_START_MONITOR:
                    service.startLogging();
                    break;
                case MSG_STOP_MONITOR:
                    service.stopLogging();
                    break;
                case MSG_GET_STATUS_MONITOR:
                    service.notifyClients(MSG_SET_STATUS_MONITOR, service.logging ? 1 : 0, 0);
                    break;
                case MSG_CHECK_BATTERY:
                    service.checkBatteryStatus();
                    break;
                case MSG_BATTERY_CHANGED:
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
        } else if (logging && ((now - unpluggedSince) >= settings.getTimeDelay())) {
            playAlarm();
        } else {
            stopAlarm();
        }

        switch (plugged) {
            case BATTERY_PLUGGED_NONE:
                showNotification(R.string.plugged_unplugged, R.drawable.stat_plug_disconnect);
                break;
            case BATTERY_PLUGGED_AC:
                showNotification(R.string.plugged_ac, R.drawable.stat_plug_ac);
                break;
            case BATTERY_PLUGGED_USB:
                showNotification(R.string.plugged_usb, R.drawable.stat_plug_usb);
                break;
            case BATTERY_PLUGGED_WIRELESS:
                showNotification(R.string.plugged_wireless, R.drawable.stat_plug_wireless);
                break;
            default:
                showNotification(R.string.plugged_unknown, R.drawable.stat_plug_ac);
                break;
        }

        notifyClients(MSG_BATTERY_CHANGED, plugged, 0);
    }

    private Ringtone getRingtone(Context context) {
        return RingtoneManager.getRingtone(context, settings.getRingtone());
    }

    private void playAlarm() {
        Context context = this;
        playTone(context);
        vibrate(context, settings.isVibrate());
    }

    private void playTone(Context context) {
        Ringtone ringtone = getRingtone(context);
        if ((ringtone != null) && !ringtone.isPlaying()) {
            this.ringtone = ringtone;
            ringtone.play();
        }
    }

    private void stopAlarm() {
        Context context = this;
        stopTone();
        vibrate(context, false);
    }

    private void stopTone() {
        Ringtone ringtone = this.ringtone;
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
        // Are we already showing the notification?
        // Updating with same notification info causes flashing.
        if ((notificationTextId == textId) && (notificationIconId == largeIconId)) {
            return;
        }
        this.notificationTextId = textId;
        this.notificationIconId = largeIconId;

        Context context = this;
        Resources res = context.getResources();

        CharSequence title = res.getText(R.string.title_service);
        CharSequence text = res.getText(textId);

        // The PendingIntent to launch our activity if the user selects this notification.
        PendingIntent contentIntent = createActivityIntent(context);

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setLargeIcon(BitmapFactory.decodeResource(res, largeIconId))
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
        this.notificationTextId = 0;
        this.notificationIconId = 0;
    }

    private void registerClient(Messenger client) {
        if (!clients.contains(client)) {
            clients.add(client);
        }
        notifyClients(MSG_SET_STATUS_MONITOR, logging ? 1 : 0, 0);
        startPolling();
    }

    private void unregisterClient(Messenger client) {
        clients.remove(client);
        if (clients.isEmpty() && !logging) {
            stopSelf();
        }
    }

    private void notifyClients(int command, int arg1, int arg2) {
        Message msg;
        for (int i = clients.size() - 1; i >= 0; i--) {
            try {
                msg = Message.obtain(null, command, arg1, arg2);
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

    private void startLogging() {
        if (!logging) {
            logging = true;
            showNotification(R.string.monitor_started, R.mipmap.ic_launcher);
            //TODO Write logs.
        }
    }

    private void stopLogging() {
        if (logging) {
            logging = false;
            showNotification(R.string.monitor_stopped, R.mipmap.ic_launcher);
            //TODO Write logs.
        }
    }

    /**
     * Vibrate the device.
     *
     * @param context the context.
     * @param vibrate {@code true} to start vibrating - {@code false} to stop.
     */
    private void vibrate(Context context, boolean vibrate) {
        Vibrator vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
        if (vibrate) {
            if (!vibrating && vibrator.hasVibrator()) {
                vibrating = true;
                vibrator.vibrate(VIBRATE_PATTERN, 0);
            }
        } else if (vibrating) {
            vibrating = false;
            vibrator.cancel();
        }
    }
}
