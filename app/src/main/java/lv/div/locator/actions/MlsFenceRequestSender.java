package lv.div.locator.actions;


import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Map;

import de.greenrobot.event.EventBus;
import lv.div.locator.Main;
import lv.div.locator.Utils;
import lv.div.locator.commons.conf.ConfigurationKey;
import lv.div.locator.commons.conf.Const;
import lv.div.locator.events.EventMlsFenceRequest;
import lv.div.locator.utils.FLogger;

public class MlsFenceRequestSender {

    private EventBus bus = EventBus.getDefault();
    private URL url;

    public MlsFenceRequestSender() {
        bus.register(this);
    }

    public void onEvent(EventMlsFenceRequest event) {
        String logText = Const.EMPTY;
        FLogger.getInstance().log(this.getClass(), "onEvent() called");

        if (!Main.getInstance().mlsFenceRecheckBusy) {
            processMlsRequestSending(event, logText);
        }
    }

    private synchronized void processMlsRequestSending(EventMlsFenceRequest event, String logText) {

        try {
            Main.getInstance().mlsFenceRecheckBusy = true;

            Map<ConfigurationKey, String> cfg = Main.getInstance().config;
            String urlAddress = String.format("http://locator.v1.lv/whereami?device=%s", Main.getInstance().buildDeviceId());
            logText = urlAddress;

            NetworkDataReport networkReport = new NetworkDataReport();
            networkReport.execute(urlAddress);
            FLogger.getInstance().log(this.getClass(), "onEvent() networkReport.execute(urlAddress);");
        } catch (Exception e) {
            FLogger.getInstance().log(this.getClass(), "Cannot send data: " + logText);
            FLogger.getInstance().log(this.getClass(), "----> " + e.getMessage());
            FLogger.getInstance().log(this.getClass(), Utils.stToString(e.getStackTrace()));
        } finally {
            Main.getInstance().mlsFenceRecheckBusy = false;
            Main.getInstance().mlsFenceRecheckDate = new Date();
        }



//        try {
//            Map<ConfigurationKey, String> cfg = Main.getInstance().config;
//            String urlAddress = Const.EMPTY;
//
//            String data = URLEncoder.encode(event.getRequestDate().toString(), Const.UTF8_ENCODING);
////            urlAddress = String.format(cfg.get(ConfigurationKey.DEVICE_REPORT_BSSID_URL_MASK), data);
//            urlAddress = String.format(cfg.get(ConfigurationKey.DEVICE_REPORT_BSSID_URL_MASK), data);
//            logText = urlAddress;
//
//            NetworkDataReport networkReport = new NetworkDataReport();
//            networkReport.execute(urlAddress);
//            FLogger.getInstance().log(this.getClass(), "onEvent() networkReport.execute(urlAddress);");
//        } catch (Exception e) {
//            FLogger.getInstance().log(this.getClass(), "Cannot send data: " + logText);
//            FLogger.getInstance().log(this.getClass(), "----> " + e.getMessage());
//            FLogger.getInstance().log(this.getClass(), Utils.stToString(e.getStackTrace()));
//
//        }
    }
}
