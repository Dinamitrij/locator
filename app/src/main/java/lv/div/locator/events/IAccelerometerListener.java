package lv.div.locator.events;


import android.hardware.SensorEvent;

public interface IAccelerometerListener {
    void onMotionDetected(SensorEvent event, float acceleration);
}
