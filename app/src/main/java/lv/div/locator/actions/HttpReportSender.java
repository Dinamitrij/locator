package lv.div.locator.actions;


import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import de.greenrobot.event.EventBus;
import lv.div.locator.Const;
import lv.div.locator.Main;
import lv.div.locator.conf.ConfigurationKey;
import lv.div.locator.events.EventHttpReport;

public class HttpReportSender {

    private EventBus bus = EventBus.getDefault();
    private URL url;
    private HttpURLConnection urlConnection;

    public HttpReportSender() {
        bus.register(this);
        urlConnection = null;
    }

    public void onEvent(EventHttpReport event) {

        try {
            String batteryStatus = URLEncoder.encode(event.getBatteryStatus(), Const.UTF8_ENCODING);
            String wifiData = URLEncoder.encode(event.getWifiData(), Const.UTF8_ENCODING);
            String latitude = URLEncoder.encode(event.getLatitude(), Const.UTF8_ENCODING);
            String longitude = URLEncoder.encode(event.getLongitude(), Const.UTF8_ENCODING);

            String deviceId = event.getDeviceId();

            Map<ConfigurationKey, String> cfg = Main.getInstance().config;
            String urlAddress = String.format(cfg.get(ConfigurationKey.DEVICE_REPORT_URL_MASK), latitude, longitude, wifiData, event.getAccuracy(), event.getSafe(), batteryStatus, event.getSpeed(), deviceId);

            NetworkReport networkReport = new NetworkReport();
            networkReport.execute(urlAddress);

        } catch (Exception e) {
            // Error. Be quiet
        }

    }
}
