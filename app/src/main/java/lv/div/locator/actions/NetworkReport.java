package lv.div.locator.actions;

import android.os.AsyncTask;

import com.google.code.microlog4android.Logger;
import com.google.code.microlog4android.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import lv.div.locator.Utils;

public class NetworkReport extends AsyncTask<String, Void, Boolean> {

    protected HttpURLConnection urlConnection;
    private static final Logger log = LoggerFactory.getLogger();

    @Override
    protected Boolean doInBackground(String... params) {
        log.debug(Utils.logtime(this.getClass()) + "doInBackground() called");
        try {
            URL url = new URL(params[0]);

            urlConnection = (HttpURLConnection) url.openConnection();

            InputStream in = urlConnection.getInputStream();
            InputStreamReader isw = new InputStreamReader(in);

            int data = isw.read();
            while (data != -1) {
                char current = (char) data;
                data = isw.read();
            }
        } catch (Exception e) {
            log.debug(Utils.logtime(this.getClass())+"doInBackground() cannot send/receive data");
            log.debug(Utils.logtime(this.getClass()) + "----> "+e.getMessage());
            log.debug(Utils.logtime(this.getClass()) + Utils.stToString(e.getStackTrace()));
        } finally {
            try {
                urlConnection.disconnect();
            } catch (Exception e) {
                log.debug(Utils.logtime(this.getClass())+"doInBackground() cannot urlConnection.disconnect();");
                log.debug(Utils.logtime(this.getClass()) + "----> "+e.getMessage());
                log.debug(Utils.logtime(this.getClass()) + Utils.stToString(e.getStackTrace()));
            }
        }


        return true;
    }
}
