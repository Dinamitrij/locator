package lv.div.locator;


import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
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
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
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


public class BackgroundService extends Service {

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






    private boolean clockTicked = false;
    private LocationManager gpsLocationManager;

    private boolean requested = false;
    private String deviceId;
    private DeviceLocationListener deviceLocationListener;
    //    private final Criteria crit;


    public BackgroundService() {
        // TODO Auto-generated constructor stub

        Log.d(Tag, "Constructor");

        // For the following we should send an SMS:
        eventsForSMS.add(EventType.BATTERY_LOW);
        eventsForSMS.add(EventType.CANNOT_CONNECT_TO_INTERNET);
        eventsForSMS.add(EventType.WIFI_ENTER);
        eventsForSMS.add(EventType.WIFI_LEAVE);
        eventsForSMS.add(EventType.LOCATION);



        Main.mServiceInstance = this;



//        crit = new Criteria();
//        crit.setAccuracy(Criteria.ACCURACY_FINE);

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        super.onStartCommand(intent, flags, startId);
        if (null == mLocationManager) {
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }



        startGPS();

        Main.getInstance().healthCheck(); // Send Healthcheck message, if needed.

        return START_NOT_STICKY;

    }

    @Override
    public void onDestroy() {
        Log.d(Tag, "onDestroy()");
        super.onDestroy();
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
        return Utils.clockTicked(event.getEventTime(), event.getEventTTLMsec());
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




    public int getSmsSendingDelay() {
        return 2000;
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
        mLocationManager.removeUpdates(deviceLocationListener);
    }


    public void startGPS() {
        AlarmManager mgr = null;
        Intent i = null;
        GregorianCalendar cal = null;
        int iProviders = 0;


//        LocationManager mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        String bestProvider = mlocManager.getBestProvider(crit, false);
//        if (mLocationManager.isProviderEnabled(bestProvider)) {
//            mLocationManager.requestLocationUpdates(bestProvider, 0, 0, this);
//            iProviders++;
//        }

        deviceLocationListener = new DeviceLocationListener();
        // Make sure at least one provider is available
        boolean networkProviderEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (networkProviderEnabled) {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, deviceLocationListener);
            iProviders++;
        }

        boolean gpsProviderEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (gpsProviderEnabled) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, deviceLocationListener);
            iProviders++;
        }

        if (iProviders == 0) {
            sleep();
            return;
        }

        lGPSTimestamp = System.currentTimeMillis();
        mgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        cal = new GregorianCalendar();
        i = new Intent(this, TimeoutReceiver.class);
        this.pi = PendingIntent.getBroadcast(this, 0, i, 0);
        cal.add(Calendar.SECOND, getMainDelay());
        mgr.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), this.pi);


        // Starting process in foreground:
        Notification note = new Notification.Builder(this).setContentTitle("Locator is on")
                .setContentIntent(pi)
                .setContentText("ContentText")
                .build();

        note.flags|=Notification.FLAG_NO_CLEAR;
        startForeground(8080, note);


    }






}


