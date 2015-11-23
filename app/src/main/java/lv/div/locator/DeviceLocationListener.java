package lv.div.locator;


import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import de.greenrobot.event.EventBus;
import lv.div.locator.conf.Constant;
import lv.div.locator.events.EventHttpReport;

public class DeviceLocationListener implements LocationListener {

    @Override
    public void onLocationChanged(Location location) {

        boolean betterLocation = isBetterLocation(location);
        if (betterLocation) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            String accuracy = String.format("%.0f", location.getAccuracy());

            String wifiNetworks = Main.getInstance().getWifiNetworks();
            EventHttpReport eventHttpReport = new EventHttpReport(Main.getInstance().getBatteryStatus(),
                    wifiNetworks, String.valueOf(latitude), String.valueOf(longitude), String.valueOf(location.getSpeed()),
                    accuracy, "n/a", Main.getInstance().buildDeviceId());
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

}
