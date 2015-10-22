package lv.div.locator;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import lv.div.locator.actions.HttpReportSender;
import lv.div.locator.actions.NetworkReport;

public class Main extends AppCompatActivity {
//public class Main extends Application {

    public static BackgroundService mServiceInstance;
    protected static Main mInstance;

    public static Main getInstance() {
        return mInstance;
    }

    private Intent batteryStatus;
    private WifiManager wifi;
    private IntentFilter ifilter;
    private List<String> safeWifi = new ArrayList<>();
    private String deviceId;
    public Date healthCheckTime = new Date(0);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInstance = this;
        mServiceInstance = null;


        safeWifi.add("www.div.lv");
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryStatus = this.registerReceiver(null, ifilter);

        // Create HTTP report sender:
        HttpReportSender httpReportSender = new HttpReportSender();


        Intent i = new Intent(Main.this, BackgroundService.class);
        startService(i);
        finish();
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

    public void startup() {
//        wakeLock1(true);
        Intent i = new Intent(this, BackgroundService.class);
        startService(i);
    }

    public String getBatteryStatus() {
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        return String.valueOf(level);
    }


    public void healthCheck() {
        if (!Utils.clockTicked(healthCheckTime, Const.HEALTH_CHECK_TIME_MSEC)) {
            return;
        }
        Date now = new Date();
        String time = null;
        try {
            time = URLEncoder.encode("healthCheck from "+buildDeviceId()+" " + now.toString(), Const.UTF8_ENCODING);
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
            sb.append(" ");
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
     * Using IMEI as device ID
     */
    public String buildDeviceId() {

        if (null == deviceId) {
            deviceId = "rrrr";

//            try {
//                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//                deviceId = telephonyManager.getDeviceId();
//                if (deviceId == null) {
//                    deviceId = String.valueOf(Math.round(Math.random() * 999999));
//                }
//            } catch (Exception e) {
//                deviceId = String.valueOf(Math.round(Math.random() * 999999));
//            }
        }
        return deviceId;
    }

}
