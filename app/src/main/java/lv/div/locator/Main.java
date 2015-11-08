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
import android.widget.Toast;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
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
import lv.div.locator.actions.InitialConfigLoader;
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
    private String deviceId;
//    private HttpURLConnection urlConnection;

    public static Main getInstance() {
        return mInstance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInstance = this;
        mServiceInstance = null;

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        // Create HTTP report sender:
        HttpReportSender httpReportSender = new HttpReportSender();

        InitialConfigLoader configLoader = new InitialConfigLoader();
        configLoader.execute();
    }

    public void startApplication() {
        Intent i = new Intent(Main.this, BackgroundService.class);
        startService(i);
        minimizeApp();
    }

    private void minimizeApp() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    public void startup() {
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
            String s = config.get(ConfigurationKey.DEVICE_ALIAS);
            time = URLEncoder.encode("healthCheck from " + s + Const.SPACE + now.toString(), Const.UTF8_ENCODING);
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
            sb.append(Const.SPACE);
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
    public String isInSafeZone() {
        //TODO: Add logic here! If there's no Starting/Ending WiFi ect.

        String result = Const.EMPTY;

        String currentVisibleWifiNetworks = getWifiNetworks();

        String safeWifis = config.get(ConfigurationKey.SAFE_ZONE_WIFI);
        String[] safeWifiPatternsWithNames = safeWifis.split(Const.WIFI_VALUES_SEPARATOR);
        for (String safeWifiPatternWithName : safeWifiPatternsWithNames) {

            String[] wifiValueAndAlias = safeWifiPatternWithName.split(Const.WIFI_NAME_SEPARATOR);
            if (currentVisibleWifiNetworks.matches(wifiValueAndAlias[0])) {
                result = wifiValueAndAlias[1];
                break;
            }

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

}
