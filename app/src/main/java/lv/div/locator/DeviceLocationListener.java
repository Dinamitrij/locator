package lv.div.locator;


import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Date;

import lv.div.locator.events.EventType;

public class DeviceLocationListener implements LocationListener {
    private static DeviceLocationListener ourInstance = new DeviceLocationListener();

    public static DeviceLocationListener getInstance() {
        return ourInstance;
    }

    private DeviceLocationListener() {
    }

    private boolean searchInProgress = false;
    private String lastGpsStatus = "No GPS data";
    private Date lastGpsStatusTime = new Date(0);



    @Override
    public void onLocationChanged(Location location) {
        setLastGpsStatusTime(new Date());
        lastGpsStatus = location.getLongitude() + " # " + location.getLatitude();
//            String longitude = "Longitude: " + loc.getLongitude();
//            String latitude = "Latitude: " + loc.getLatitude();


//        SMSEvent smsEvent = new SMSEvent();
//        smsEvent.setAlertMessage(lastGpsStatus);
//        smsEvent.setEventTime(new Date());
////            smsEvent.setSmsRepeatDelayMsec(20000);
//        smsEvent.setProblemType(EventType.LOCATION);
//        ArrayList<String> phonesToAlert = new ArrayList<>();
//        phonesToAlert.add(Const.PHONE1);
//        smsEvent.setPhonesToAlert(phonesToAlert);

        searchInProgress = false;

//        events.put(EventType.LOCATION, smsEvent);
    }


    public String getLastGpsStatus() {
        return lastGpsStatus;
    }

    public void setLastGpsStatus(String lastGpsStatus) {
        this.lastGpsStatus = lastGpsStatus;
    }

    public Date getLastGpsStatusTime() {
        return lastGpsStatusTime;
    }

    public void setLastGpsStatusTime(Date lastGpsStatusTime) {
        this.lastGpsStatusTime = lastGpsStatusTime;
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

    public boolean isSearchInProgress() {
        return searchInProgress;
    }

    public void setSearchInProgress(boolean searchInProgress) {
        this.searchInProgress = searchInProgress;
    }
}