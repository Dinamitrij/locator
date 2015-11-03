package lv.div.locator.conf;

/**
 * Configuration keys for application settings
 */
public enum ConfigurationKey {

    /**
     * Configuration keys for device states
     */
    IN_SAFE_ZONE("IN_SAFE_ZONE"),
    OUT_OF_SAFE_ZONE("OUT_OF_SAFE_ZONE"),
    OUT_OF_SAFE_ZONE_TIMES("OUT_OF_SAFE_ZONE_POINTS"),


    /**
     * Common values for all devices
     */
    DISPLAY_LAST_POINTS("DISPLAY_LAST_POINTS"),
    LEAVE_SAFE_ZONE_POINTS("LEAVE_SAFE_ZONE_POINTS"),
    ENTER_SAFE_ZONE_POINTS("ENTER_SAFE_ZONE_POINTS"),
    LEAVE_SAFE_ZONE_MESSAGE("LEAVE_SAFE_ZONE_MESSAGE"),
    ENTER_SAFE_ZONE_MESSAGE("ENTER_SAFE_ZONE_MESSAGE"),
    SEND_ALERT_ADDRESS("SEND_ALERT_ADDRESS"),
    ADMIN_ALERT_ENABLED("ADMIN_ALERT_ENABLED"),
    ADMIN_ALERT_ADDRESS("ADMIN_ALERT_ADDRESS"),

    /**
     * Device specific settings
     */
    SAFE_ZONE_WIFI("SAFE_ZONE_WIFI"),
    DEVICE_ALIAS("DEVICE_ALIAS"),
    SEND_ALERT_ADDRESS_PARAM1("SEND_ALERT_ADDRESS_PARAM1"),
    SEND_ALERT_ADDRESS_PARAM2("SEND_ALERT_ADDRESS_PARAM2"),
    SEND_ALERT_ADDRESS_PARAM3("SEND_ALERT_ADDRESS_PARAM3"),
    DEVICE_SEND_ALERT_ADDRESS("DEVICE_SEND_ALERT_ADDRESS"),
    DEVICE_WIFI_REFRESH_MSEC("DEVICE_WIFI_REFRESH_MSEC"),
    DEVICE_PING_ENABLED("DEVICE_PING_ENABLED"),
    DEVICE_PING_GATE_ADDRESS("DEVICE_PING_GATE_ADDRESS"),
    DEVICE_PING_ADDRESS("DEVICE_PING_ADDRESS"),
    DEVICE_PING_TEXT("DEVICE_PING_TEXT"),
    DEVICE_PING_MINUTES("DEVICE_PING_MINUTES");


    private final String configurationKey;


    private ConfigurationKey(final String configurationKey) {
        this.configurationKey = configurationKey;
    }

    @Override
    public String toString() {
        return configurationKey;
    }

}
