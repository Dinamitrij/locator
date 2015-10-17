package lv.div.locator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receives AlarmManager RTC_WAKEUPs ... stops GPS polling
 */
public class TimeoutReceiver  extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Main.mServiceInstance.gpsTimeout();


    }
}
