package lv.div.locator.events;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.Date;
import java.util.HashSet;

import lv.div.locator.Main;
import lv.div.locator.Utils;
import lv.div.locator.commons.conf.ConfigurationKey;
import lv.div.locator.utils.FLogger;

public class MovementDetector implements SensorEventListener {

    protected final String TAG = getClass().getSimpleName();

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

    //////////////////////
//    private HashSet<Listener> mListeners = new HashSet<MovementDetector.Listener>();
    private IAccelerometerListener accelerometerListener;

    private void init() {
        sensorMan = (SensorManager) Main.getInstance().getBaseContext().getSystemService(Context.SENSOR_SERVICE);
//        accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
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

            if ((curTime - lastUpdate) > 100) {
//                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

//                float sensorValue = 0L;
//                float sensorValue = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;
                float sensorValue = Math.abs(x + y + z - last_x - last_y - last_z) / 200 * 10000;


//                Log.d(TAG, "Device motion detected - " + sensorValue);
                if (sensorValue > 20 && Utils.clockTicked(Main.getInstance().deviceMotionTimeout, 3000)) {
                    Main.getInstance().deviceMotionTimeout = new Date();

                    FLogger.getInstance().log(this.getClass(), "onSensorChanged(): MOTION detected (once in 3 sec.)- " + sensorValue);

//                Log.d(TAG, "Device motion detected!!!!");
//                    for (Listener listener : mListeners) {
//                        listener.onMotionDetected(event, sensorValue);
//                    }
                    if (null!=accelerometerListener) {
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