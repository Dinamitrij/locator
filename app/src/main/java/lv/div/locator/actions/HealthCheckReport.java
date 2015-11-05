package lv.div.locator.actions;

import android.telephony.SmsManager;

import java.io.IOException;
import java.util.Map;

import lv.div.locator.Const;
import lv.div.locator.Main;
import lv.div.locator.Utils;
import lv.div.locator.conf.ConfigurationKey;

public class HealthCheckReport extends NetworkReport {


    @Override
    protected Boolean doInBackground(String... params) {

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

        Runtime runtime = Runtime.getRuntime();
        try {

            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int exitValue = ipProcess.waitFor();
            return (exitValue == 0);

        } catch (IOException e) {
            // quiet
        } catch (InterruptedException e) {
            // quiet
        }

        return false;
    }


    private void sendAlertBySMS() {
        SmsManager smsManager = SmsManager.getDefault();
        try {

            Map<ConfigurationKey, String> conf = Main.getInstance().config;

            if (Const.TRUE_FLAG.equals(conf.get(ConfigurationKey.DEVICE_SMS_ALERT_ENABLED))
                    && !Const.EMPTY.equals(conf.get(ConfigurationKey.DEVICE_SMS_ALERT_PHONE))) {

                String pingMessage = Utils.fillPlaceholdersWithSystemVariables(conf.get(ConfigurationKey.DEVICE_PING_TEXT)) + Const.NO_INTERNET_SMS_ALERT_POSTFIX;

                if (pingMessage.length() > Const.MAX_MESSAGE_SIZE) {
                    pingMessage = pingMessage.substring(0, Const.MAX_MESSAGE_SIZE);
                }
                smsManager.sendTextMessage(conf.get(ConfigurationKey.DEVICE_SMS_ALERT_PHONE), null, pingMessage, null, null);
            }

        } catch (Exception e) {
            // quiet
        }


    }


}
