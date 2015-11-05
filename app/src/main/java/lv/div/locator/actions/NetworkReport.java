package lv.div.locator.actions;

import android.os.AsyncTask;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetworkReport extends AsyncTask<String, Void, Boolean> {

    protected HttpURLConnection urlConnection;

    @Override
    protected Boolean doInBackground(String... params) {

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
            // Error. Be quiet
            String message = e.getMessage();
        } finally {
            try {
                urlConnection.disconnect();
            } catch (Exception e) {
                // Error. Be quiet
            }
        }


        return true;
    }
}
