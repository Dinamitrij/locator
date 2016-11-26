package lv.div.locator.actions;

import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import lv.div.locator.Utils;
import lv.div.locator.utils.FLogger;

public class NetworkDataReport extends AsyncTask<String, Void, Boolean>   {

    protected HttpURLConnection urlConnection;

    protected Boolean doInBackground(String... params) {
        FLogger.getInstance().log(this.getClass(), "NDR doInBackground() called");
        StringBuffer sb = new StringBuffer();
        try {
            URL url = new URL(params[0]);

            urlConnection = (HttpURLConnection) url.openConnection();

            InputStream in = urlConnection.getInputStream();
            InputStreamReader isw = new InputStreamReader(in);

            int data = isw.read();
            while (data != -1) {
                char current = (char) data;
                sb.append(current);
                data = isw.read();
            }


        } catch (Exception e) {
            FLogger.getInstance().log(this.getClass(), "NDR doInBackground() cannot send/receive data");
            FLogger.getInstance().log(this.getClass(), "NDR ----> " + e.getMessage());
            FLogger.getInstance().log(this.getClass(), Utils.stToString(e.getStackTrace()));
        } finally {
            try {
                urlConnection.disconnect();
            } catch (Exception e) {
                FLogger.getInstance().log(this.getClass(), "NDR doInBackground() cannot urlConnection.disconnect();");
                FLogger.getInstance().log(this.getClass(), "NDR ----> " + e.getMessage());
                FLogger.getInstance().log(this.getClass(), Utils.stToString(e.getStackTrace()));
            }
        }

        return true;
    }
}
