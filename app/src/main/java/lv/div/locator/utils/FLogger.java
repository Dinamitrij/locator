package lv.div.locator.utils;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
        // Do not log anything, if shutting down process initiated
        if (!Main.getInstance().shuttingDown) {
            logData(clazz, logtext, Constant.LOG_BUFFER_SIZE);
        }
    }

    /**
     * Log data to the log file and force "flush" operation (max buffer size = 0).
     * Does the task in foreground
     *
     * @param clazz
     * @param logtext
     */
    public static void logAndFlush(Class clazz, String logtext) {
        // Do not log anything, if shutting down process initiated
        if (!Main.getInstance().shuttingDown) {
            logData(clazz, logtext, 0);
        }
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
        Map<ConfigurationKey, String> cfg = Main.getInstance().config;

        File externalStorageDirectory = Environment.getExternalStorageDirectory();
//        File logFile = null;

//        if (!Const.EMPTY.equals(cfg.get(ConfigurationKey.DEVICE_LOCAL_LOGGING_PATH))) {
        File logFile = new File(externalStorageDirectory, Main.getInstance().buildLogFileName());
//            File logFile = new File(new File(cfg.get(ConfigurationKey.DEVICE_LOCAL_LOGGING_PATH)), filename);
//        } else {
//            logFile = new File(externalStorageDirectory, filename);
//        }

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
