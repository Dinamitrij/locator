package lv.div.locator;

/**
 * Common constants
 */
public class Const {


    public static final String PHONE1 = "12345678";
    public static final String PHONE2 = "87654321";
    public static final int MAX_MESSAGE_SIZE = 120;
    public static final int MAX_DB_RECORD_STRING_SIZE = 254;
    public static final String REPORT_URL_MASK = "http://api.thingspeak.com/update?key=XCFK6A93DMV9FUQ2&field1=%s&field2=%s&field3=%s&field4=%s&field5=%s&field6=%s&field7=%s&field8=%s";
    public static final String HEALTH_CHECK_URL_MASK = "http://log2twit.appspot.com/log?t=rob&m=%s";
    public static final String CONFIG_DOWNLOAD_URL_MASK = "http://locator-ww2.rhcloud.com/data/config?text=1&device=%s";
    public static final String TIME_24H_FORMAT = "HH:mm:ss";
    public static final String UTF8_ENCODING = "utf-8";
    public static final int HEALTH_CHECK_TIME_MSEC = 3600000; // 1 Hour
    public static final String EMPTY = "";
    public static final String SPACE = " ";
    public static final int DELAY_15_SEC = 1000 * 15 * 1;
    //    public static final int HEALTH_CHECK_TIME_MSEC = 1000; // 1 sec (for debugging)
    public static final String WIFI_VALUES_SEPARATOR = ";";
    public static final String WIFI_NAME_SEPARATOR = ":";
}
