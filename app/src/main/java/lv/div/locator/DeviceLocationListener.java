package lv.div.locator;


import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import de.greenrobot.event.EventBus;
import lv.div.locator.events.EventHttpReport;

public class DeviceLocationListener implements LocationListener {

    @Override
    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        String accuracy = String.format("%.0f", location.getAccuracy());


        String wifiNetworks = Main.getInstance().getWifiNetworks();
        EventHttpReport eventHttpReport = new EventHttpReport(Main.getInstance().getBatteryStatus(),
                wifiNetworks, String.valueOf(latitude), String.valueOf(longitude),
                accuracy, String.valueOf(Main.getInstance().isInSafeZone(wifiNetworks)), Main.getInstance().buildDeviceId());
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
