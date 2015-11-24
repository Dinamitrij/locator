package lv.div.locator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import lv.div.locator.utils.FLogger;


/**
 * Receives AlarmManager RTC_WAKEUPs ... stops GPS polling
 */
public class TimeoutReceiver  extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        FLogger.getInstance().log(this.getClass(), "onReceive() called");
        Main.mServiceInstance.gpsTimeout();

    }
}
