package lv.div.locator.actions;

import android.telephony.SmsManager;

import java.io.IOException;
import java.util.Map;

import lv.div.locator.Main;
import lv.div.locator.Utils;
import lv.div.locator.commons.conf.ConfigurationKey;
import lv.div.locator.commons.conf.Const;
import lv.div.locator.conf.Constant;
import lv.div.locator.utils.FLogger;


public class HealthCheckReport extends NetworkReport {

    @Override
    protected Boolean doInBackground(String... params) {
        FLogger.getInstance().log(this.getClass(), "doInBackground() called");

        boolean online = isOnline();
        if (online) {
            super.doInBackground(params);
        } else {
            sendAlertBySMS();
        }

        return true;
    }


    /**
     * Ping Google DNS to check we're really online.
     *
     * @return
     */
    private boolean isOnline() {
        FLogger.getInstance().log(this.getClass(), "isOnline() called");
        Runtime runtime = Runtime.getRuntime();
        try {

            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int exitValue = ipProcess.waitFor();
            FLogger.getInstance().log(this.getClass(), "isOnline() look OK.");
            return (exitValue == 0);

        } catch (IOException e) {
            FLogger.getInstance().log(this.getClass(), "IOException!");
            FLogger.getInstance().log(this.getClass(), "----> " + e.getMessage());
            FLogger.getInstance().log(this.getClass(), Utils.stToString(e.getStackTrace()));
        } catch (InterruptedException e) {
            FLogger.getInstance().log(this.getClass(), "InterruptedException!");
            FLogger.getInstance().log(this.getClass(), "----> " + e.getMessage());
            FLogger.getInstance().log(this.getClass(), Utils.stToString(e.getStackTrace()));
        }

        return false;
    }


    private void sendAlertBySMS() {
        FLogger.getInstance().log(this.getClass(), "sendAlertBySMS() called");
        SmsManager smsManager = SmsManager.getDefault();
        try {

            Map<ConfigurationKey, String> conf = Main.getInstance().config;

            if (Const.TRUE_FLAG.equals(conf.get(ConfigurationKey.DEVICE_SMS_ALERT_ENABLED))
                    && !Const.EMPTY.equals(conf.get(ConfigurationKey.DEVICE_SMS_ALERT_PHONE))) {

                String pingMessage = Utils.fillPlaceholdersWithSystemVariables(conf.get(ConfigurationKey.DEVICE_PING_TEXT)) + Constant.NO_INTERNET_SMS_ALERT_POSTFIX;

                if (pingMessage.length() > Constant.MAX_MESSAGE_SIZE) {
                    pingMessage = pingMessage.substring(0, Constant.MAX_MESSAGE_SIZE);
                }
                smsManager.sendTextMessage(conf.get(ConfigurationKey.DEVICE_SMS_ALERT_PHONE), null, pingMessage, null, null);
                FLogger.getInstance().log(this.getClass(), "sendAlertBySMS() looks OK.");
            }

        } catch (Exception e) {
            FLogger.getInstance().log(this.getClass(), "----> " + e.getMessage());
            FLogger.getInstance().log(this.getClass(), Utils.stToString(e.getStackTrace()));
        }


    }


}
