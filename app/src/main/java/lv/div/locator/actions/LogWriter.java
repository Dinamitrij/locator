package lv.div.locator.actions;

import android.os.AsyncTask;

import lv.div.locator.utils.FLogger;

public class LogWriter extends AsyncTask<String, Void, Boolean> {

    /**
     * Write logging data in background
     *
     * @param params
     * @return
     */
    @Override
    protected Boolean doInBackground(String... params) {
        try {
            FLogger.getInstance().appendLog(params[0]);
        } catch (Exception e) {
            //quiet
        }
        return true;
    }


}
