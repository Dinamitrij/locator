package lv.div.locator.actions;


import android.os.AsyncTask;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import de.greenrobot.event.EventBus;
import lv.div.locator.Const;
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
            String gpsData = URLEncoder.encode(event.getGpsData(), Const.UTF8_ENCODING);

            String deviceId = "test1";

            String urlAddress = String.format(Const.REPORT_URL_MASK, batteryStatus, wifiData, gpsData, event.getAccuracy(), event.getSafe(), deviceId);


            NetworkReport networkReport = new NetworkReport();
            networkReport.execute(urlAddress);

//            url = new URL(urlAddress);
//
//            urlConnection = (HttpURLConnection) url.openConnection();
//            InputStream in = urlConnection.getInputStream();
//            InputStreamReader isw = new InputStreamReader(in);
//
//            int data = isw.read();
//            while (data != -1) {
//                char current = (char) data;
//                data = isw.read();
//            }
        } catch (Exception e) {
            // Error. Be quiet
            String message = e.getMessage();
        } finally {
//            try {
//                urlConnection.disconnect();
//            } catch (Exception e) {
//                // Error. Be quiet
//            }
        }


    }
}
