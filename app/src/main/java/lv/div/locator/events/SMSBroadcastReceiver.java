package lv.div.locator.events;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import java.util.Map;

import lv.div.locator.Main;
import lv.div.locator.commons.conf.ConfigurationKey;
import lv.div.locator.commons.conf.Const;
import lv.div.locator.conf.Constant;


/**
 * Start Locator by receiving SMS with "#LOCSTART" text (#LOCSTOP)
 */
public class SMSBroadcastReceiver extends BroadcastReceiver {

    private static final String SMSACTION = "android.provider.Telephony.SMS_RECEIVED";


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(SMSACTION)) {
            //StringBuilder sb = new StringBuilder();

            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                // Get all messages contained in the Intent
                SmsMessage[] messages =
                        Telephony.Sms.Intents.getMessagesFromIntent(intent);

                String smsToCheck = Const.EMPTY;
                // Feed the StringBuilder with all Messages found.
                for (SmsMessage currentMessage : messages) {
                    //sb.append(currentMessage.getDisplayOriginatingAddress());
                    smsToCheck = currentMessage.getDisplayMessageBody();
                }

                if (Constant.SMS_COMMAND_START_APP.equalsIgnoreCase(smsToCheck)) {
                    // Start application
                    Intent thisApplication = new Intent(context, Main.class);
                    thisApplication.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(thisApplication);
                } else if (Constant.SMS_COMMAND_STOP_APP.equalsIgnoreCase(smsToCheck)) {
                    // Stop application: overwriting some config values to emulate the need of shutdown
                    try {
                        Map<ConfigurationKey, String> cfg = Main.getInstance().config;
                        cfg.put(ConfigurationKey.DEVICE_APP_SHUTDOWN_ENABLED, Const.TRUE_FLAG);
                        cfg.put(ConfigurationKey.DEVICE_APP_SHUTDOWN_TIME, "00:00:00");
                        cfg.put(ConfigurationKey.DEVICE_SHUTDOWN_IF_NOT_IN_SAFE_ZONE, Const.TRUE_FLAG);
                    } catch (Exception e) {
                        // be quiet...
                    }
                    // and wait...
                }


            }
        }
    }
}
