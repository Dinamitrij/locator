package lv.div.locator.events;

import android.hardware.SensorEvent;

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
        Main.getInstance().deviceWasMoved = true; // Device moved
        Main.getInstance().accelerometerValue = Math.round(acceleration); // last movement value
        Main.getInstance().previousSafeZoneCall = Const.EMPTY;
        Main.getInstance().deviceMovedTime = new Date(); // ...now
        FLogger.getInstance().log(this.getClass(), "AccelerometerListener: acceleration = " + acceleration);
    }
}
