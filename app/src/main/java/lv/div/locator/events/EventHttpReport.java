package lv.div.locator.events;

public class EventHttpReport {


    private String batteryStatus;
    private String wifiData;
    private String latitude;
    private String longitude;
    private String speed;
    private String accuracy;
    private String safe;
    private String deviceId;
    private String mlsData;

    public EventHttpReport(String batteryStatus, String wifiData, String latitude, String longitude, String speed, String accuracy, String safe, String deviceId, String mlsData) {
        this.batteryStatus = batteryStatus;
        this.wifiData = wifiData;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.accuracy = accuracy;
        this.safe = safe;
        this.deviceId = deviceId;
        this.mlsData = mlsData;
    }


    public String getSpeed() {
        return speed;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public String getBatteryStatus() {
        return batteryStatus;
    }

    public void setBatteryStatus(String batteryStatus) {
        this.batteryStatus = batteryStatus;
    }

    public String getWifiData() {
        return wifiData;
    }

    public void setWifiData(String wifiData) {
        this.wifiData = wifiData;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(String accuracy) {
        this.accuracy = accuracy;
    }

    public String getSafe() {
        return safe;
    }

    public void setSafe(String safe) {
        this.safe = safe;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getMlsData() {
        return mlsData;
    }

    public void setMlsData(String mlsData) {
        this.mlsData = mlsData;
    }
}
