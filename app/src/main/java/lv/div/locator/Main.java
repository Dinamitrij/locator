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
import java.util.HashSet;
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
    public static boolean locationRequested = false; // Initially - no location request
    public static DeviceLocationListener deviceLocationListener = new DeviceLocationListener();
    public static Location currentBestLocation;
    public static String wifiCache = "";
    public static Set<String> wifiNetworksCache = new HashSet<>();

    public static Date wifiCacheDate = new Date(0);
    public static Main mInstance;
    public Date healthCheckTime = new Date(0);
    private WifiManager wifi;
    private List<String> safeWifi = new ArrayList<>();
    private String deviceId;
    private HttpURLConnection urlConnection;

    public static Main getInstance() {
        return mInstance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInstance = this;
        mServiceInstance = null;

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        safeWifi.add("www.div.lv");

        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        // Create HTTP report sender:
        HttpReportSender httpReportSender = new HttpReportSender();

        ConfigLoader configLoader = new ConfigLoader();
        configLoader.execute();

    }

    public void startApplication() {
        Intent i = new Intent(Main.this, BackgroundService.class);
        startService(i);
//        finish();
        minimizeApp();
    }

    private void minimizeApp() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
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

        if (!Utils.clockTicked(wifiCacheDate, Integer.valueOf(config.get(ConfigurationKey.DEVICE_WIFI_REFRESH_MSEC)))) {
            return wifiCache;
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
        wifiNetworksCache.clear();
        while (iterator.hasNext()) {
            Map.Entry<Integer, String> network = iterator.next();
            sb.append(network.getValue());
            wifiNetworksCache.add(network.getValue());
            sb.append(Const.SPACE);
            sb.append(network.getKey());
            sb.append("; ");
        }

        wifiCache = sb.toString();
        wifiCacheDate = new Date();
        return sb.toString();
    }

    /**
     * Is device in safe zone?
     * Safe zone = zone within particular WiFi network range
     *
     * @return
     */
    public boolean isInSafeZone() {
        //TODO: Add logic here! If there's no Starting/Ending WiFi ect.


//        Date date = new Date();   // given date
//        Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
//        calendar.setTime(date);   // assigns calendar to given date
//        calendar.get(Calendar.HOUR_OF_DAY); // gets hour in 24h format
//        calendar.get(Calendar.HOUR);        // gets hour in 12h format
//        calendar.get(Calendar.MONTH);       // gets month number, NOTE this is zero based!
//
//        int i = calendar.get(Calendar.MINUTE);
//        boolean insafezone = (i % 2) == 0;
//        return insafezone;


        boolean result = false;


        String refreshedWifiData = getWifiNetworks();

        String safeWifis = config.get(ConfigurationKey.SAFE_ZONE_WIFI);
        String[] split = safeWifis.split(Const.WIFI_VALUES_SEPARATOR);

        for (String safeNetwork : wifiNetworksCache) {
            if (safeWifis.indexOf(safeNetwork) >= 0) {
                result = true;
                break;
            }

//            String safeNetworkName = safeNetwork;
//            final String[] netName = safeNetworkName.split(Const.WIFI_NAME_SEPARATOR);
//            if (netName.length > 1) {
//                safeNetworkName = netName[0];
//            }
//            if (refreshedWifiData.indexOf(safeNetworkName) >= 0) {
//                return true;
//            }
        }


        return result;

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
