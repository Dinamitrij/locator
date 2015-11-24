package lv.div.locator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.code.microlog4android.Logger;
import com.google.code.microlog4android.LoggerFactory;

/**
 * Receives AlarmManager RTC_WAKEUPs ... stops GPS polling
 */
public class TimeoutReceiver  extends BroadcastReceiver {

    private static final Logger log = LoggerFactory.getLogger();

    @Override
    public void onReceive(Context context, Intent intent) {
        log.debug(Utils.logtime(this.getClass()) + "onReceive() called");
        Main.mServiceInstance.gpsTimeout();

    }
}
