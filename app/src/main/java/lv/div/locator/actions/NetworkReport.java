package lv.div.locator.actions;

import android.os.AsyncTask;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import lv.div.locator.Utils;
import lv.div.locator.utils.FLogger;

public class NetworkReport extends AsyncTask<String, Void, Boolean> {

    protected HttpURLConnection urlConnection;

    @Override
    protected Boolean doInBackground(String... params) {
        FLogger.getInstance().log(this.getClass(), "doInBackground() called");
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
            FLogger.getInstance().log(this.getClass(), "doInBackground() cannot send/receive data");
            FLogger.getInstance().log(this.getClass(), "----> " + e.getMessage());
            FLogger.getInstance().log(this.getClass(), Utils.stToString(e.getStackTrace()));
        } finally {
            try {
                urlConnection.disconnect();
            } catch (Exception e) {
                FLogger.getInstance().log(this.getClass(), "doInBackground() cannot urlConnection.disconnect();");
                FLogger.getInstance().log(this.getClass(), "----> " + e.getMessage());
                FLogger.getInstance().log(this.getClass(), Utils.stToString(e.getStackTrace()));
            }
        }


        return true;
    }
}
