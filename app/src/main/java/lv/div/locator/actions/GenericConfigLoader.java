package lv.div.locator.actions;


import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import lv.div.locator.Main;
import lv.div.locator.commons.conf.ConfigurationKey;
import lv.div.locator.commons.conf.Const;
import lv.div.locator.conf.Constant;
import lv.div.locator.utils.FLogger;

import static android.R.attr.password;


public abstract class GenericConfigLoader extends AsyncTask<Void, Void, Void> {
    /**
     * Filename to store config data
     */
    public static final String CONFIG_FILE_NAME = "/locator.cfg";

    private HttpURLConnection urlConnection;

    @Override
    protected void onPreExecute() {
    }

    protected URL buildURL(String deviceId) throws Exception {
        String address = String.format(Constant.CONFIG_DOWNLOAD_URL_MASK, deviceId);
//        String query = URLEncoder.encode("apples oranges", "utf-8");
//        String url = "https://stackoverflow.com/search?q=" + query;


        return new URL(address);
//        return new URL("http://fe7c3f09.ngrok.io/config/config?device=00000000-5a30-9710-a81e-80c80033c587&text=1");
    }


    @Override
    protected Void doInBackground(Void... params) {
        String deviceId = Main.getInstance().buildDeviceId();

        try {

            URL url = buildURL(deviceId);

            urlConnection = (HttpURLConnection) url.openConnection();

            InputStream in = urlConnection.getInputStream();
            InputStreamReader isw = new InputStreamReader(in);

            String str;
            StringBuilder sb = new StringBuilder();
            int data = isw.read();
            while (data != -1) {
                char current = (char) data;
                sb.append(current);
                data = isw.read();
            }

            str = sb.toString();

            parseConfigurationFromString(str);
            writeToFile(str, Main.getInstance().getBaseContext());

            adjustConfigForDevMode();

        } catch (Exception e) {

            try {
                int status = urlConnection.getResponseCode();
                String configStr = readFromFile(Main.getInstance().getBaseContext());
                parseConfigurationFromString(configStr);
            } catch (Exception e1) {
                handleLoadException(deviceId);
            }



        } finally {
            try {
                urlConnection.disconnect();
            } catch (Exception e) {
            }
        }


        return null;
    }

    private void parseConfigurationFromString(String str) {
        Map<ConfigurationKey, String> tmpConfig = new HashMap<>();
        String[] split = str.split("\n");
        for (String line : split) {
            String[] keyVal = line.split(" = ");
            if (keyVal.length == 2) {
                String name = keyVal[0];
                ConfigurationKey key = ConfigurationKey.valueOf(name);
                String value = keyVal[1];
                tmpConfig.put(key, value);
            }
        }

        // If we need BSSIDs, then GPS should always be "ON":
        if (Const.TRUE_FLAG.equals(tmpConfig.get(ConfigurationKey.DEVICE_COLLECT_BSSID_MODE_ENABLED))) {
            tmpConfig.put(ConfigurationKey.SAFE_ZONE_WIFI, "nozone");
        }

        Main.getInstance().config.clear();
        Main.getInstance().config = tmpConfig;
    }

    /**
     * If we're in DEV mode (file "locatordev.ini" is present), edit Locator server engine URLs
     */
    private void adjustConfigForDevMode() {
        File devModeFlag = FLogger.buildFilePointer("locatordev.ini");
        if (devModeFlag.exists()) {
            Map<ConfigurationKey, String> cfg = Main.getInstance().config;
            for (ConfigurationKey key : cfg.keySet()) {
                String configStringValue = cfg.get(key);
                cfg.put(key, configStringValue.replace("/locator.", "/locatordev."));
            }
        }
    }

    /**
     * Exception, while loading configuration.
     *
     * @param deviceId
     */
    protected abstract void handleLoadException(String deviceId);

    protected abstract void onPostExecute(Void result);


    private void writeToFile(String data, Context context) throws IOException {
        File cfgFile = new File(Environment.getExternalStorageDirectory(), CONFIG_FILE_NAME);

//        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(CONFIG_FILE_NAME, Context.MODE_PRIVATE));
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(cfgFile));
        outputStreamWriter.write(data);
        outputStreamWriter.close();
    }


    private String readFromFile(Context context) throws FileNotFoundException, IOException {
        String ret = "";
        File cfgFile = new File(Environment.getExternalStorageDirectory(), CONFIG_FILE_NAME);
//        InputStream inputStream = context.openFileInput(CONFIG_FILE_NAME);
        InputStream inputStream = new FileInputStream(cfgFile);

        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString = "";
            StringBuilder stringBuilder = new StringBuilder();

            while ((receiveString = bufferedReader.readLine()) != null) {
                stringBuilder.append(receiveString);
            }

            inputStream.close();
            ret = stringBuilder.toString();
        }

        return ret;
    }

//    public SecretKey generateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
//        String s = Main.buildDeviceId();
//        return new SecretKeySpec(s, "AES");
//    }

//    public byte[] encryptMsg(String message, SecretKey secret)
//            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidParameterSpecException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
//   /* Encrypt the message. */
//        Cipher cipher = null;
//        cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
//        cipher.init(Cipher.ENCRYPT_MODE, secret);
//        byte[] cipherText = cipher.doFinal(message.getBytes("UTF-8"));
//        return cipherText;
//    }
//
//    public String decryptMsg(byte[] cipherText, SecretKey secret)
//            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidParameterSpecException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
//        Cipher cipher = null;
//        cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
//        cipher.init(Cipher.DECRYPT_MODE, secret);
//        String decryptString = new String(cipher.doFinal(cipherText), "UTF-8");
//        return decryptString;
//    }

}


