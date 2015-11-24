package lv.div.locator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import lv.div.locator.utils.FLogger;

/**
 * Receives AlarmManager RTC_WAKEUPs
 */
public class MainReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        FLogger.getInstance().log(this.getClass(), "onReceive() called");
        Main.getInstance().startup();
    }
}
