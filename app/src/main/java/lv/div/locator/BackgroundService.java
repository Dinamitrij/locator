package lv.div.locator;


import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.greenrobot.event.EventBus;
import lv.div.locator.actions.HealthCheckReport;
import lv.div.locator.actions.NetworkReport;
import lv.div.locator.conf.ConfigurationKey;
import lv.div.locator.events.EventHttpReport;
import lv.div.locator.events.EventType;


public class BackgroundService extends Service implements LocationListener {

    public static final int MAIN_DELAY = 10;
    public static final String DEFAULT_STATE = "n/a";
    private final static String Tag = "---IntentServicetest";
    private final IntentFilter ifilter;
    public Intent batteryStatus;
    private LocationManager mLocationManager = null;
    private PendingIntent pi;
    private Date pingTime = new Date(0);
    private Map<EventType, SMSEvent> events = new HashMap();
    private Set<EventType> eventsForSMS = new HashSet<>();
    private long smsSendingDelay;
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


        if (null == mLocationManager) {
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }

        batteryStatus = this.registerReceiver(null, ifilter);

        startGPS();

//        Main.getInstance().healthCheck(); // Send Healthcheck message, if needed.

        if (Main.getInstance().wifiNetworksCache.isEmpty()) {
            Main.getInstance().getWifiNetworks();
        }


//        Main.getInstance().sendAlert("Locator started");
        return START_STICKY;

    }

    @Override
    public void onDestroy() {
        Log.d(Tag, "onDestroy()");
        super.onDestroy();
    }


    /**
     * Getting system delay after next "round"
     *
     * @return
     */
    private int getMainDelay() {
        return MAIN_DELAY;
    } // once per 10 sec.


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public void sleep() {

        AlarmManager mgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent i = new Intent(this, MainReceiver.class);
        Calendar cal = new GregorianCalendar();

        cal.add(Calendar.SECOND, getMainDelay());

        this.pi = PendingIntent.getBroadcast(this, 0, i, 0);
        mgr.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), this.pi);
    }


    public void gpsTimeout() {

        stopGPS();

        ping(); // Send healthcheck alert if needed

        shutdownAppIfNeeded();

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



/*
        boolean inSafeZone = Main.getInstance().isInSafeZone();
        if (inSafeZone)     {

            if (null != deviceLocationListener && null != deviceLocationListener) {
                locationManager.removeUpdates(deviceLocationListener);
//                Main.getInstance().locationManager.removeUpdates(deviceLocationListener);
            }

        }
*/


//        boolean inSafeZone = Main.getInstance().isInSafeZone();
        String safeZoneName = Main.getInstance().isInSafeZone();
        if (!Const.EMPTY.equals(safeZoneName) && null != mLocationManager) {

            mLocationManager.removeUpdates(this);

        }


//        if (Main.getInstance().isInSafeZone()) {
//            locationManager.removeUpdates(deviceLocationListener);
//        }

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



/*        boolean inSafeZone = Main.getInstance().isInSafeZone();
        if (!inSafeZone)     { // Only if we're out of safe zone:

            deviceLocationListener = new DeviceLocationListener();

            // Make sure at least one provider is available
            boolean networkProviderEnabled = Main.getInstance().locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (networkProviderEnabled) {
//                Main.getInstance().locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, Main.getInstance().deviceLocationListener);
                Main.getInstance().locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, deviceLocationListener);
                iProviders++;
            }


            boolean gpsProviderEnabled = Main.getInstance().locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (gpsProviderEnabled) {
//                Main.getInstance().locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, Main.getInstance().deviceLocationListener);
                Main.getInstance().locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, deviceLocationListener);
                iProviders++;
            }
//        } else {
//            // Report safe zone:
//            reportSafeZone();
//        }

            if (iProviders == 0) {
                sleep();
                return;
            }


        } else {
            reportSafeZone();
        }*/


        String wifiZoneName = Main.getInstance().isInSafeZone();
        if (Const.EMPTY.equals(wifiZoneName)) { // Only if we're out of safe zone:


            // Make sure at least one provider is available
            boolean networkProviderEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (networkProviderEnabled) {
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
                iProviders++;
            }

            boolean gpsProviderEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (gpsProviderEnabled) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                iProviders++;
            }

            if (iProviders == 0) {
                sleep();
                return;
            }

        } else {
            reportSafeZone(wifiZoneName);
        }


        startMainProcessInForeground();

    }

    private void reportSafeZone(String wifiZoneName) {
        String wifiNetworks = Main.getInstance().getWifiNetworks();
        String deviceId = Main.getInstance().buildDeviceId();
        EventHttpReport eventHttpReport = new EventHttpReport(Main.getInstance().getBatteryStatus(),
                wifiNetworks, "0.0", "0.0", "0", "0", "safe", deviceId);
        EventBus.getDefault().post(eventHttpReport);
    }


    private void startMainProcessInForeground() {
        AlarmManager mgr;
        GregorianCalendar cal;
        Intent i;
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


    @Override
    public void onLocationChanged(Location location) {

//        double latitude = location.getLatitude();
//        double longitude = location.getLongitude();
//        String accuracy = String.format("%.0f", location.getAccuracy());
//
//        String s = Main.getInstance().buildDeviceId();
////
//        EventHttpReport eventHttpReport = new EventHttpReport(getBatteryStatus(), getWifiNetworks(), String.valueOf(latitude), String.valueOf(longitude), accuracy, String.valueOf(isInSafeZone()), deviceId);
//        EventBus.getDefault().post(eventHttpReport);


        boolean betterLocation = isBetterLocation(location);
        if (betterLocation) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            String accuracy = String.format("%.0f", location.getAccuracy());

            String wifiNetworks = Main.getInstance().getWifiNetworks();
            EventHttpReport eventHttpReport = new EventHttpReport(Main.getInstance().getBatteryStatus(),
                    wifiNetworks, String.valueOf(latitude), String.valueOf(longitude), String.valueOf(location.getSpeed()),
                    accuracy, DEFAULT_STATE, Main.getInstance().buildDeviceId());
            EventBus.getDefault().post(eventHttpReport);
        }


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


    /**
     * Determines whether one Location reading is better than the current Location fix
     *
     * @param location The new Location that you want to evaluate
     */
    public boolean isBetterLocation(Location location) {
        boolean result = false;

        if (Main.getInstance().currentBestLocation == null) {
            // A new location is always better than no location
            Main.getInstance().currentBestLocation = location;
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - Main.getInstance().currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > Const.DELAY_15_SEC;
        boolean isSignificantlyOlder = timeDelta < -Const.DELAY_15_SEC;
        boolean isNewer = timeDelta > 0;

        // If it's been more than XX sec since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            Main.getInstance().currentBestLocation = location;
            result = true;
            // If the new location is more than XX sec older, it must be worse
        } else if (isSignificantlyOlder) {
            result = false;
            return result;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - Main.getInstance().currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                Main.getInstance().currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            Main.getInstance().currentBestLocation = location;
            result = true;
        } else if (isNewer && !isLessAccurate) {
            Main.getInstance().currentBestLocation = location;
            result = true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            Main.getInstance().currentBestLocation = location;
            result = true;
        }


        return result;
    }

    /**
     * Checks whether two providers are the same
     */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }


    private void ping() {

        Map<ConfigurationKey, String> cfg = Main.getInstance().config;

        if (Const.TRUE_FLAG.equals(cfg.get(ConfigurationKey.DEVICE_PING_ENABLED))) {

            Date firstTime = new Date(0);
            if (firstTime.getTime() == pingTime.getTime()) {
                pingTime = new Date();
                return; // heartbeating later...
            }


            Integer pingMinutes = Integer.valueOf(cfg.get(ConfigurationKey.DEVICE_PING_MINUTES));
            if (Utils.clockTicked(pingTime, pingMinutes * 60 * 1000)) {

                try {
                    String pingMessage = Utils.fillPlaceholdersWithSystemVariables(Main.getInstance().config.get(ConfigurationKey.DEVICE_PING_TEXT));
                    String urlAddress = String.format(cfg.get(ConfigurationKey.DEVICE_PING_GATE_ADDRESS), Main.getInstance().buildDeviceId(), URLEncoder.encode(pingMessage, Const.UTF8_ENCODING));

                    HealthCheckReport healthCheck = new HealthCheckReport();
                    healthCheck.execute(urlAddress);

                } catch (Exception e) {
                    //be quiet
                }


                pingTime = new Date();
            }
        }
    }


    private void shutdownAppIfNeeded() {
/*
        Map<ConfigurationKey, String> cfg = Main.getInstance().config;
        if (Const.TRUE_FLAG.equals(cfg.get(ConfigurationKey.DEVICE_APP_SHUTDOWN_ENABLED))) {
            String shutdownTime = cfg.get(ConfigurationKey.DEVICE_APP_SHUTDOWN_TIME);
            Integer current = Integer.valueOf(Utils.currentTime().replaceAll(":", ""));
            Integer shutdown = Integer.valueOf(shutdownTime.replaceAll(":", ""));
            if (current.compareTo(shutdown) > 0) {
//                System.exit(1);
                android.os.Process.killProcess(android.os.Process.myPid());

//                Intent intent = new Intent(getApplicationContext(), Main.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                intent.putExtra("EXIT", true);
//                startActivity(intent);

            }

        }
*/
    }



}


