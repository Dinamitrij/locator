package lv.div.locator;

import java.util.Date;
import java.util.List;

import lv.div.locator.events.EventType;

public class SMSEvent {


    private Date eventTime;
    private Integer eventTTLMsec;
    private EventType problemType;
    private List<String> phonesToAlert;
    private String alertMessage;
    private boolean smsSent = false;


    public Date getEventTime() {
        return eventTime;
    }

    public void setEventTime(Date eventTime) {
        this.eventTime = eventTime;
    }

    public Integer getEventTTLMsec() {
        return eventTTLMsec;
    }

    public void setEventTTLMsec(Integer eventTTLMsec) {
        this.eventTTLMsec = eventTTLMsec;
    }

    public EventType getProblemType() {
        return problemType;
    }

    public void setProblemType(EventType problemType) {
        this.problemType = problemType;
    }

    public List<String> getPhonesToAlert() {
        return phonesToAlert;
    }

    public void setPhonesToAlert(List<String> phonesToAlert) {
        this.phonesToAlert = phonesToAlert;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }

    public boolean isSmsSent() {
        return smsSent;
    }

    public void setSmsSent(boolean smsSent) {
        this.smsSent = smsSent;
    }
}
