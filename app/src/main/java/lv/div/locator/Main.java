package lv.div.locator;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;

import java.text.SimpleDateFormat;
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

import de.greenrobot.event.EventBus;
import lv.div.locator.actions.HttpReportSender;
import lv.div.locator.actions.InitialConfigLoader;
import lv.div.locator.actions.MlsFenceRequestSender;
import lv.div.locator.commons.conf.ConfigurationKey;
import lv.div.locator.commons.conf.Const;
import lv.div.locator.conf.WifiScanResult;
import lv.div.locator.events.AccelerometerListener;
import lv.div.locator.events.EventMlsFenceRequest;
import lv.div.locator.events.MovementDetector;
import lv.div.locator.utils.FLogger;


public class Main extends AppCompatActivity {

    public static final int MAX_VALUE_FOR_RANDOM = 999999;
    public static BackgroundService mServiceInstance;
    public static Map<ConfigurationKey, String> config = new HashMap<>();

    public static LocationManager locationManager = null;
    public static Location currentBestLocation;
    public static String wifiCache = Const.EMPTY;
    public static String previousSafeZoneCall = Const.EMPTY;
    public static String previousMLSFenceCall = Const.EMPTY;
    public static Map<String, WifiScanResult> wifiNetworksCache = new HashMap();
    public static String mlsCache = Const.EMPTY;
    public static int safeZoneTimesCount = 0;
    public static Map<String, String> bssidNetworks = new HashMap<>();
    public static Date wifiCacheDate = new Date(0);
    public static Date mlsFenceCacheDate = new Date(0);
    public static Date mlsFenceRecheckDate = new Date(0);
    public static boolean mlsFenceRecheckBusy = false;
    public static Date wifiReportedDate = new Date(0);
    public static Date gpsReportedDate = new Date(0);
    public static Date deviceMotionTimeout = new Date(0);
    public static boolean deviceWasMoved = true; // default value to force Wifi scan
    public static int accelerometerValue = 0; // last movement value
    public static Date deviceMovedTime = new Date(Long.MAX_VALUE);
    public static boolean gpsLocationRequested = false;
    public static String gpsDataCache = Const.EMPTY;
    public static Date gpsLocationRequestTime = new Date(0);
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

        // Create MLS Fence checker:
        MlsFenceRequestSender mlsFenceRequestSender = new MlsFenceRequestSender();

        // Accelerometer initialization
        MovementDetector.getInstance().setListener(AccelerometerListener.getInstance());

        InitialConfigLoader configLoader = new InitialConfigLoader();
        configLoader.execute();

        FLogger.getInstance().log(this.getClass(), "onCreate completed.");

        // Adding RabbitMQ subscription...

    }

    @Override
    protected void onPause() {
        FLogger.getInstance().logAndFlush(this.getClass(), "onPause() called.");
        super.onPause();
        MovementDetector.getInstance().stop();
    }

    @Override
    protected void onStop() {
        FLogger.getInstance().logAndFlush(this.getClass(), "onStop() called!");
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FLogger.getInstance().logAndFlush(this.getClass(), "onResume() called");
        minimizeApp(); //  android:launchMode="singleTask" + this - means if the App will run again - will be minimized at once and no duplicate created.
    }

    @Override
    protected void onDestroy() {
        FLogger.getInstance().logAndFlush(this.getClass(), "onDestroy() called!!!");
        super.onDestroy();
    }

    public void startApplication() {
        mainApplicationService = new Intent(Main.this, BackgroundService.class);
        startService(mainApplicationService);
        MovementDetector.getInstance().start();
        minimizeApp();
    }

    public void exitApplication() {
        stopService(mainApplicationService);
        super.finish();
    }

    private synchronized void minimizeApp() {
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

    public synchronized String getBatteryStatus() {
        int level = mServiceInstance.batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        return String.valueOf(level);
    }

    public synchronized String getWifiNetworks() {
        FLogger.getInstance().log(this.getClass(), "getWifiNetworks() called");

        if (Main.getInstance().shuttingDown) {
            FLogger.getInstance().log(this.getClass(), "getWifiNetworks() - We're shutting down! Do not poll Wifi!");
            return wifiCache;
        }

        if (!Utils.clockTicked(wifiCacheDate, Integer.valueOf(config.get(ConfigurationKey.DEVICE_WIFI_REFRESH_MSEC)))) {
            FLogger.getInstance().log(this.getClass(), "getWifiNetworks(): returning wifiCache");
            return wifiCache;
        }

        // Just mark the CACHED(!) data due to device NOT moving:
        if (!Const.EMPTY.equals(Main.getInstance().previousSafeZoneCall)
                && !Main.getInstance().deviceWasMoved) {
            return "*" + wifiCache;
        }

        if (wifi.isWifiEnabled() == false) {
            wifi.setWifiEnabled(true);
        }

        wifi.startScan();
        // get list of the results in object format ( like an array )
        List<ScanResult> results = wifi.getScanResults();
        bssidNetworks.clear();
        wifiNetworksCache.clear();
        SortedMap<Integer, String> networks = new TreeMap<>(Collections.reverseOrder());
        for (ScanResult result : results) {
            WifiScanResult wifiScanResult = new WifiScanResult();
            wifiScanResult.setBssid(result.BSSID);
            wifiScanResult.setSsid(result.SSID);
            wifiScanResult.setLevel(result.level);
            wifiNetworksCache.put(result.BSSID, wifiScanResult);

            networks.put(result.level, result.SSID);
            bssidNetworks.put(result.BSSID, result.SSID);
        }

        Set<Map.Entry<Integer, String>> entries = networks.entrySet();
        Iterator<Map.Entry<Integer, String>> iterator = entries.iterator();
        StringBuffer sb = new StringBuffer();
        while (iterator.hasNext()) {
            Map.Entry<Integer, String> network = iterator.next();
            sb.append(Const.SPACE);
            sb.append(network.getValue());
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
    public synchronized String isInSafeZone() {
        //TODO: Add logic here! If there's no Starting/Ending WiFi ect.
        FLogger.getInstance().log(this.getClass(), "isInSafeZone() called");
        String result = Const.EMPTY;

        if (!Const.EMPTY.equals(previousSafeZoneCall) && !deviceWasMoved) {
            FLogger.getInstance().log(this.getClass(), "isInSafeZone(): deviceWasMoved=FALSE! Returning previousSafeZoneCall");
            return previousSafeZoneCall; // "*" means - NO movement
        }

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


        if (Utils.clockTicked(Main.getInstance().mlsFenceRecheckDate, 120000) && !Main.getInstance().mlsFenceRecheckBusy) { // need to recheck MLS fence safe zone area every 120 sec:

            EventMlsFenceRequest mlsFenceRequest = new EventMlsFenceRequest("EventMlsFenceRequest");
            EventBus.getDefault().post(mlsFenceRequest);

//            Main.getInstance().mlsFenceRecheckDate = new Date();
        }



//        if (Const.EMPTY.equals(result)) { // Zone is not safe, according to WiFi settings. Trying MLS fences:
//            if (!Const.EMPTY.equals(previousMLSFenceCall) && !Utils.clockTicked(Main.getInstance().mlsFenceCacheDate, 70000)) { // each 70 sec.
//                result = previousMLSFenceCall;
//            } else {
//
//                previousMLSFenceCall = result; // Refresh value
//            }
//
//
//        }


        FLogger.getInstance().log(this.getClass(), "isInSafeZone(): result = " + result);
        if (Const.EMPTY.equals(result)) {
            // Not a Safe Zone!
            FLogger.getInstance().log(this.getClass(), "isInSafeZone() Not a Safe Zone! accuSize=" + Main.getInstance().safeZoneTimesCount + " CLEARED!");
            Main.getInstance().safeZoneTimesCount = 0;// Cleanup Safe zone send(s) accumulator
            deviceWasMoved = true; // Reset motion variable. Out of safe zone, assumed moving. Enable rescan Wifi
        }

        previousSafeZoneCall = result; // Refresh value
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
                deviceId = String.valueOf(Math.round(Math.random() * MAX_VALUE_FOR_RANDOM));
            }

        }
        return deviceId;
    }

    public static String buildLogFileName() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");
        return "loc." + simpleDateFormat.format(new Date()) + ".txt";
    }


    public synchronized void forgetWifiNetworksButHome() {
        String homeNetworks = "www.div.lv,Jack_Daniels";
        String[] split = homeNetworks.split(",");

        List<WifiConfiguration> list = wifi.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            String ssid = i.SSID;
            int nid = i.networkId;

            boolean thisIsHomeNetwork = false;
            for (String homeNetwork : split) {
                if (ssid.indexOf(homeNetwork) >= 0) {
                    thisIsHomeNetwork = true;
                    break;
                }
            }

            if (!thisIsHomeNetwork) {
                FLogger.getInstance().log(this.getClass(), "Forget Wifi: "+ssid);
                wifi.removeNetwork(i.networkId);
                wifi.saveConfiguration();
            }

        }

    }


}
