package lv.div.locator.utils;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import lv.div.locator.Main;
import lv.div.locator.Utils;
import lv.div.locator.actions.LogWriter;
import lv.div.locator.commons.conf.ConfigurationKey;
import lv.div.locator.commons.conf.Const;
import lv.div.locator.conf.Constant;

/**
 * Buffered file logger
 */
public class FLogger {
    private static FLogger ourInstance = new FLogger();
    private static StringBuffer stringBuffer = new StringBuffer();

    public static FLogger getInstance() {
        return ourInstance;
    }

    private FLogger() {
    }


    /**
     * Log data to the log file
     *
     * @param clazz
     * @param logtext
     */
    public static void log(Class clazz, String logtext) {
        logData(clazz, logtext, Constant.LOG_BUFFER_SIZE);
    }

    /**
     * Log data to the log file and force "flush" operation (max buffer size = 0).
     * Does the task in foreground
     *
     * @param clazz
     * @param logtext
     */
    public static void logAndFlush(Class clazz, String logtext) {
        logData(clazz, logtext, 0);
    }

    private static void logData(Class clazz, String logtext, int logBufferSize) {
        Map<ConfigurationKey, String> cfg = Main.getInstance().config;
        if (!Const.TRUE_FLAG.equals(cfg.get(ConfigurationKey.DEVICE_LOCAL_LOGGING_ENABLED))) {
            return; // Logging disabled
        }

        stringBuffer.append(Utils.logtime(clazz));
        stringBuffer.append(Const.SPACE);

        stringBuffer.append(logtext);
        stringBuffer.append('\n');

        int bufferSize = stringBuffer.length();
        if (bufferSize > logBufferSize) {
            String logData = stringBuffer.toString();

            if (logBufferSize > 0) { // Force flush?
                LogWriter logWriter = new LogWriter();
                logWriter.execute(logData);
            } else {
                appendLog(logData); // Log and flush
            }

            stringBuffer.setLength(0); // Clear buffer
        }
    }

    public static void appendLog(String text) {
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        File logFile = new File(externalStorageDirectory, "locator.txt");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                //quiet!
            }
        }

        BufferedWriter buf = null;
        try {
            buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.flush();
        } catch (IOException e) {
            //quiet
        } finally {
            try {
                buf.close();
            } catch (IOException e) {
                //quiet
            }
        }

    }


}
