package lv.div.locator.events;

public class EventHttpReport {


    private String batteryStatus;
    private String wifiData;
    private String gpsData;

    public EventHttpReport(String batteryStatus, String wifiData, String gpsData) {
        this.batteryStatus = batteryStatus;
        this.wifiData = wifiData;
        this.gpsData = gpsData;
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
}
