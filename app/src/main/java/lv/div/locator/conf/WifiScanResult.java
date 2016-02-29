package lv.div.locator.conf;

/**
 * Data holder for Wifi Scan result
 */
public class WifiScanResult {

    private String bssid;
    private String ssid;
    private int level;


    public String getBssid() {
        return bssid;
    }

    public void setBssid(String bssid) {
        this.bssid = bssid;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
