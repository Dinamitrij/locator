package lv.div.locator.events;

public class EventHttpReport {


    private String batteryStatus;
    private String wifiData;
    private String gpsData;
    private String accuracy;
    private String safe;

    public EventHttpReport(String batteryStatus, String wifiData, String gpsData, String accuracy, String safe) {
        this.batteryStatus = batteryStatus;
        this.wifiData = wifiData;
        this.gpsData = gpsData;
        this.accuracy = accuracy;
        this.safe = safe;
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

    public String getGpsData() {
        return gpsData;
    }

    public void setGpsData(String gpsData) {
        this.gpsData = gpsData;
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
}
