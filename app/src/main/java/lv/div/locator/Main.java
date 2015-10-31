package lv.div.locator;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import lv.div.locator.actions.HttpReportSender;
import lv.div.locator.actions.NetworkReport;
import lv.div.locator.conf.ConfigurationKey;

public class Main extends AppCompatActivity {
    //public class Main extends Application {

    public static BackgroundService mServiceInstance;
    public static Map<ConfigurationKey, String> config = new HashMap<>();
    public static LocationManager locationManager = null;
    public static DeviceLocationListener deviceLocationListener = new DeviceLocationListener();
    public static Location currentBestLocation;
    protected static Main mInstance;
    public Date healthCheckTime = new Date(0);
    private WifiManager wifi;
    private List<String> safeWifi = new ArrayList<>();
    private String deviceId;
    private HttpURLConnection urlConnection;

    public static Main getInstance() {
        return mInstance;
    }

    public static void sendAlert(String text) {
//        if (!Main.getInstance().config.isEmpty()) {
//            NetworkReport1 networkReport = new NetworkReport1();
//
//            try {
//                String reportAddress = String.format(Main.getInstance().config.get(ConfigurationKey.DEVICE_SEND_ALERT_ADDRESS),
//                        Main.getInstance().config.get(ConfigurationKey.SEND_ALERT_ADDRESS_PARAM1),
//                        URLEncoder.encode(Main.getInstance().config.get(ConfigurationKey.DEVICE_ALIAS) + text, Const.UTF8_ENCODING));
//
//                networkReport.execute(reportAddress);
//            } catch (Exception e) {
//                // Cannot send alert!
//                int a=1+2;
//            }
//
//
//        }

    }


//    @Override
//    protected void onResume() {
//        super.onResume();
//        stopService(new Intent(Main.this,
//                BackgroundService.class));
//        if (isService) {
//            isService = false;
//        }
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInstance = this;
        mServiceInstance = null;

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        safeWifi.add("www.div.lv");

//        NetworkConfigurationLoader confLoader = new NetworkConfigurationLoader();
//        confLoader.execute(String.format(Const.CONFIG_DOWNLOAD_URL_MASK, buildDeviceId()));


        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        // Create HTTP report sender:
        HttpReportSender httpReportSender = new HttpReportSender();

        ConfigLoader configLoader = new ConfigLoader();
        configLoader.execute();

//        Main.getInstance().sendAlert("Locator started");


//        Intent i = new Intent(Main.this, BackgroundService.class);
//        startService(i);
//        finish();
    }

    public void startApplication() {
        Intent i = new Intent(Main.this, BackgroundService.class);
        startService(i);
        finish();
    }

    public void startup() {
//        wakeLock1(true);
        Intent i = new Intent(this, BackgroundService.class);
        startService(i);
    }

    public String getBatteryStatus() {
        int level = mServiceInstance.batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        return String.valueOf(level);
    }

    public void healthCheck() {
        if (!Utils.clockTicked(healthCheckTime, Const.HEALTH_CHECK_TIME_MSEC)) {
            return;
        }
        Date now = new Date();
        String time = null;
        try {
            time = URLEncoder.encode("healthCheck from " + config.get(ConfigurationKey.DEVICE_ALIAS) + Const.SPACE + now.toString(), Const.UTF8_ENCODING);
        } catch (UnsupportedEncodingException e) {
            time = "0";
        }


        String urlAddress = String.format(Const.HEALTH_CHECK_URL_MASK, time);
        NetworkReport networkReport = new NetworkReport();
        networkReport.execute(urlAddress);

        healthCheckTime = new Date();

    }

    public String getWifiNetworks() {


        if (wifi.isWifiEnabled() == false) {
            wifi.setWifiEnabled(true);
        }


        wifi.startScan();
        // get list of the results in object format ( like an array )
        List<ScanResult> results = wifi.getScanResults();

        SortedMap<Integer, String> networks = new TreeMap<>(Collections.reverseOrder());
        for (ScanResult result : results) {
            networks.put(result.level, result.SSID);
        }

        Set<Map.Entry<Integer, String>> entries = networks.entrySet();
        Iterator<Map.Entry<Integer, String>> iterator = entries.iterator();
        StringBuffer sb = new StringBuffer();
        while (iterator.hasNext()) {
            Map.Entry<Integer, String> network = iterator.next();
            sb.append(network.getValue());
            sb.append(Const.SPACE);
            sb.append(network.getKey());
            sb.append("; ");
        }

//        wifiCache = sb.toString();
//        wifiCacheDate = new Date();
        return sb.toString();
    }

    /**
     * Is device in safe zone?
     * Safe zone = zone within particular WiFi network range
     *
     * @param wifiNetworks
     * @return
     */
    public boolean isInSafeZone(String wifiNetworks) {
        //TODO: Add logic here! If there's no Starting/Ending WiFi ect.

        for (String wifiNet : safeWifi) {
            if (wifiNetworks.indexOf(wifiNet) >= 0) {
                return true;
            }

        }

        return false;
    }

    /**
     * Generating Device ID
     */
    public String buildDeviceId() {

        if (null == deviceId) {
            try {
                final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);

                final String tmDevice, tmSerial, androidId;
                tmDevice = Const.EMPTY + tm.getDeviceId();
                tmSerial = Const.EMPTY + tm.getSimSerialNumber();
                androidId = Const.EMPTY + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

                UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
                deviceId = deviceUuid.toString();

            } catch (Exception e) {
                deviceId = String.valueOf(Math.round(Math.random() * 999999));
            }

        }
        return deviceId;
    }

    private class ConfigLoader extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                URL url = new URL(String.format(Const.CONFIG_DOWNLOAD_URL_MASK, buildDeviceId()));

                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream in = urlConnection.getInputStream();
                InputStreamReader isw = new InputStreamReader(in);


                Main.getInstance().config.clear();

                String str;
                StringBuilder sb = new StringBuilder();
                int data = isw.read();
                while (data != -1) {
                    char current = (char) data;
                    sb.append(current);
                    data = isw.read();
                }

                str = sb.toString();
                String[] split = str.split("\n");
                for (String line : split) {
                    String[] keyVal = line.split(" = ");
                    if (keyVal.length == 2) {
                        String name = keyVal[0];
                        ConfigurationKey key = ConfigurationKey.valueOf(name);
                        String value = keyVal[1];
                        Main.getInstance().config.put(key, value);
                    }
                }


            } catch (Exception e) {
                System.exit(1); /// THIS IS FATAL ERROR!!! WE NEED INTERNET AND CONFIGURATION ALWAYS!!!
            } finally {
                try {
                    urlConnection.disconnect();
                } catch (Exception e) {
                }
            }


            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            startApplication();
        }

    }


}
