package lv.div.locator.actions;


import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Map;

import de.greenrobot.event.EventBus;
import lv.div.locator.Main;
import lv.div.locator.Utils;
import lv.div.locator.commons.conf.ConfigurationKey;
import lv.div.locator.commons.conf.Const;
import lv.div.locator.events.EventHttpReport;
import lv.div.locator.utils.FLogger;

public class HttpReportSender {

    private EventBus bus = EventBus.getDefault();
    private URL url;

    public HttpReportSender() {
        bus.register(this);
    }

    public void onEvent(EventHttpReport event) {
        String logText = Const.EMPTY;
        FLogger.getInstance().log(this.getClass(), "onEvent() called");
        processReportSending(event, logText);
    }

    private synchronized void processReportSending(EventHttpReport event, String logText) {
        try {
            Map<ConfigurationKey, String> cfg = Main.getInstance().config;
            String urlAddress = Const.EMPTY;

            if (!Const.TRUE_FLAG.equals(cfg.get(ConfigurationKey.DEVICE_COLLECT_BSSID_MODE_ENABLED)) && null != event.getBatteryStatus()) {

                String batteryStatus = URLEncoder.encode(event.getBatteryStatus(), Const.UTF8_ENCODING);
                String wifiData = URLEncoder.encode(event.getWifiData(), Const.UTF8_ENCODING);
                String mlsData = URLEncoder.encode(event.getMlsData(), Const.UTF8_ENCODING);
                String latitude = URLEncoder.encode(event.getLatitude(), Const.UTF8_ENCODING);
                String longitude = URLEncoder.encode(event.getLongitude(), Const.UTF8_ENCODING);
                String accelerometer = String.valueOf(Main.getInstance().accelerometerValue);
                String deviceId = event.getDeviceId();
                long deviceTime = (new Date()).getTime();

                urlAddress = String.format(cfg.get(ConfigurationKey.DEVICE_REPORT_URL_MASK), latitude, longitude, wifiData, event.getAccuracy(), event.getSafe(), batteryStatus, event.getSpeed(), deviceId, deviceTime, accelerometer, mlsData);
                logText = urlAddress;

            } else {
                String data = URLEncoder.encode(event.getWifiData(), Const.UTF8_ENCODING);
                urlAddress = String.format(cfg.get(ConfigurationKey.DEVICE_REPORT_BSSID_URL_MASK), data);
                logText = urlAddress;
            }


            NetworkReport networkReport = new NetworkReport();
            networkReport.execute(urlAddress);
            FLogger.getInstance().log(this.getClass(), "onEvent() networkReport.execute(urlAddress);");


        } catch (Exception e) {
            FLogger.getInstance().log(this.getClass(), "Cannot send data: " + logText);
            FLogger.getInstance().log(this.getClass(), "----> " + e.getMessage());
            FLogger.getInstance().log(this.getClass(), Utils.stToString(e.getStackTrace()));

        }
    }
}
