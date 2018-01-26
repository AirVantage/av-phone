package com.sierrawireless.avphone.task;

import java.io.IOException;

import net.airvantage.model.AirVantageException;
import net.airvantage.model.alert.v1.AlertRule;
import net.airvantage.utils.IAirVantageClient;

import com.sierrawireless.avphone.model.AvPhoneApplication;

public class AlertRuleClient implements IAlertRuleClient {

    private IAirVantageClient client;

    AlertRuleClient(IAirVantageClient client) {
        this.client = client;
    }
    
    @Override
    public AlertRule getAlertRule(String serialNumber) throws IOException, AirVantageException {
        String alertRuleName = AvPhoneApplication.ALERT_RULE_NAME;
        return client.getAlertRuleByName(alertRuleName);
    }
   
    @Override
    public void createAlertRule() throws IOException, AirVantageException {
        AlertRule alertRule = AvPhoneApplication.createAlertRule();
        client.createAlertRule(alertRule);
    }

    
}
