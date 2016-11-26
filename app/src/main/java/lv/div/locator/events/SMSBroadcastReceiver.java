package lv.div.locator.events;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

import java.util.Map;

import lv.div.locator.Main;
import lv.div.locator.commons.conf.ConfigurationKey;
import lv.div.locator.commons.conf.Const;
import lv.div.locator.conf.Constant;


/**
 * Handler for SMS received commands like #LOCSTART, #LOCSTOP, etc.
 */
public class SMSBroadcastReceiver extends BroadcastReceiver {

    private static final String SMSACTION = "android.provider.Telephony.SMS_RECEIVED";


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(SMSACTION)) {

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
                } else if (Constant.SMS_COMMAND_DIAGNOSE_APP.equalsIgnoreCase(smsToCheck)) {
                    // Send some telemetry data to the admin's phone
                    sendTelemetry();
                }


            }
        }
    }

    private synchronized void sendTelemetry() {
        Map<ConfigurationKey, String> cfg = Main.getInstance().config;
        SmsManager smsManager = SmsManager.getDefault();
        try {

            if (Const.TRUE_FLAG.equals(cfg.get(ConfigurationKey.DEVICE_SMS_ALERT_ENABLED))
                    && !Const.EMPTY.equals(cfg.get(ConfigurationKey.DEVICE_SMS_ALERT_PHONE))) {

                String telemetryMessage = cfg.get(ConfigurationKey.DEVICE_ALIAS) + " B: " + Main.getInstance().getBatteryStatus() + Const.SPACE +
                        Main.getInstance().gpsDataCache + Const.SPACE + Main.getInstance().wifiCache;

                if (telemetryMessage.length() > Constant.MAX_MESSAGE_SIZE) {
                    telemetryMessage = telemetryMessage.substring(0, Constant.MAX_MESSAGE_SIZE);
                }
                smsManager.sendTextMessage(cfg.get(ConfigurationKey.DEVICE_SMS_ALERT_PHONE), null, telemetryMessage, null, null);
            }

        } catch (Exception e) {
            // quiet
        }
    }



}
