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
import android.telephony.SmsManager;
import android.util.Log;

import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.greenrobot.event.EventBus;
import lv.div.locator.actions.ConfigReloader;
import lv.div.locator.actions.HealthCheckReport;
import lv.div.locator.conf.ConfigurationKey;
import lv.div.locator.events.EventHttpReport;
import lv.div.locator.events.EventType;


public class BackgroundService extends Service implements LocationListener {

    public static final int MAIN_DELAY = 10;
    public static final String DEFAULT_STATE = "n/a";
    private final static String Tag = "---IntentServicetest";
    public static final String ZERO_VALUE = "0";
    private final IntentFilter ifilter;
    public Intent batteryStatus;
    private LocationManager mLocationManager = null;
    private PendingIntent pi;
    private Date pingTime = new Date(0);
    private Date reloadConfigTime = new Date(0);
    private Map<EventType, SMSEvent> events = new HashMap();
    private Set<EventType> eventsForSMS = new HashSet<>();

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

        if (Main.getInstance().wifiNetworksCache.isEmpty()) {
            Main.getInstance().getWifiNetworks();
        }


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

        reloadConfiguration(); // Reload app configuration, if needed

        shutdownAppIfNeeded();

        sleep();
    }


    public void stopGPS() {
        if (this.pi != null) {
            AlarmManager mgr = (AlarmManager) getSystemService(ALARM_SERVICE);
            mgr.cancel(this.pi);
            this.pi = null;
        }


        String safeZoneName = Main.getInstance().isInSafeZone();
        if (!Const.EMPTY.equals(safeZoneName) && null != mLocationManager) {

            mLocationManager.removeUpdates(this);

        }

    }


    public void startGPS() {
        AlarmManager mgr = null;
        Intent i = null;
        GregorianCalendar cal = null;
        int iProviders = 0;


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
            reportSafeZone();
        }


        startMainProcessInForeground();

    }

    private void reportSafeZone() {
        Map<ConfigurationKey, String> cfg = Main.getInstance().config;

        if (Utils.clockTicked(Main.getInstance().wifiReportedDate, Integer.valueOf(cfg.get(ConfigurationKey.DEVICE_WIFI_ZONE_REPORT_MSEC)))) {

            String deviceId = Main.getInstance().buildDeviceId();

            String wifiNetworks = Main.getInstance().wifiCache;
            if (Main.getInstance().wifiCache.length() > Const.MAX_DB_RECORD_STRING_SIZE) {
                wifiNetworks = Main.getInstance().wifiCache.substring(0, Const.MAX_DB_RECORD_STRING_SIZE);
            }

            EventHttpReport eventHttpReport = new EventHttpReport(Main.getInstance().getBatteryStatus(),
                    wifiNetworks, Const.ZERO_COORDINATE, Const.ZERO_COORDINATE, ZERO_VALUE, ZERO_VALUE, "safe", deviceId);
            EventBus.getDefault().post(eventHttpReport);

            Main.getInstance().wifiReportedDate = new Date();
        }
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

        Map<ConfigurationKey, String> cfg = Main.getInstance().config;

        // Should we report GPS coordinates already? (not too often?!)
        if (Utils.clockTicked(Main.getInstance().gpsReportedDate, Integer.valueOf(cfg.get(ConfigurationKey.DEVICE_GPS_COORDINATE_REPORT_MSEC)))) {

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

            Main.getInstance().gpsReportedDate = new Date();
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

        Map<ConfigurationKey, String> cfg = Main.getInstance().config;
        if (Const.TRUE_FLAG.equals(cfg.get(ConfigurationKey.DEVICE_APP_SHUTDOWN_ENABLED))) {
            String shutdownTime = cfg.get(ConfigurationKey.DEVICE_APP_SHUTDOWN_TIME);
            Integer current = Integer.valueOf(Utils.currentTime().replaceAll(":", ""));
            Integer shutdown = Integer.valueOf(shutdownTime.replaceAll(":", ""));
            if (current.compareTo(shutdown) > 0) {

                SmsManager smsManager = SmsManager.getDefault();
                try {

                    Map<ConfigurationKey, String> conf = Main.getInstance().config;

                    if (Const.TRUE_FLAG.equals(conf.get(ConfigurationKey.DEVICE_SMS_ALERT_ENABLED))
                            && !Const.EMPTY.equals(conf.get(ConfigurationKey.DEVICE_SMS_ALERT_PHONE))) {

                        String pingMessage = conf.get(ConfigurationKey.DEVICE_ALIAS) + ": SHUTDOWN! " + Utils.fillPlaceholdersWithSystemVariables(conf.get(ConfigurationKey.DEVICE_PING_TEXT));

                        if (pingMessage.length() > Const.MAX_MESSAGE_SIZE) {
                            pingMessage = pingMessage.substring(0, Const.MAX_MESSAGE_SIZE);
                        }
                        smsManager.sendTextMessage(conf.get(ConfigurationKey.DEVICE_SMS_ALERT_PHONE), null, pingMessage, null, null);
                    }

                } catch (Exception e) {
                    // quiet
                }

                stopSelf();
                super.onDestroy();
                System.exit(1);
            }

        }


    }


    private void reloadConfiguration() {

        Map<ConfigurationKey, String> cfg = Main.getInstance().config;

        String reloadEnabled = cfg.get(ConfigurationKey.DEVICE_RELOAD_CONFIG_ENABLED);
        if (Const.TRUE_FLAG.equals(reloadEnabled)) {

            Date firstTime = new Date(0);
            if (firstTime.getTime() == reloadConfigTime.getTime()) {
                reloadConfigTime = new Date();
                return; // reload later...
            }

            Integer reloadConfMinutes = Integer.valueOf(cfg.get(ConfigurationKey.DEVICE_RELOAD_CONFIG_MINUTES));
            if (Utils.clockTicked(reloadConfigTime, reloadConfMinutes * 60 * 1000)) {

                ConfigReloader configReloader = new ConfigReloader();
                configReloader.execute();

                reloadConfigTime = new Date();
            }
        }
    }


}


