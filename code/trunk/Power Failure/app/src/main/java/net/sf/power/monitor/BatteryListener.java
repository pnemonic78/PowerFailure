package net.sf.power.monitor;

/**
 * Created by moshe.w on 03/01/2016.
 */
public interface BatteryListener {

    int BATTERY_PLUGGED_NONE = BatteryUtils.BATTERY_PLUGGED_NONE;

    /**
     * Notification that the battery is being been charged.
     *
     * @param plugged the type of plugged-in device.
     */
    void onBatteryPlugged(int plugged);

}
