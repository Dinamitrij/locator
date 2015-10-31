package lv.div.locator.actions;

import android.os.AsyncTask;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import lv.div.locator.Main;
import lv.div.locator.conf.ConfigurationKey;

public class NetworkConfigurationLoader extends AsyncTask<String, Void, Boolean> {

    HttpURLConnection urlConnection;

    @Override
    protected Boolean doInBackground(String... params) {

        try {
            URL url = new URL(params[0]);

            urlConnection = (HttpURLConnection) url.openConnection();

            InputStream in = urlConnection.getInputStream();
            InputStreamReader isw = new InputStreamReader(in);


            Main.getInstance().config.clear();

            String str;
            StringBuilder sb = new StringBuilder();
            int data = isw.read();
            while (data != -1) {
                char current = (char) data;
                sb.append(current);
                data = isw.read();
            }

            str = sb.toString();
            String[] split = str.split("\n");
            for (String line : split) {
                String[] keyVal = line.split(" = ");
                Main.getInstance().config.put(ConfigurationKey.valueOf(keyVal[0]), keyVal[1]);
            }



        } catch (Exception e) {
            // Error. Be quiet

            System.exit(1); /// THIS IS FATAL ERROR!!! WE NEED INTERNET AND CONFIGURATION ALWAYS!!!

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
