package lv.div.locator;


import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.telephony.SmsManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lv.div.locator.events.EventType;


public class BackgroundService extends Service {

    private final static String Tag = "---IntentServicetest";
    public static final int MAIN_DELAY = 10;
    private PendingIntent pi;

//    private LocationManager locationManager = null;

    private Date lastProblematicMoment = new Date(0);
    private Map<EventType, SMSEvent> events = new HashMap();
    private Set<EventType> eventsForSMS = new HashSet<>();
    private long smsSendingDelay;
    public Intent batteryStatus;
    private final IntentFilter ifilter;

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
        ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);


    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        super.onStartCommand(intent, flags, startId);
//        if (null == locationManager) {
//        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        }

        batteryStatus = this.registerReceiver(null, ifilter);

        startGPS();

        Main.getInstance().healthCheck(); // Send Healthcheck message, if needed.
        Main.getInstance().sendAlert("Locator started");
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
        if (this.pi != null) {
            AlarmManager mgr = (AlarmManager) getSystemService(ALARM_SERVICE);
            mgr.cancel(this.pi);
            this.pi = null;
        }
        //locationManager.removeUpdates(deviceLocationListener);
    }


    public void startGPS() {
        AlarmManager mgr = null;
        Intent i = null;
        GregorianCalendar cal = null;
        int iProviders = 0;


//        LocationManager mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        String bestProvider = mlocManager.getBestProvider(crit, false);
//        if (locationManager.isProviderEnabled(bestProvider)) {
//            locationManager.requestLocationUpdates(bestProvider, 0, 0, this);
//            iProviders++;
//        }

//        if (null != deviceLocationListener) {
//            Main.getInstance().locationManager.removeUpdates(deviceLocationListener);
//        }
//        deviceLocationListener = new DeviceLocationListener();

        // Make sure at least one provider is available
        boolean networkProviderEnabled = Main.getInstance().locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (networkProviderEnabled) {
            Main.getInstance().locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, Main.getInstance().deviceLocationListener);
            iProviders++;
        }

        boolean gpsProviderEnabled = Main.getInstance().locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (gpsProviderEnabled) {
            Main.getInstance().locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, Main.getInstance().deviceLocationListener);
            iProviders++;
        }

        if (iProviders == 0) {
            sleep();
            return;
        }

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

        note.flags |= Notification.FLAG_NO_CLEAR;
        startForeground(8080, note);


    }


}


