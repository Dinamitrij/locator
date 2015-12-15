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
import android.os.Environment;
import android.os.IBinder;
import android.telephony.SmsManager;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.greenrobot.event.EventBus;
import lv.div.locator.actions.ConfigReloader;
import lv.div.locator.actions.HealthCheckReport;
import lv.div.locator.commons.conf.ConfigurationKey;
import lv.div.locator.commons.conf.Const;
import lv.div.locator.conf.Constant;
import lv.div.locator.events.EventHttpReport;
import lv.div.locator.events.EventType;
import lv.div.locator.utils.FLogger;


public class BackgroundService extends Service implements LocationListener {

    public static final int MAIN_DELAY = 10;
    public static final String DEFAULT_STATE = "n/a";
    private final IntentFilter ifilter;
    public Intent batteryStatus;
    private LocationManager mLocationManager = null;
    private PendingIntent pi;
    private Date pingTime = new Date(0);
    private Date reloadConfigTime = new Date(0);
    private Map<EventType, SMSEvent> events = new HashMap();

    public BackgroundService() {

        FLogger.getInstance().log(this.getClass(), "BackgroundService constructor called.");
        Main.mServiceInstance = this;
        ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        super.onStartCommand(intent, flags, startId);
        FLogger.getInstance().log(this.getClass(), "onStartCommand() called.");


        if (null == mLocationManager) {
            FLogger.getInstance().log(this.getClass(), "onStartCommand() mLocationManager=null");
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
        FLogger.getInstance().log(this.getClass(), "onDestroy() called");
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
        FLogger.getInstance().log(this.getClass(), "sleep() called");
        AlarmManager mgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent i = new Intent(this, MainReceiver.class);
        Calendar cal = new GregorianCalendar();

        cal.add(Calendar.SECOND, getMainDelay());

        this.pi = PendingIntent.getBroadcast(this, 0, i, 0);
        mgr.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), this.pi);
    }


    public void gpsTimeout() {
        FLogger.getInstance().log(this.getClass(), "gpsTimeout() called");

        stopGPS();

        ping(); // Send healthcheck alert if needed

        reportWifiNetworks(); // Report Wifi networks if needed (especially, between SafeZone and onLocationChanged() event!)

        reloadConfiguration(); // Reload app configuration, if needed

        shutdownAppIfNeeded();

        powerOff(); // Power Off application, if needed

        sleep();
    }


    public void stopGPS() {
        FLogger.getInstance().log(this.getClass(), "stopGPS() called");
        if (this.pi != null) {
            AlarmManager mgr = (AlarmManager) getSystemService(ALARM_SERVICE);
            mgr.cancel(this.pi);
            FLogger.getInstance().log(this.getClass(), "stopGPS() AlarmManager canceled");
            this.pi = null;
        }


        String safeZoneName = Main.getInstance().isInSafeZone();
        if (!Const.EMPTY.equals(safeZoneName) && null != mLocationManager) {

            mLocationManager.removeUpdates(this);
            FLogger.getInstance().log(this.getClass(), "stopGPS() mLocationManager.removeUpdates(this);");
        }

    }


    public void startGPS() {
        FLogger.getInstance().log(this.getClass(), "startGPS() called");
        AlarmManager mgr = null;
        Intent i = null;
        GregorianCalendar cal = null;
        int iProviders = 0;


        String wifiZoneName = Main.getInstance().isInSafeZone();
        FLogger.getInstance().log(this.getClass(), "startGPS() wifiZoneName = " + wifiZoneName);
        if (Const.EMPTY.equals(wifiZoneName) && !Main.getInstance().shuttingDown) { // Only if we're out of safe zone, and NOT shutting down:


            FLogger.getInstance().log(this.getClass(), "startGPS() wifiZoneName is empty. Preparing mLocationManager.");
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
                if (!Main.getInstance().gpsLocationRequested) { // We're not waiting for location...
                    Main.getInstance().gpsLocationRequestTime = new Date(); // We just requested GPS location [change]
                    Main.getInstance().gpsLocationRequested = true; // Just requested location. Let's wait for result
                }
            }

            if (iProviders == 0) {
                FLogger.getInstance().log(this.getClass(), "startGPS() iProviders == 0. Sleep!");
                sleep();
                return;
            }

        } else {
            reportWifiNetworks();
        }


        startMainProcessInForeground();

    }

    private void reportWifiNetworks() {
        if (Main.getInstance().shuttingDown) {
            return; // Do not proceed. We're shutting down now...
        }

        FLogger.getInstance().log(this.getClass(), "reportWifiNetworks() called");
        Map<ConfigurationKey, String> cfg = Main.getInstance().config;

        String safeZoneName = Main.getInstance().isInSafeZone();

        Integer safeReportTimes = Integer.valueOf(cfg.get(ConfigurationKey.DEVICE_SAFE_ZONE_WIFI_REPORT_SLOWER_TIMES));

        if (Utils.clockTicked(Main.getInstance().wifiReportedDate, Integer.valueOf(cfg.get(ConfigurationKey.DEVICE_WIFI_ZONE_REPORT_MSEC)))) {
            FLogger.getInstance().log(this.getClass(), "reportWifiNetworks() Need to report Wifi data!");


            if (Const.EMPTY.equals(safeZoneName)) { // NON-Safe zone - Do we need to report this?

                if (Main.getInstance().gpsLocationRequested &&
                        Utils.clockTicked(Main.getInstance().gpsLocationRequestTime, Constant.NOLOCATION_DELAY_15_SEC)) {

                    FLogger.getInstance().log(this.getClass(), "reportWifiNetworks() - NON-Safe zone, but we're waiting too long for GPS location update. Let's log at least Wifi networks.");

                    prepareAndSendWifiReport();

                    Main.getInstance().wifiReportedDate = new Date();
                } else {
                    FLogger.getInstance().log(this.getClass(), "reportWifiNetworks() - NON-Safe zone. Waiting for GPS. Not reporting Wifi yet.");
                }
            } else {
                //Safe zone, SLOW DOWN reporting speed, if needed (Internet Traffic Saver)
                FLogger.getInstance().log(this.getClass(), "reportWifiNetworks() accuSize=" + Main.getInstance().safeZoneTimesCount);
                boolean justEntered = Main.getInstance().safeZoneTimesCount <= Constant.ENTER_SAFE_ZONE_POINTS;
                boolean accumulatorCheckPoint = (Main.getInstance().safeZoneTimesCount >= safeReportTimes) && (Main.getInstance().safeZoneTimesCount % safeReportTimes == 0);

                if (justEntered) {
                    FLogger.getInstance().log(this.getClass(), "reportWifiNetworks() justEntered! < Constant.ENTER_SAFE_ZONE_POINTS. Inc+");
                    prepareAndSendWifiReport();
                    Main.getInstance().safeZoneTimesCount = Main.getInstance().safeZoneTimesCount + 1; // Increment accumulator
                } else if (accumulatorCheckPoint) {
                    FLogger.getInstance().log(this.getClass(), "reportWifiNetworks() accumulatorCheckPoint!");
                    prepareAndSendWifiReport();
                    Main.getInstance().safeZoneTimesCount = Main.getInstance().safeZoneTimesCount + 1; // Increment accumulator
                } else {
                    Main.getInstance().safeZoneTimesCount = Main.getInstance().safeZoneTimesCount + 1; // Increment accumulator
                    FLogger.getInstance().log(this.getClass(), "reportWifiNetworks() accuSize++; Now accu=" + Main.getInstance().safeZoneTimesCount);
                }


            }

            FLogger.getInstance().log(this.getClass(), "reportWifiNetworks() EventBus.getDefault().post(eventHttpReport);");

        } else {

            FLogger.getInstance().log(this.getClass(), "reportWifiNetworks() NO need to report Wifi data yet...");
        }
    }

    private void prepareAndSendWifiReport() {
        FLogger.getInstance().log(this.getClass(), "prepareAndSendWifiReport() called");
        String deviceId = Main.getInstance().buildDeviceId();
        String wifiNetworks = Main.getInstance().getWifiNetworks();
        if (Main.getInstance().wifiCache.length() > Constant.MAX_DB_RECORD_STRING_SIZE) {
            wifiNetworks = Main.getInstance().wifiCache.substring(0, Constant.MAX_DB_RECORD_STRING_SIZE);
        }

        EventHttpReport eventHttpReport = new EventHttpReport(Main.getInstance().getBatteryStatus(),
                wifiNetworks, Const.ZERO_COORDINATE, Const.ZERO_COORDINATE, Const.ZERO_VALUE, Const.ZERO_VALUE, "?", deviceId);
        EventBus.getDefault().post(eventHttpReport);

    }


    private void startMainProcessInForeground() {
        FLogger.getInstance().log(this.getClass(), "startMainProcessInForeground() called");
        AlarmManager mgr;
        GregorianCalendar cal;
        Intent i;
        mgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        cal = new GregorianCalendar();
        i = new Intent(this, TimeoutReceiver.class);
        this.pi = PendingIntent.getBroadcast(this, 0, i, 0);
        cal.add(Calendar.SECOND, getMainDelay());
        mgr.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), this.pi);
        FLogger.getInstance().log(this.getClass(), "startMainProcessInForeground() mgr.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), this.pi);");

        // Starting process in foreground:
        Notification note = new Notification.Builder(this).setContentTitle("Locator is on")
                .setContentIntent(pi)
                .setContentText("ContentText")
                .build();

        note.flags |= Notification.FLAG_NO_CLEAR;
        startForeground(8080, note);
        FLogger.getInstance().log(this.getClass(), "startMainProcessInForeground() startForeground(8080, note);");
    }


    @Override
    public void onLocationChanged(Location location) {
        FLogger.getInstance().log(this.getClass(), "onLocationChanged() called");

        Main.getInstance().gpsLocationRequested = false; // Location update is not in "Requested" state anymore

        Map<ConfigurationKey, String> cfg = Main.getInstance().config;

        // Should we report GPS coordinates already? (not too often?!)
        if (Utils.clockTicked(Main.getInstance().gpsReportedDate, Integer.valueOf(cfg.get(ConfigurationKey.DEVICE_GPS_COORDINATE_REPORT_MSEC)))) {
            FLogger.getInstance().log(this.getClass(), "onLocationChanged() Need to report GPS coords #--->");

            boolean betterLocation = isBetterLocation(location);
            if (betterLocation) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                String accuracy = String.format("%.0f", location.getAccuracy());

                if (!Const.TRUE_FLAG.equals(cfg.get(ConfigurationKey.DEVICE_COLLECT_BSSID_MODE_ENABLED))) {

                    String wifiNetworks = Main.getInstance().getWifiNetworks();
                    EventHttpReport eventHttpReport = new EventHttpReport(Main.getInstance().getBatteryStatus(),
                            wifiNetworks, String.valueOf(latitude), String.valueOf(longitude), String.valueOf(location.getSpeed()),
                            accuracy, DEFAULT_STATE, Main.getInstance().buildDeviceId());
                    EventBus.getDefault().post(eventHttpReport);
                    FLogger.getInstance().log(this.getClass(), "onLocationChanged() GPS reporting EventBus.getDefault().post(eventHttpReport);");

                } else {

                    FLogger.getInstance().log(this.getClass(), "onLocationChanged() DEVICE_COLLECT_BSSID_MODE_ENABLED");
                    if (!Main.getInstance().bssidNetworks.isEmpty()) {

                        JSONObject obj = new JSONObject();

                        try {
                            obj.put("device", Main.getInstance().buildDeviceId());
                            obj.put("devicename", cfg.get(ConfigurationKey.DEVICE_ALIAS));
                            obj.put("battery", Main.getInstance().getBatteryStatus());
                            obj.put("latitude", String.valueOf(latitude));
                            obj.put("longitude", String.valueOf(longitude));
                            obj.put("accuracy", accuracy);


                            JSONArray jarr = new JSONArray();
                            for (String bssid : Main.getInstance().bssidNetworks.keySet()) {
                                JSONObject arrayObject = new JSONObject();
                                arrayObject.put("name", Main.getInstance().bssidNetworks.get(bssid).replace('"', '`'));
                                arrayObject.put("bssid", bssid);
                                jarr.put(arrayObject);
                            }

                            obj.put("bssids", jarr);

                            String jsonToSend = obj.toString();

                            EventHttpReport eventHttpReport = new EventHttpReport(null, jsonToSend, null, null, null, null, null, null);
                            EventBus.getDefault().post(eventHttpReport);
                            FLogger.getInstance().log(this.getClass(), "onLocationChanged() BSSID reporting EventBus.getDefault().post(eventHttpReport);");
                        } catch (Exception e) {
                            FLogger.getInstance().log(this.getClass(), "----> " + e.getMessage());
                            FLogger.getInstance().log(this.getClass(), Utils.stToString(e.getStackTrace()));

                        }

                    }
                }


            } else {
                FLogger.getInstance().log(this.getClass(), "onLocationChanged() NOT a better location");
            }

            Main.getInstance().gpsReportedDate = new Date();
        } else {
            FLogger.getInstance().log(this.getClass(), "onLocationChanged() NO need to report GPS coords yet...");
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
        boolean isSignificantlyNewer = timeDelta > Constant.DELAY_15_SEC;
        boolean isSignificantlyOlder = timeDelta < -Constant.DELAY_15_SEC;
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
        if (Main.getInstance().shuttingDown) {
            return; // Do not proceed. We're shutting down now...
        }

        FLogger.getInstance().log(this.getClass(), "ping() called");
        Map<ConfigurationKey, String> cfg = Main.getInstance().config;

        if (Const.TRUE_FLAG.equals(cfg.get(ConfigurationKey.DEVICE_PING_ENABLED))) {

            Date firstTime = new Date(0);
            if (firstTime.getTime() == pingTime.getTime()) {
                pingTime = new Date();
                return; // heartbeating later...
            }


            Integer pingMinutes = Integer.valueOf(cfg.get(ConfigurationKey.DEVICE_PING_MINUTES));
            if (Utils.clockTicked(pingTime, pingMinutes * 60 * 1000)) {
                FLogger.getInstance().log(this.getClass(), "ping() need to ping");
                try {
                    String pingMessage = Utils.fillPlaceholdersWithSystemVariables(Main.getInstance().config.get(ConfigurationKey.DEVICE_PING_TEXT));
                    String urlAddress = String.format(cfg.get(ConfigurationKey.DEVICE_PING_GATE_ADDRESS), Main.getInstance().buildDeviceId(), URLEncoder.encode(pingMessage, Const.UTF8_ENCODING));

                    HealthCheckReport healthCheck = new HealthCheckReport();
                    healthCheck.execute(urlAddress);
                    FLogger.getInstance().log(this.getClass(), "ping() healthCheck.execute(urlAddress)");

                } catch (Exception e) {
                    FLogger.getInstance().log(this.getClass(), "----> " + e.getMessage());
                    FLogger.getInstance().log(this.getClass(), Utils.stToString(e.getStackTrace()));
                }

                pingTime = new Date();
            }
        }
    }


    private void shutdownAppIfNeeded() {
        if (Main.getInstance().shuttingDown) {
            return; // Do not proceed. We're already shutting down!
        }
        Map<ConfigurationKey, String> cfg = Main.getInstance().config;
        if (Const.TRUE_FLAG.equals(cfg.get(ConfigurationKey.DEVICE_APP_SHUTDOWN_ENABLED))) {
            String shutdownTime = cfg.get(ConfigurationKey.DEVICE_APP_SHUTDOWN_TIME);
            Integer current = Integer.valueOf(Utils.currentTime().replaceAll(":", ""));
            Integer shutdown = Integer.valueOf(shutdownTime.replaceAll(":", ""));
            if (current.compareTo(shutdown) > 0) { // Shutdown now!!!
                Main.getInstance().shuttingDown = true;

                if (Const.TRUE_FLAG.equals(cfg.get(ConfigurationKey.DEVICE_LOCAL_LOGGING_ENABLED))) {
                    // Zip Log, send to server, then, onSuccess or onFailure - close App
                    zipLogFile();
                    sendZipToServer();
                } else {
                    Main.getInstance().readyForPowerOff = true;
                }

            }

        }


    }

    private void sendShutdownSms() {
        Map<ConfigurationKey, String> cfg = Main.getInstance().config;
        SmsManager smsManager = SmsManager.getDefault();
        try {

            if (Const.TRUE_FLAG.equals(cfg.get(ConfigurationKey.DEVICE_SMS_ALERT_ENABLED))
                    && !Const.EMPTY.equals(cfg.get(ConfigurationKey.DEVICE_SMS_ALERT_PHONE))) {

                String pingMessage = cfg.get(ConfigurationKey.DEVICE_ALIAS) + ": SHUTDOWN! " + Utils.fillPlaceholdersWithSystemVariables(cfg.get(ConfigurationKey.DEVICE_PING_TEXT));

                if (pingMessage.length() > Constant.MAX_MESSAGE_SIZE) {
                    pingMessage = pingMessage.substring(0, Constant.MAX_MESSAGE_SIZE);
                }
                smsManager.sendTextMessage(cfg.get(ConfigurationKey.DEVICE_SMS_ALERT_PHONE), null, pingMessage, null, null);
            }

        } catch (Exception e) {
            // quiet
        }
    }

    /**
     * Shutdown application
     */
    private void powerOff() {
        if (Main.getInstance().readyForPowerOff) {
            sendShutdownSms();
            stopSelf();
            super.onDestroy();
            System.exit(1);
        }
    }

    private void sendZipToServer() {
        final Map<ConfigurationKey, String> cfg = Main.getInstance().config;

        File zippedLogFileToSend = zipFileObject();
        RequestParams params = new RequestParams();
        try {
            params.put(Const.DEVICE_ID_HTTP_PARAMETER, Main.getInstance().buildDeviceId());
            params.put(Const.DEVICE_ALIAS_HTTP_PARAMETER, cfg.get(ConfigurationKey.DEVICE_ALIAS));
            params.put(Const.ZIPPED_LOG_FILENAME_PARAM, zipFileName());
            params.put(Const.ZIPPED_LOG_FILECONTENT_PARAM, new FileInputStream(zippedLogFileToSend));

        } catch (FileNotFoundException e) {
        }


        AsyncHttpClient client = new AsyncHttpClient();
        try {
            String url = cfg.get(ConfigurationKey.DEVICE_LOCAL_LOGGING_EXPORT_URL);
            client.post(getBaseContext(), url, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {

                    if (Const.TRUE_FLAG.equals(cfg.get(ConfigurationKey.DEVICE_LOCAL_LOGGING_EXPORT_URL.DEVICE_LOCAL_LOGGING_DELETE_AFTER_SENT))) {
                        File zippedLogFileToSend = zipFileObject();
                        boolean deleted = zippedLogFileToSend.delete();

                        File oldLog = new File(Environment.getExternalStorageDirectory(), Main.getInstance().buildLogFileName());
                        deleted = oldLog.delete();
                    }

                    Main.getInstance().readyForPowerOff = true;
                }

                @Override
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                    Main.getInstance().readyForPowerOff = true;
                }
            });
        } catch (Exception e) {
            Main.getInstance().readyForPowerOff = true;
        }

    }


    private File zipFileObject() {
        String zipFileName = zipFileName();
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        File zipFile = new File(externalStorageDirectory, zipFileName);
        return zipFile;
    }


    private String zipFileName() {
        String logFileName = Main.getInstance().buildLogFileName();
        return logFileName + ".zip";
    }


    public void zipLogFile() {
        File zippedLogFileToSend = zipFileObject();
        boolean deleted = zippedLogFileToSend.delete(); // Cleanup old ZIP file

        List<String> files = new ArrayList<>();
        String logFileName = Main.getInstance().buildLogFileName();
        files.add(logFileName);

        BufferedInputStream origin = null;
        ZipOutputStream out = null;
        try {
            out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFileObject())));
            byte data[] = new byte[Constant.ZIP_BUFFER_SIZE];

            for (String file : files) {
                FileInputStream fi = new FileInputStream(new File(Environment.getExternalStorageDirectory(), file));
                origin = new BufferedInputStream(fi, Constant.ZIP_BUFFER_SIZE);
                try {
                    ZipEntry entry = new ZipEntry(file.substring(file.lastIndexOf("/") + 1));
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, Constant.ZIP_BUFFER_SIZE)) != -1) {
                        out.write(data, 0, count);
                    }
                } finally {
                    origin.close();
                }
            }
        } catch (Exception e) {
            // quiet
        } finally {
            try {
                out.close();
            } catch (Exception e) {
                // quiet
            }
        }


    }


    private void reloadConfiguration() {
        if (Main.getInstance().shuttingDown) {
            return; // Do not proceed. We're shutting down now...
        }
        FLogger.getInstance().log(this.getClass(), "reloadConfiguration() called");
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
                FLogger.getInstance().log(this.getClass(), "reloadConfiguration() configReloader.execute();");

                reloadConfigTime = new Date();
            }
        }
    }


}


