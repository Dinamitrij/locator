package lv.div.locator;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.telephony.SmsManager;
import android.util.Log;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
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

import de.greenrobot.event.EventBus;
import lv.div.locator.events.EventHttpReport;
import lv.div.locator.events.EventType;


public class BackgroundService extends IntentService {


    private static final String MESSAGE_IN = "message_input";
    private static final String MESSAGE_OUT = "message_output";
    private final static String Tag = "---IntentServicetest";

    private boolean busy = false;
    private boolean generateEvent = true;

    private Date lastProblematicMoment = new Date(0);
    private Map<EventType, SMSEvent> events = new HashMap();

    private Set<EventType> eventsForSMS = new HashSet<>();
    private long smsSendingDelay;
    private String gps = "";
    private int globalIterations = 0;
    private WifiManager wifi;
    List<ScanResult> results;
    private Intent batteryStatus;
    private final IntentFilter ifilter;
//    private LocationManager mlocManager;
//    private String bestProvider;


    public BackgroundService() {
        // TODO Auto-generated constructor stub
        super("BackgroundService");
        Log.d(Tag, "Constructor");

        // For the following we should send an SMS:
        eventsForSMS.add(EventType.BATTERY_LOW);
        eventsForSMS.add(EventType.CANNOT_CONNECT_TO_INTERNET);
        eventsForSMS.add(EventType.WIFI_ENTER);
        eventsForSMS.add(EventType.WIFI_LEAVE);
        eventsForSMS.add(EventType.LOCATION);

        ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

//        Criteria crit = new Criteria();
//        crit.setAccuracy(Criteria.ACCURACY_FINE);
//        LocationManager mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        String bestProvider = mlocManager.getBestProvider(crit, false);


//        Context applicationContext = getApplicationContext();


//        SMSEvent smsEvent = new SMSEvent();
//        smsEvent.setAlertMessage("Alert from SMS sender!");
//        smsEvent.setEventTime(new Date());
//        smsEvent.setSmsRepeatDelayMsec(20000);
//        smsEvent.setProblemType(EventType.BATTERY_LOW);
//
//        ArrayList<String> phonesToAlert = new ArrayList<>();
//        phonesToAlert.add("11111111");
//        smsEvent.setPhonesToAlert(phonesToAlert);
//
//        events.add(smsEvent);
//
//        SMSEvent smsEvent2 = new SMSEvent();
//        smsEvent2.setAlertMessage("Alert 222 from SMS sender!");
//        smsEvent2.setEventTime(new Date());
//        smsEvent2.setSmsRepeatDelayMsec(20000);
//        smsEvent2.setProblemType(EventType.WIFI_ENTER);
//
//        ArrayList<String> phonesToAlert2 = new ArrayList<>();
//        phonesToAlert2.add("2222222");
//        smsEvent2.setPhonesToAlert(phonesToAlert2);
//
//        events.add(smsEvent2);


    }

    @Override
    public void onDestroy() {
        Log.d(Tag, "onDestroy()");
        super.onDestroy();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.d(Tag, "onStart()");
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Tag, "onStartCommand()");
        return super.onStartCommand(intent, flags, startId);
//        return START_STICKY;
    }

    @Override
    public void setIntentRedelivery(boolean enabled) {
        Log.d(Tag, "setIntentRedelivery()");
        super.setIntentRedelivery(enabled);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        batteryStatus = this.registerReceiver(null, ifilter);

        Date startCycleTime = new Date(0); // Very old date. We should run cycle!
        int mainDelay = getMainDelay();

        // TODO Auto-generated method stub
        Log.d(Tag, "IntentServicetest is onHandleIntent!");


//        if (generateEvent) {
//            generateEvent = false;


//        }

//        synchronized (this) {
//
//
//            if (!busy) {
//                busy = true;

        while (!shouldWeStop()) {
            SystemClock.sleep(getMainDelay());

            boolean clockTicked = clockTicked(startCycleTime, mainDelay);
            if (!clockTicked) {
                continue;
            }


//            pollWifiNetworksAndPrepareSMS();


//            pollGPSlocationAndPrepareSMS();


//                    LocationManager mlocManager=null;
//                    LocationListener mlocListener;
//                    mlocManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
//                    mlocListener = new MyLocationListener();
//                    mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 3, mlocListener);
//                    mlocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 3, mlocListener);

//                    if (mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//                        if(MyLocationListener.latitude>0)
//                        {
//                            et_field_name.append("Latitude:- " + MyLocationListener.latitude + '\n');
//                            et_field_name.append("Longitude:- " + MyLocationListener.longitude + '\n');
//                        }
//                        else
//                        {
//                            alert.setTitle("Wait");
//                            alert.setMessage("GPS in progress, please wait.");
//                            alert.setPositiveButton("OK", null);
//                            alert.show();
//                        }
//                    } else {
//                        et_field_name.setText("GPS is not turned on...");
//                    }


//                    LocationManager locationManager = (LocationManager)
//                            getSystemService(Context.LOCATION_SERVICE);
//                    LocationListener locationListener = new MyLocationListener();
//
//
//                    locationManager.requestLocationUpdates(
//                            LocationManager.GPS_PROVIDER, getGPSupdateTime(), getGPSDistanceUpdateMeters(), locationListener);


//            sendSMSIfNeeded();
            sendHTTPMessageIfNeeded();


            // Round is completed. Start "delay" for the next one.
            startCycleTime = new Date();

        }

//                busy = false;
//            System.exit(0);

//            }
//
//
//        }

    }

    private void pollGPSlocationAndPrepareSMS() {
//        Criteria crit = new Criteria();
//        crit.setAccuracy(Criteria.ACCURACY_FINE);
//        LocationManager mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        String bestProvider = mlocManager.getBestProvider(crit, false);


        Criteria crit = new Criteria();
        crit.setAccuracy(Criteria.ACCURACY_FINE);
        LocationManager mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        String bestProvider = mlocManager.getBestProvider(crit, false);

        DeviceLocationListener gpsListener = DeviceLocationListener.getInstance();
        LocationListener mlocListener = DeviceLocationListener.getInstance();


        Location lastKnownLocation = mlocManager.getLastKnownLocation(bestProvider);
        String locationString = "No GPS data yet...";

        if (null != lastKnownLocation) {
            double latitude = lastKnownLocation.getLatitude();
            double longitude = lastKnownLocation.getLongitude();
            locationString = String.valueOf(latitude) + ", " + String.valueOf(longitude);
        }

//        mlocManager.requestLocationUpdates(bestProvider, 0, 0, mlocListener);
        mlocManager.requestLocationUpdates(bestProvider, 0, 0, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

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
        });


        SMSEvent smsEvent = new SMSEvent();
        smsEvent.setAlertMessage(locationString);
        smsEvent.setEventTime(new Date());
//            smsEvent.setSmsRepeatDelayMsec(20000);
        smsEvent.setProblemType(EventType.LOCATION);
        ArrayList<String> phonesToAlert = new ArrayList<>();
        phonesToAlert.add(Const.PHONE1);
        smsEvent.setPhonesToAlert(phonesToAlert);

        events.put(EventType.LOCATION, smsEvent);


//        boolean searchInProgress = gpsListener.isSearchInProgress();
//        if (!searchInProgress) {
//            gpsListener.setSearchInProgress(true);
//            mlocManager.requestLocationUpdates(bestProvider, 0, 0, mlocListener);
//        }

//                    LocationManager best = mgr.getBestProvider(crit, false);


    }


    private boolean messageOutdated(SMSEvent event) {
        return clockTicked(event.getEventTime(), event.getEventTTLMsec());
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
        return sb.toString();
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
                    int aaa = 2;
                }
            }


//                    if (!smsEvent.isSmsSent()) {
//                        try {
//                            smsManager.sendTextMessage("1111111", null, "Privet from LG!", null, null);
//                        } catch (Exception e) {
//                            smsEvent
//                        }
//                    }


        }


    }


    private void sendHTTPMessageIfNeeded() {






        URL url;
        HttpURLConnection urlConnection = null;
        try {
//            String batteryStatus = URLEncoder.encode(getBatteryStatus(), Const.UTF8_ENCODING);
//            String wifiData = URLEncoder.encode(getWifiNetworks(), Const.UTF8_ENCODING);


//            SMSEvent smsEvent = events.get(EventType.LOCATION);

            DeviceLocationListener gpsLocator = DeviceLocationListener.getInstance();

//            String gpsData = URLEncoder.encode(gpsLocator.getLastGpsStatus(), Const.UTF8_ENCODING);
//            String gpsData = URLEncoder.encode(smsEvent.getAlertMessage(), Const.UTF8_ENCODING);
//            gpsData = gpsData + URLEncoder.encode(smsEvent.getAlertMessage(), Const.UTF8_ENCODING);


            EventHttpReport eventHttpReport = new EventHttpReport(getBatteryStatus(), getWifiNetworks(), gpsLocator.getLastGpsStatus());
            EventBus.getDefault().post(eventHttpReport);
            int a=1;

/*





//            url = new URL("http://api.thingspeak.com/update?key=SGASYB87X8BPLHIY&field1=" + batteryStatus + "&field2=" + wifiData + "&field3=" + gpsData);
            String urlAddress = String.format(Const.REPORT_URL_MASK, batteryStatus, wifiData, gpsData);
            url = new URL(urlAddress);
//
            urlConnection = (HttpURLConnection) url
                    .openConnection();
            InputStream in = urlConnection.getInputStream();
            InputStreamReader isw = new InputStreamReader(in);

            int data = isw.read();
            while (data != -1) {
                char current = (char) data;
                data = isw.read();
            }
            int aaaa = 0;

            */
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                urlConnection.disconnect();
            } catch (Exception e) {
                e.printStackTrace(); //If you want further info on failure...
            }
        }





    }

    private int getGPSDistanceUpdateMeters() {
        return 5;
    }

    private int getGPSupdateTime() {
        return 5000;
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
        return 1000;
    } // once per 1 sec.

    private String getBatteryStatus() {
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);

        // Are we charging / charged?
//        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
//        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
//                status == BatteryManager.BATTERY_STATUS_FULL;
//        String charge = "[-]";
//        if (isCharging) {
//            charge = "[+]";
//        }
//        return level + "% " + charge;
        return level + "%";
    }


    public int getSmsSendingDelay() {
        return 2000;
    }


/*
    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            gps = loc.getLongitude() + " " + loc.getLatitude();
//            String longitude = "Longitude: " + loc.getLongitude();
//            String latitude = "Latitude: " + loc.getLatitude();


            SMSEvent smsEvent = new SMSEvent();
            smsEvent.setAlertMessage(gps);
            smsEvent.setEventTime(new Date());
//            smsEvent.setSmsRepeatDelayMsec(20000);
            smsEvent.setProblemType(EventType.LOCATION);
            ArrayList<String> phonesToAlert = new ArrayList<>();
            phonesToAlert.add(PHONE1);
            smsEvent.setPhonesToAlert(phonesToAlert);

            events.put(EventType.LOCATION, smsEvent);
        }

        @Override
        public void onProviderDisabled(String provider) {
            int a = 1;
        }

        @Override
        public void onProviderEnabled(String provider) {
            int a = 1;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            int a = 1;
        }
    }
*/


}


