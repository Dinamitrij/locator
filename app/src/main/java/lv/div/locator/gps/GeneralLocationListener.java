package lv.div.locator.gps;

import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.os.Bundle;

import java.util.Iterator;

import lv.div.locator.BackgroundService;

public class GeneralLocationListener implements LocationListener, GpsStatus.Listener, GpsStatus.NmeaListener {

    private static String listenerName;
    private static BackgroundService mainService;
    protected String latestHdop;
    protected String latestPdop;
    protected String latestVdop;
    protected String geoIdHeight;
    protected String ageOfDgpsData;
    protected String dgpsId;

    public GeneralLocationListener(BackgroundService activity, String name) {
        mainService = activity;
        listenerName = name;
    }

    /**
     * Event raised when a new fix is received.
     */
    public void onLocationChanged(Location loc) {

        try {
            if (loc != null) {
                Bundle b = new Bundle();
                b.putString("HDOP", this.latestHdop);
                b.putString("PDOP", this.latestPdop);
                b.putString("VDOP", this.latestVdop);
                b.putString("GEOIDHEIGHT", this.geoIdHeight);
                b.putString("AGEOFDGPSDATA", this.ageOfDgpsData);
                b.putString("DGPSID", this.dgpsId);

                b.putBoolean("PASSIVE", listenerName.equalsIgnoreCase("PASSIVE"));
                b.putString("LISTENER", listenerName);

                loc.setExtras(b);
                mainService.OnLocationChanged(loc);

                this.latestHdop = "";
                this.latestPdop = "";
                this.latestVdop = "";
            }

        } catch (Exception ex) {
            int a = 0;
            a = a+1;


            //be quiet
//            tracer.error("GeneralLocationListener.onLocationChanged", ex);
        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        int a = 0;
        a = a+1;
    }

    @Override
    public void onProviderEnabled(String provider) {
        int a = 0;
        a = a+1;

    }

    @Override
    public void onProviderDisabled(String provider) {
        int a = 0;
        a = a+1;

    }

//    public void onProviderDisabled(String provider) {
//        tracer.info("Provider disabled: " + provider);
//        mainService.RestartGpsManagers();
//    }

//    public void onProviderEnabled(String provider) {
//        tracer.info("Provider enabled: " + provider);
//        mainService.RestartGpsManagers();
//    }

//    public void onStatusChanged(String provider, int status, Bundle extras) {
//        if (status == LocationProvider.OUT_OF_SERVICE) {
//            tracer.info(provider + " is out of service");
//            mainService.StopManagerAndResetAlarm();
//        }
//
//        if (status == LocationProvider.AVAILABLE) {
//            tracer.info(provider + " is available");
//        }
//
//        if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
//            tracer.info(provider + " is temporarily unavailable");
//            mainService.StopManagerAndResetAlarm();
//        }
//    }

//    public void onGpsStatusChanged(int event) {
//
//        switch (event) {
//            case GpsStatus.GPS_EVENT_FIRST_FIX:
//                tracer.debug(mainService.getString(R.string.fix_obtained));
//                break;
//
//            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
//
//                GpsStatus status = mainService.gpsLocationManager.getGpsStatus(null);
//
//                int maxSatellites = status.getMaxSatellites();
//
//                Iterator<GpsSatellite> it = status.getSatellites().iterator();
//                int count = 0;
//
//                while (it.hasNext() && count <= maxSatellites) {
//                    it.next();
//                    count++;
//                }
//
//                tracer.debug(String.valueOf(count) + " satellites");
//                mainService.SetSatelliteInfo(count);
//                break;
//
////            case GpsStatus.GPS_EVENT_STARTED:
////                tracer.info(mainService.getString(R.string.started_waiting));
////                break;
////
////            case GpsStatus.GPS_EVENT_STOPPED:
////                tracer.info(mainService.getString(R.string.gps_stopped));
////                break;
//
//        }
//    }

    @Override
    public void onNmeaReceived(long timestamp, String nmeaSentence) {
//        mainService.OnNmeaSentence(timestamp, nmeaSentence);

        int a=0;
        a=a+1;

//
//        if(Utilities.IsNullOrEmpty(nmeaSentence)){
//            return;
//        }
//
//        String[] nmeaParts = nmeaSentence.split(",");
//
//        if (nmeaParts[0].equalsIgnoreCase("$GPGSA")) {
//
//            if (nmeaParts.length > 15 && !Utilities.IsNullOrEmpty(nmeaParts[15])) {
//                this.latestPdop = nmeaParts[15];
//            }
//
//            if (nmeaParts.length > 16 &&!Utilities.IsNullOrEmpty(nmeaParts[16])) {
//                this.latestHdop = nmeaParts[16];
//            }
//
//            if (nmeaParts.length > 17 &&!Utilities.IsNullOrEmpty(nmeaParts[17]) && !nmeaParts[17].startsWith("*")) {
//
//                this.latestVdop = nmeaParts[17].split("\\*")[0];
//            }
//        }
//
//
//        if (nmeaParts[0].equalsIgnoreCase("$GPGGA")) {
//            if (nmeaParts.length > 8 &&!Utilities.IsNullOrEmpty(nmeaParts[8])) {
//                this.latestHdop = nmeaParts[8];
//            }
//
//            if (nmeaParts.length > 11 &&!Utilities.IsNullOrEmpty(nmeaParts[11])) {
//                this.geoIdHeight = nmeaParts[11];
//            }
//
//            if (nmeaParts.length > 13 &&!Utilities.IsNullOrEmpty(nmeaParts[13])) {
//                this.ageOfDgpsData = nmeaParts[13];
//            }
//
//            if (nmeaParts.length > 14 &&!Utilities.IsNullOrEmpty(nmeaParts[14]) && !nmeaParts[14].startsWith("*")) {
//                this.dgpsId = nmeaParts[14].split("\\*")[0];
//            }
//        }
    }

    @Override
    public void onGpsStatusChanged(int event) {
        int a = 0;
        a = a+1;

    }
}
