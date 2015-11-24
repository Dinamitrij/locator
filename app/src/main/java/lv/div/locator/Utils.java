package lv.div.locator;

import java.text.SimpleDateFormat;
import java.util.Date;

import lv.div.locator.commons.conf.Const;
import lv.div.locator.conf.SystemVariable;

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

    public static String currentDate() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Const.DATE_FORMAT);
        return simpleDateFormat.format(new Date());
    }

    public static String currentTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Const.TIME_24H_FORMAT);
        return simpleDateFormat.format(new Date());
    }

    public static String logtime(Class clazz) {
        return currentDate()+Const.SPACE+currentTime()+Const.SPACE+clazz.getSimpleName()+Const.SPACE;
    }

    public static String stToString(StackTraceElement[] stackTraceElements) {
        if (stackTraceElements == null) {
            return Const.EMPTY;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (StackTraceElement element : stackTraceElements)
            stringBuilder.append(element.toString()).append("\n");
        return stringBuilder.toString();
    }


    public static String fillPlaceholdersWithSystemVariables(String sourceMessage) {
        String result = sourceMessage;
        for (SystemVariable s : SystemVariable.values()) {
            if (result.indexOf(s.toString()) >= 0) {
                if (SystemVariable.DATE.equals(s)) {
                    result = result.replaceAll(s.toString(), Utils.currentDate());
                } else if (SystemVariable.TIME.equals(s)) {
                    result = result.replaceAll(s.toString(), Utils.currentTime());
                } else if (SystemVariable.BATTERY_LEVEL.equals(s)) {
                    result = result.replaceAll(s.toString(), Main.getInstance().getBatteryStatus());
                }
            }
        }
        return result;
    }


}
