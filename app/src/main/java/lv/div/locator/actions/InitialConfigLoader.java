package lv.div.locator.actions;

import lv.div.locator.Main;
import lv.div.locator.utils.FLogger;

public class InitialConfigLoader extends GenericConfigLoader {

    @Override
    protected void handleLoadException(String deviceId) {
        FLogger.getInstance().logAndFlush(this.getClass(), "Fatal ERROR! Cannot load configuration.");
        System.exit(1); /// THIS IS FATAL ERROR!!! WE NEED INTERNET AND CONFIGURATION ALWAYS!!!
    }

    @Override
    protected void onPostExecute(Void result) {
        Main.getInstance().startApplication();
    }
}
