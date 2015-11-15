package lv.div.locator.actions;

import android.telephony.SmsManager;

import java.net.URL;
import java.util.Map;

import lv.div.locator.Const;
import lv.div.locator.Main;
import lv.div.locator.Utils;
import lv.div.locator.conf.ConfigurationKey;

public class ConfigReloader extends GenericConfigLoader {


    @Override
    protected void handleLoadException(String deviceId) {

        // Not fatal. Just using old configuration...

        SmsManager smsManager = SmsManager.getDefault();
        try {

            Map<ConfigurationKey, String> conf = Main.getInstance().config;

            if (Const.TRUE_FLAG.equals(conf.get(ConfigurationKey.DEVICE_SMS_ALERT_ENABLED))
                    && !Const.EMPTY.equals(conf.get(ConfigurationKey.DEVICE_SMS_ALERT_PHONE))) {

                String pingMessage = "Config NOT reloaded for \"" + conf.get(ConfigurationKey.DEVICE_ALIAS) + "\" " + Utils.fillPlaceholdersWithSystemVariables(conf.get(ConfigurationKey.DEVICE_PING_TEXT));

                if (pingMessage.length() > Const.MAX_MESSAGE_SIZE) {
                    pingMessage = pingMessage.substring(0, Const.MAX_MESSAGE_SIZE);
                }
                smsManager.sendTextMessage(conf.get(ConfigurationKey.DEVICE_SMS_ALERT_PHONE), null, pingMessage, null, null);
            }

        } catch (Exception e) {
            // quiet
        }


    }

    @Override
    protected void onPostExecute(Void result) {
        //Nothing to do.
    }
}
