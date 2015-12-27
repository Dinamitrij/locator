package lv.div.locator.events;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.Date;
import java.util.Map;

import lv.div.locator.Main;
import lv.div.locator.Utils;
import lv.div.locator.commons.conf.ConfigurationKey;
import lv.div.locator.utils.FLogger;

public class MovementDetector implements SensorEventListener {

    /**
     * Timer threshold for low level accelerometer check. Used for filtering "noise" values.
     */
    public static final int LOW_LEVEL_SENSOR_CHECK_TIME_MSEC = 100;
    private SensorManager sensorMan;
    private Sensor accelerometer;
    private long lastUpdate;
    float last_x;
    float last_y;
    float last_z;

    private MovementDetector() {
    }

    private static MovementDetector mInstance;

    public static MovementDetector getInstance() {
        if (mInstance == null) {
            mInstance = new MovementDetector();
            mInstance.init();
        }
        return mInstance;
    }

    private IAccelerometerListener accelerometerListener;

    private void init() {
        sensorMan = (SensorManager) Main.getInstance().getBaseContext().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void start() {
        sensorMan.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void stop() {
        sensorMan.unregisterListener(this);
    }

    public void setListener(IAccelerometerListener listener) {
        accelerometerListener = listener;
    }

    /* (non-Javadoc)
     * @see android.hardware.SensorEventListener#onSensorChanged(android.hardware.SensorEvent)
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > LOW_LEVEL_SENSOR_CHECK_TIME_MSEC) {
                lastUpdate = curTime;

                // 200 & 10000 Just adjusted numbers for checking accelerometer values
                float sensorValue = Math.abs(x + y + z - last_x - last_y - last_z) / 200 * 10000;

                Map<ConfigurationKey, String> cfg = Main.getInstance().config;

                if (sensorValue > Integer.valueOf(cfg.get(ConfigurationKey.DEVICE_ACCELEROMETER_THRESHOLD)) &&
                        Utils.clockTicked(Main.getInstance().deviceMotionTimeout, Integer.valueOf(cfg.get(ConfigurationKey.DEVICE_ACCELEROMETER_CHECK_TIME_MSEC)))) {
                    Main.getInstance().deviceMotionTimeout = new Date();

                    FLogger.getInstance().log(this.getClass(), "onSensorChanged(): MOTION detected (once in 3 sec.)- " + sensorValue);
                    if (null != accelerometerListener) {
                        accelerometerListener.onMotionDetected(event, sensorValue);
                    } else {
                        FLogger.getInstance().log(this.getClass(), "onSensorChanged(): accelerometerListener was not set!");
                    }
                }
                last_x = x;
                last_y = y;
                last_z = z;
            }


        }

    }

    /* (non-Javadoc)
     * @see android.hardware.SensorEventListener#onAccuracyChanged(android.hardware.Sensor, int)
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

}