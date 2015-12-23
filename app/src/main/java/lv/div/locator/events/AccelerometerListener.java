package lv.div.locator.events;

import android.hardware.SensorEvent;
import android.util.Log;

import java.util.Date;

import lv.div.locator.Main;
import lv.div.locator.commons.conf.Const;
import lv.div.locator.utils.FLogger;

/**
 * Accelerometer event receiver
 */
public class AccelerometerListener implements IAccelerometerListener {
    private static AccelerometerListener mInstance;

    public static AccelerometerListener getInstance() {
        if (mInstance == null) {
            mInstance = new AccelerometerListener();
        }
        return mInstance;
    }

    private AccelerometerListener() {
    }


    @Override
    public void onMotionDetected(SensorEvent event, float acceleration) {
        //                if (acceleration > 0.4f) {
        Main.getInstance().deviceWasMoved = true; // Device moved
        Main.getInstance().previousSafeZoneCall = Const.EMPTY;
        Main.getInstance().deviceMovedTime = new Date(); // ...now

//        Log.d("Acc", "AccelerometerListener::onMotionDetected(): acceleration detected - " + acceleration);
        FLogger.getInstance().log(this.getClass(), "AccelerometerListener::onMotionDetected(): acceleration detected - " + acceleration);

    }
}
