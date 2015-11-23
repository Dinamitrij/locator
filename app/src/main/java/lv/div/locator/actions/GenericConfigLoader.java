package lv.div.locator.actions;


import android.os.AsyncTask;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import lv.div.locator.Main;
import lv.div.locator.commons.conf.ConfigurationKey;
import lv.div.locator.commons.conf.Const;
import lv.div.locator.conf.Constant;


public abstract class GenericConfigLoader extends AsyncTask<Void, Void, Void> {
    private HttpURLConnection urlConnection;

    @Override
    protected void onPreExecute() {
    }

    protected URL buildURL(String deviceId) throws Exception {
        return new URL(String.format(Constant.CONFIG_DOWNLOAD_URL_MASK, deviceId));
    }


    @Override
    protected Void doInBackground(Void... params) {
        String deviceId = Main.getInstance().buildDeviceId();

        try {

            URL url = buildURL(deviceId);

            urlConnection = (HttpURLConnection) url.openConnection();

            InputStream in = urlConnection.getInputStream();
            InputStreamReader isw = new InputStreamReader(in);

            String str;
            StringBuilder sb = new StringBuilder();
            int data = isw.read();
            while (data != -1) {
                char current = (char) data;
                sb.append(current);
                data = isw.read();
            }

            Map<ConfigurationKey, String> tmpConfig = new HashMap<>();
            str = sb.toString();
            String[] split = str.split("\n");
            for (String line : split) {
                String[] keyVal = line.split(" = ");
                if (keyVal.length == 2) {
                    String name = keyVal[0];
                    ConfigurationKey key = ConfigurationKey.valueOf(name);
                    String value = keyVal[1];
                    tmpConfig.put(key, value);
                }
            }

            // If we need BSSIDs, then GPS should always be "ON":
            if (Const.TRUE_FLAG.equals(tmpConfig.get(ConfigurationKey.DEVICE_COLLECT_BSSID_MODE_ENABLED))) {
                tmpConfig.put(ConfigurationKey.SAFE_ZONE_WIFI, "nozone");
            }

            Main.getInstance().config.clear();
            Main.getInstance().config = tmpConfig;

        } catch (Exception e) {
            handleLoadException(deviceId);
        } finally {
            try {
                urlConnection.disconnect();
            } catch (Exception e) {
            }
        }


        return null;
    }

    protected abstract void handleLoadException(String deviceId);

    protected abstract void onPostExecute(Void result);

}


