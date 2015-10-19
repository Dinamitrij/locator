package lv.div.locator;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.telephony.SmsManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import de.greenrobot.event.EventBus;
import lv.div.locator.events.EventHttpReport;
import lv.div.locator.events.EventType;


public class BackgroundService extends Service implements LocationListener {

    private final static String Tag = "---IntentServicetest";
    public static final int MAIN_DELAY = 10;

    private boolean busy = false;
    private boolean generateEvent = true;
    private PendingIntent pi;
    private long lGPSTimestamp;

    private LocationManager mLocationManager = null;

    private Date lastProblematicMoment = new Date(0);
    private Map<EventType, SMSEvent> events = new HashMap();
    private Set<EventType> eventsForSMS = new HashSet<>();
    private long smsSendingDelay;
    private String wifiCache = "";
    private Date wifiCacheDate = new Date(0);

    private int globalIterations = 0;
    private WifiManager wifi;
    private Intent batteryStatus;
    private List<String> safeWifi = new ArrayList<>();


    private final IntentFilter ifilter;
    private boolean clockTicked = false;
    private LocationManager gpsLocationManager;

    private boolean requested = false;


    public BackgroundService() {
        // TODO Auto-generated constructor stub

        Log.d(Tag, "Constructor");

        // For the following we should send an SMS:
        eventsForSMS.add(EventType.BATTERY_LOW);
        eventsForSMS.add(EventType.CANNOT_CONNECT_TO_INTERNET);
        eventsForSMS.add(EventType.WIFI_ENTER);
        eventsForSMS.add(EventType.WIFI_LEAVE);
        eventsForSMS.add(EventType.LOCATION);

        safeWifi.add("www.div.lv");

        Main.mServiceInstance = this;

        ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);


    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (null == mLocationManager) {
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }

        batteryStatus = this.registerReceiver(null, ifilter);

        startGPS();

        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public void onDestroy() {
        Log.d(Tag, "onDestroy()");
        super.onDestroy();
    }


    /**
     * Is device in safe zone?
     * Safe zone = zone within particular WiFi network range
     *
     * @return
     */
    private boolean isInSafeZone() {
        //TODO: Add logic here! If there's no Starting/Ending WiFi ect.

        String wifiNetworks = getWifiNetworks();

        for (String wifiNet : safeWifi) {
            if (wifiNetworks.indexOf(wifiNet) >= 0) {
                return true;
            }

        }

        return false;
    }


    private boolean clockTicked(Date fromDate, Integer tickMsec) {
        Date now = new Date();
        if (now.getTime() < fromDate.getTime() + tickMsec) {
            return false;
            //This event/state is still alive. Do not overwrite it.
        } else {
            return true;
        }
    }


    private void pollWifiNetworksAndPrepareSMS() {


        SMSEvent oldWifiEvent = events.get(EventType.WIFI_ENTER);
        if (null != oldWifiEvent && !messageOutdated(oldWifiEvent)) {
            return;
        }


        String wifiNetworks = getWifiNetworks();


        SMSEvent wifiEvent = new SMSEvent();
        if (wifiNetworks.length() > 0) {
            wifiEvent.setAlertMessage(wifiNetworks);
            wifiEvent.setEventTime(new Date());
            wifiEvent.setProblemType(EventType.WIFI_ENTER);
            wifiEvent.setEventTTLMsec(1000 * 60 * 5);
            ArrayList<String> phonesToAlert = new ArrayList<>();
            phonesToAlert.add(Const.PHONE1);
            wifiEvent.setPhonesToAlert(phonesToAlert);
        } else {
            wifiEvent.setAlertMessage("Empty WiFi poll results");
            wifiEvent.setEventTime(new Date());
            wifiEvent.setEventTTLMsec(1000 * 60 * 5);
            wifiEvent.setProblemType(EventType.WIFI_ENTER);
            ArrayList<String> phonesToAlert = new ArrayList<>();
            phonesToAlert.add(Const.PHONE1);
            wifiEvent.setPhonesToAlert(phonesToAlert);
        }

        events.put(EventType.WIFI_ENTER, wifiEvent);
    }

    private String getWifiNetworks() {

        if (!clockTicked(wifiCacheDate, getMainDelay())) { //Prevent to poll WiFi too often
            return wifiCache;
        }


        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
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

        wifiCache = sb.toString();
        wifiCacheDate = new Date();
        return wifiCache;
    }

    private void sendSMSIfNeeded() {
        SmsManager smsManager = SmsManager.getDefault();

        Iterator<EventType> iterator = events.keySet().iterator();
        while (iterator.hasNext()) {

            EventType key = iterator.next();
            SMSEvent smsEvent = events.get(key);
            boolean smsSent = smsEvent.isSmsSent();
            if (smsSent) {
                continue;
            }


            if (eventsForSMS.contains(smsEvent.getProblemType())) {

//            Date now = new Date();
//            Date eventTime = smsEvent.getEventTime();

//                        (now.getTime() - eventTime.getTime())>=smsEvent.getSmsRepeatDelayMsec()

                try {
                    List<String> phonesToAlert = smsEvent.getPhonesToAlert();
                    for (String phone : phonesToAlert) {

//                        smsManager.sendTextMessage(phone, null, smsEvent.getProblemType() + " " + smsEvent.getEventTime(), null, null);
//                        smsManager.sendTextMessage(phone, null, smsEvent.getProblemType() + " " + smsEvent.getAlertMessage(), null, null);
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Const.TIME_24H_FORMAT);
                        String time = simpleDateFormat.format(new Date());

                        String alertMessage = smsEvent.getAlertMessage();
                        if (alertMessage.length() > Const.MAX_MESSAGE_SIZE) {
                            alertMessage = alertMessage.substring(0, Const.MAX_MESSAGE_SIZE);
                        }
//                        smsManager.sendTextMessage(phone, null, globalIterations + " " + time + " " + alertMessage, null, null);

                        SystemClock.sleep(getSmsSendingDelay());
                    }
                    smsEvent.setSmsSent(true);
//                    iterator.remove();

                    boolean outdated = messageOutdated(smsEvent);
                    if (outdated) {
                        iterator.remove();
//                        events.remove(key);
                    }


                } catch (Exception e) {
                    // be quiet
                }
            }


        }


    }

    private boolean messageOutdated(SMSEvent event) {
        return clockTicked(event.getEventTime(), event.getEventTTLMsec());
    }

    private boolean shouldWeStop() {
        // TODO: Add logic here, if applicable

        //return (globalIterations++) > 2000;
        return false;

    }

    /**
     * Getting system delay after next "round"
     *
     * @return
     */
    private int getMainDelay() {
        return MAIN_DELAY;
    } // once per 1 sec.

    private String getBatteryStatus() {
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        return level + "%";
    }


    public int getSmsSendingDelay() {
        return 2000;
    }


    /**
     * Checks if a string is null or empty
     *
     * @param text
     * @return
     */
    public static boolean isNullOrEmpty(String text) {
        return text == null || text.length() == 0;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public void sleep() {

        AlarmManager mgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent i = new Intent(this, MainReceiver.class);
        Calendar cal = new GregorianCalendar();

        cal.add(Calendar.SECOND, 10);

        this.pi = PendingIntent.getBroadcast(this, 0, i, 0);
        mgr.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), this.pi);
    }


    public void gpsTimeout() {

        stopGPS();
//        saveLocation();
        sleep();
//        MainApplication.wakeLock2(false);
    }


    public void stopGPS() {
        lGPSTimestamp = 0;
        if (this.pi != null) {
            AlarmManager mgr = (AlarmManager) getSystemService(ALARM_SERVICE);
            mgr.cancel(this.pi);
            this.pi = null;
        }
        mLocationManager.removeUpdates(this);
    }


    public void startGPS() {
        AlarmManager mgr = null;
        Intent i = null;
        GregorianCalendar cal = null;
        int iProviders = 0;

//        gpsLocationListener = new GeneralLocationListener(this, "GPS");

        // Make sure at least one provider is available
        if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 0, this);
            iProviders++;
        }

        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, this);
            iProviders++;
        }

        if (iProviders == 0) {
            sleep();
//            Main.wakeLock2(false);
            return;
        }

        lGPSTimestamp = System.currentTimeMillis();
        mgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        cal = new GregorianCalendar();
        i = new Intent(this, TimeoutReceiver.class);
        this.pi = PendingIntent.getBroadcast(this, 0, i, 0);
        cal.add(Calendar.SECOND, getMainDelay());
        mgr.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), this.pi);

    }

    @Override
    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
//        String coordinates = String.valueOf(latitude) + ", " + String.valueOf(longitude);

        String accuracy = String.format("%.0f", location.getAccuracy());

        EventHttpReport eventHttpReport = new EventHttpReport(getBatteryStatus(), getWifiNetworks(), String.valueOf(latitude), String.valueOf(longitude), accuracy, String.valueOf(isInSafeZone()));
        EventBus.getDefault().post(eventHttpReport);

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}


