package lv.div.locator.conf;

/**
 * Tasker-like system variables
 */
public enum SystemVariable {

    DATE("%DATE"),
    TIME("%TIME"),
    BATTERY_LEVEL("%BATT");


    private final String systemVariable;


    private SystemVariable(final String systemVariable) {
        this.systemVariable = systemVariable;
    }

    @Override
    public String toString() {
        return systemVariable;
    }


}
