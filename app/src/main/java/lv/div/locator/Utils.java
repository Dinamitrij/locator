package lv.div.locator;

import java.util.Date;

public class Utils {

    public static boolean clockTicked(Date fromDate, Integer tickMsec) {
        Date now = new Date();
        if (now.getTime() < fromDate.getTime() + tickMsec) {
            return false;
            //This event/state is still alive. Do not overwrite it.
        } else {
            return true;
        }
    }

}
