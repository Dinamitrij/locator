package lv.div.locator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import lv.div.locator.conf.Constant;
import lv.div.locator.utils.FLogger;

/**
 * Receives AlarmManager RTC_WAKEUPs
 */
public class MainReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Do not proceed, if we're shutting down now...
//        if (!Main.getInstance().shuttingDown) {
            FLogger.getInstance().log(this.getClass(), "onReceive() called. Working...");
            Main.getInstance().startup();
//        }
    }
}
