package net.sf.power.monitor;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

public class MainActivity extends Activity implements BatteryListener, View.OnClickListener {

    private static final String TAG = "MainActivity";

    private static final int LEVEL_UNKNOWN = 0;
    private static final int LEVEL_UNPLUGGED = 1;
    private static final int LEVEL_PLUGGED = 2;

    private static final int LEVEL_START = 0;
    private static final int LEVEL_STOP = 1;

    private ImageView pluggedView;
    private ImageButton playButton;

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
        playButton = (ImageButton) findViewById(R.id.play);
        playButton.setImageLevel(LEVEL_START);
        playButton.setOnClickListener(this);

        handler = new MainHandler(this);
        messenger = new Messenger(handler);
        bindService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService();
    }

    private void startMonitor() {
        pluggedView.setImageLevel(LEVEL_UNKNOWN);
        playButton.setImageLevel(LEVEL_STOP);
        registerClient();
    }

    private void stopMonitor() {
        pluggedView.setImageLevel(LEVEL_UNKNOWN);
        playButton.setImageLevel(LEVEL_START);
        unregisterClient();
    }

    @Override
    public void onBatteryPlugged(int plugged) {
        if (plugged != BATTERY_PLUGGED_NONE) {
            pluggedView.setImageLevel(LEVEL_PLUGGED);
        } else {
            pluggedView.setImageLevel(LEVEL_UNPLUGGED);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == playButton) {
            switch (playButton.getDrawable().getLevel()) {
                case LEVEL_START:
                    startMonitor();
                    break;
                case LEVEL_STOP:
                    stopMonitor();
                    break;
            }
        }
    }

    private static class MainHandler extends Handler {

        private static final int MSG_STATUS_CHANGED = PowerConnectionService.MSG_STATUS_CHANGED;

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
        bindService(new Intent(this, PowerConnectionService.class), connection, Context.BIND_AUTO_CREATE);
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
    public void registerClient() {
        if (serviceIsBound && (service != null)) {
            // We want to monitor the service for as long as we are connected to it.
            try {
                Message msg = Message.obtain(null, PowerConnectionService.MSG_REGISTER_CLIENT);
                msg.replyTo = messenger;
                service.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
            Log.i(TAG, "Registered with service.");
        }
    }

    /**
     * Unregister this client from the service to stop receiving commands.
     */
    private void unregisterClient() {
        if (serviceIsBound && (service != null)) {
            try {
                Message msg = Message.obtain(null, PowerConnectionService.MSG_UNREGISTER_CLIENT);
                msg.replyTo = messenger;
                service.send(msg);
            } catch (RemoteException e) {
                // There is nothing special we need to do if the service has crashed.
            }
            Log.i(TAG, "Unregistered from service.");
        }
    }
}
