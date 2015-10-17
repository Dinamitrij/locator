package lv.div.locator.actions;


import android.location.Location;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import de.greenrobot.event.EventBus;
import lv.div.locator.Const;
import lv.div.locator.events.EventHttpReport;
import lv.div.locator.events.LocationEvent;

public class LocationReportSender {

    private EventBus bus = EventBus.getDefault();
    private URL url;
    private HttpURLConnection urlConnection;

    public LocationReportSender() {
        bus.register(this);
        urlConnection = null;
    }

    public void onEvent(LocationEvent event) {

        try {
//            String batteryStatus = URLEncoder.encode(event.getBatteryStatus(), Const.UTF8_ENCODING);
//            String wifiData = URLEncoder.encode(event.getWifiData(), Const.UTF8_ENCODING);
//            String gpsData = URLEncoder.encode(event.getGpsData(), Const.UTF8_ENCODING);

            Location loc = event.getLoc();
            String lastGpsStatus = loc.getLongitude() + " # " + loc.getLatitude();

            String gpsData = URLEncoder.encode(lastGpsStatus, Const.UTF8_ENCODING);

            String urlAddress = String.format(Const.REPORT_URL_MASK, "", "", gpsData);
            url = new URL(urlAddress);

            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = urlConnection.getInputStream();
            InputStreamReader isw = new InputStreamReader(in);

            int data = isw.read();
            while (data != -1) {
                char current = (char) data;
                data = isw.read();
            }
        } catch (Exception e) {
            // Error. Be quiet
        } finally {
            try {
                urlConnection.disconnect();
            } catch (Exception e) {
                // Error. Be quiet
            }
        }


    }
}
