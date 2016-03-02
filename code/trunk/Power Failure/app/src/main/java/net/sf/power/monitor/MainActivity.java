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
 * https://sourceforge.net/projects/power-failure/
 *
 * Contributor(s):
 *   Moshe Waisberg
 *
 */
package net.sf.power.monitor;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

/**
 * Main activity.
 *
 * @author Moshe Waisberg
 */
public class MainActivity extends Activity implements BatteryListener {

    private static final String TAG = "MainActivity";

    private static final int LEVEL_UNKNOWN = 0;
    private static final int LEVEL_UNPLUGGED = 1;
    private static final int LEVEL_PLUGGED_AC = 2;
    private static final int LEVEL_PLUGGED_USB = 3;
    private static final int LEVEL_PLUGGED_WIRELESS = 4;
    private static final int LEVEL_PLUGGED_UNKNOWN = LEVEL_PLUGGED_AC;

    private ImageView pluggedView;
    private MenuItem menuItemStart;
    private MenuItem menuItemStop;

    private Handler handler;
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private Messenger messenger;
    /**
     * Messenger for communicating with service.
     */
    private Messenger service;
    /**
     * Flag indicating whether we have called bind on the service.
     */
    private boolean serviceIsBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        pluggedView = (ImageView) findViewById(R.id.plugged);
        pluggedView.setImageLevel(LEVEL_UNKNOWN);

        handler = new MainHandler(this);
        messenger = new Messenger(handler);
        bindService();

        onBatteryPlugged(BatteryUtils.getPlugged(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService();
    }

    private void startMonitor() {
        // We want to monitor the service for as long as we are connected to it.
        try {
            notifyService(PowerConnectionService.MSG_START_MONITOR);
            setMonitorStatus(true);
        } catch (RemoteException e) {
            // In this case the service has crashed before we could even
            // do anything with it; we can count on soon being
            // disconnected (and then reconnected if it can be restarted)
            // so there is no need to do anything here.
        }
    }

    private void stopMonitor() {
        try {
            notifyService(PowerConnectionService.MSG_STOP_MONITOR);
            setMonitorStatus(false);
        } catch (RemoteException e) {
            // In this case the service has crashed before we could even
            // do anything with it; we can count on soon being
            // disconnected (and then reconnected if it can be restarted)
            // so there is no need to do anything here.
        }
    }

    private void setMonitorStatus(final boolean polling) {
        pluggedView.setImageLevel(LEVEL_UNKNOWN);
        if (menuItemStart != null) {
            menuItemStart.setVisible(!polling);
            menuItemStop.setVisible(polling);
        }
        onBatteryPlugged(BatteryUtils.getPlugged(MainActivity.this));
    }

    @Override
    public void onBatteryPlugged(int plugged) {
        switch (plugged) {
            case BATTERY_PLUGGED_NONE:
                pluggedView.setImageLevel(LEVEL_UNPLUGGED);
                pluggedView.setContentDescription(getText(R.string.plugged_unplugged));
                break;
            case BATTERY_PLUGGED_AC:
                pluggedView.setImageLevel(LEVEL_PLUGGED_AC);
                pluggedView.setContentDescription(getText(R.string.plugged_ac));
                break;
            case BATTERY_PLUGGED_USB:
                pluggedView.setImageLevel(LEVEL_PLUGGED_USB);
                pluggedView.setContentDescription(getText(R.string.plugged_usb));
                break;
            case BATTERY_PLUGGED_WIRELESS:
                pluggedView.setImageLevel(LEVEL_PLUGGED_WIRELESS);
                pluggedView.setContentDescription(getText(R.string.plugged_wireless));
                break;
            default:
                pluggedView.setImageLevel(LEVEL_PLUGGED_UNKNOWN);
                pluggedView.setContentDescription(getText(R.string.plugged_unknown));
                break;
        }
    }

    private static class MainHandler extends Handler {

        private static final int MSG_STATUS_CHANGED = PowerConnectionService.MSG_BATTERY_CHANGED;
        private static final int MSG_START_MONITOR = PowerConnectionService.MSG_START_MONITOR;
        private static final int MSG_STOP_MONITOR = PowerConnectionService.MSG_STOP_MONITOR;
        private static final int MSG_SET_STATUS_MONITOR = PowerConnectionService.MSG_SET_STATUS_MONITOR;

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
                case MSG_STATUS_CHANGED:
                    activity.onBatteryPlugged(msg.arg1);
                    break;
                case MSG_START_MONITOR:
                    activity.startMonitor();
                    break;
                case MSG_STOP_MONITOR:
                    activity.stopMonitor();
                    break;
                case MSG_SET_STATUS_MONITOR:
                    activity.setMonitorStatus(msg.arg1 != 0);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection connection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            service = new Messenger(binder);
            Log.i(TAG, "Service connected.");

            registerClient();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            service = null;
            Log.i(TAG, "Service disconnected.");
        }
    };

    private void bindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        Intent intent = new Intent(this, PowerConnectionService.class);

        //This will keep service running even after activity destroyed.
        startService(intent);

        bindService(intent, connection, BIND_AUTO_CREATE);
        serviceIsBound = true;
        Log.i(TAG, "Service binding.");
    }

    private void unbindService() {
        if (serviceIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            unregisterClient();

            // Detach our existing connection.
            unbindService(connection);
            serviceIsBound = false;
            Log.i(TAG, "Service unbinding.");
        }
    }

    /**
     * Register this client with the service to receive commands.
     */
    private void registerClient() {
        // We want to monitor the service for as long as we are connected to it.
        try {
            notifyService(PowerConnectionService.MSG_REGISTER_CLIENT);
            Log.i(TAG, "Registered with service.");
        } catch (RemoteException e) {
            // In this case the service has crashed before we could even
            // do anything with it; we can count on soon being
            // disconnected (and then reconnected if it can be restarted)
            // so there is no need to do anything here.
        }
    }

    /**
     * Unregister this client from the service to stop receiving commands.
     */
    private void unregisterClient() {
        try {
            notifyService(PowerConnectionService.MSG_UNREGISTER_CLIENT);
            Log.i(TAG, "Unregistered from service.");
        } catch (RemoteException e) {
            // There is nothing special we need to do if the service has crashed.
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        menuItemStart = menu.findItem(R.id.start);
        menuItemStop = menu.findItem(R.id.stop);

        try {
            notifyService(PowerConnectionService.MSG_GET_STATUS_MONITOR);
        } catch (RemoteException e) {
            // In this case the service has crashed before we could even
            // do anything with it; we can count on soon being
            // disconnected (and then reconnected if it can be restarted)
            // so there is no need to do anything here.
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.start:
                handler.sendEmptyMessage(MainHandler.MSG_START_MONITOR);
                return true;
            case R.id.stop:
                handler.sendEmptyMessage(MainHandler.MSG_STOP_MONITOR);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void notifyService(int command) throws RemoteException {
        if (serviceIsBound && (service != null)) {
            Message msg = Message.obtain(null, command);
            msg.replyTo = messenger;
            service.send(msg);
        }
    }
}
