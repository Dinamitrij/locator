package lv.div.locator;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;

import java.text.SimpleDateFormat;
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
import lv.div.locator.actions.InitialConfigLoader;
import lv.div.locator.commons.conf.ConfigurationKey;
import lv.div.locator.commons.conf.Const;
import lv.div.locator.conf.Constant;
import lv.div.locator.utils.FLogger;


public class Main extends AppCompatActivity {

    public static BackgroundService mServiceInstance;
    public static Map<ConfigurationKey, String> config = new HashMap<>();

    public static LocationManager locationManager = null;
    public static Location currentBestLocation;
    public static String wifiCache = Const.EMPTY;
    public static Set<String> wifiNetworksCache = new HashSet<>();
    public static List<Boolean> safeZoneFlags = new ArrayList<>();
    public static Map<String, String> bssidNetworks = new HashMap<>();
    public static Date wifiCacheDate = new Date(0);
    public static Date wifiReportedDate = new Date(0);
    public static Date gpsReportedDate = new Date(0);
    public static Date locationReportedDate = new Date(Long.MAX_VALUE);
    public static Main mInstance;
    public static boolean shuttingDown = false;
    public static boolean readyForPowerOff = false;
    private WifiManager wifi;
    private String deviceId;
    private Intent mainApplicationService;

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

        FLogger.getInstance().log(this.getClass(), "onCreate completed.");
    }

    @Override
    protected void onPause() {
        FLogger.getInstance().logAndFlush(this.getClass(), "onPause() called.");
        super.onPause();
    }

    @Override
    protected void onStop() {
        FLogger.getInstance().logAndFlush(this.getClass(), "onStop() called!");
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        FLogger.getInstance().logAndFlush(this.getClass(), "onDestroy() called!!!");
        super.onDestroy();
    }

    public void startApplication() {
        mainApplicationService = new Intent(Main.this, BackgroundService.class);
        startService(mainApplicationService);
        minimizeApp();
    }

    public void exitApplication() {
        stopService(mainApplicationService);
        super.finish();
    }

    private void minimizeApp() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    public void startup() {
        FLogger.getInstance().log(this.getClass(), "startup() called");
        Intent i = new Intent(this, BackgroundService.class);
        startService(i);
    }

    public String getBatteryStatus() {
        int level = mServiceInstance.batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        return String.valueOf(level);
    }

    public String getWifiNetworks() {
        FLogger.getInstance().log(this.getClass(), "getWifiNetworks() called");

        if (Main.getInstance().shuttingDown) {
            FLogger.getInstance().log(this.getClass(), "getWifiNetworks() - We're shutting down! Do not poll Wifi!");
            return wifiCache;
        }

        if (wifi.isWifiEnabled() == false) {
            wifi.setWifiEnabled(true);
        }

        if (!Utils.clockTicked(wifiCacheDate, Integer.valueOf(config.get(ConfigurationKey.DEVICE_WIFI_REFRESH_MSEC)))) {
            FLogger.getInstance().log(this.getClass(), "getWifiNetworks(): returning wifiCache");
            return wifiCache;
        }

        FLogger.getInstance().log(this.getClass(), "getWifiNetworks(): start Wifi Scan...");
        wifi.startScan();
        FLogger.getInstance().log(this.getClass(), "getWifiNetworks(): Wifi Scan completed");
        // get list of the results in object format ( like an array )
        List<ScanResult> results = wifi.getScanResults();
        bssidNetworks.clear();
        SortedMap<Integer, String> networks = new TreeMap<>(Collections.reverseOrder());
        for (ScanResult result : results) {
            networks.put(result.level, result.SSID);
            bssidNetworks.put(result.BSSID, result.SSID);
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
        FLogger.getInstance().log(this.getClass(), "getWifiNetworks(): Wifi Scan results: " + wifiCache);
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
        FLogger.getInstance().log(this.getClass(), "isInSafeZone() called");
        String result = Const.EMPTY;

        String currentVisibleWifiNetworks = getWifiNetworks();

        String safeWifis = config.get(ConfigurationKey.SAFE_ZONE_WIFI);
        String[] safeWifiPatternsWithNames = safeWifis.split(Const.WIFI_VALUES_SEPARATOR);
        for (String safeWifiPatternWithName : safeWifiPatternsWithNames) {

            String[] wifiValueAndAlias = safeWifiPatternWithName.split(Const.WIFI_NAME_SEPARATOR);
            if (currentVisibleWifiNetworks.matches(wifiValueAndAlias[0])) {
                FLogger.getInstance().log(this.getClass(), "isInSafeZone(): Matched Regexp: " + wifiValueAndAlias[0]);
                result = wifiValueAndAlias[1];
                break;
            }

        }
        FLogger.getInstance().log(this.getClass(), "isInSafeZone(): result = " + result);
        if (Const.EMPTY.equals(result)) {
            // Not a Safe Zone!
            FLogger.getInstance().log(this.getClass(), "isInSafeZone() Not a Safe Zone! accuSize=" + Main.getInstance().safeZoneFlags.size() + " CLEARED!");
            Main.getInstance().safeZoneFlags.clear(); // Cleanup Safe zone send(s) accumulator
        }


        return result;

    }


    @Override
    protected void onResume() {
        super.onResume();
        FLogger.getInstance().logAndFlush(this.getClass(), "onResume() called");
        minimizeApp(); //  android:launchMode="singleTask" + this - means if the App will run again - will be minimized at once and no duplicate created.
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

    public static String buildLogFileName() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");
        return "loc." + simpleDateFormat.format(new Date()) + ".txt";
    }


}
