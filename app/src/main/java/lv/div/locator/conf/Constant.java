package lv.div.locator.conf;

/**
 * Local constants
 */
public class Constant {
    public static final int MAX_MESSAGE_SIZE = 120;
    public static final int MAX_DB_RECORD_STRING_SIZE = 254;
    public static final String CONFIG_DOWNLOAD_URL_MASK = "http://locator.v1.lv/config/config?device=%s&text=1";
    public static final String NO_INTERNET_SMS_ALERT_POSTFIX = " / No Internet connection";
    public static final int DELAY_15_SEC = 1000 * 15 * 1;
    public static final int NOLOCATION_DELAY_15_SEC = 1000 * 15 * 1;
    public static final int LOG_BUFFER_SIZE = 2048;
    public static final int ZIP_BUFFER_SIZE = 512;
    public static final int ENTER_SAFE_ZONE_POINTS = 2;
    public static final String SMS_COMMAND_START_APP = "#LOCSTART";
    public static final String SMS_COMMAND_STOP_APP = "#LOCSTOP";
    public static final int DELAY_BETWEEN_LOCATION_UPDATES_MSEC = 2000;
    public static final int DISTANCE_BETWEEN_LOCATION_UPDATE_METERS = 15;
}
