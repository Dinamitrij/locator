package lv.div.locator.events;

import android.location.Location;

public class LocationEvent {

    private Location loc;

    public LocationEvent(Location loc) {

        this.loc = loc;
    }

    public Location getLoc() {
        return loc;
    }

    public void setLoc(Location loc) {
        this.loc = loc;
    }
}
