package com.sierrawireless.avphone.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.crashlytics.android.Crashlytics;

import android.util.Log;
import net.airvantage.model.AirVantageException;
import net.airvantage.model.User;
import net.airvantage.model.UserRights;
import net.airvantage.utils.IAirVantageClient;

public class UserClient implements IUserClient {

    private static final String LOGTAG = UserClient.class.getName();

    private IAirVantageClient client;

    public UserClient(IAirVantageClient client) {
        this.client = client;
    }

    @Override
    public List<String> checkRights() throws AirVantageException {

        List<String> requiredRights = new ArrayList<String>(Arrays.asList("entities.applications.view",
                "entities.applications.create", "entities.applications.edit", "entities.systems.view",
                "entities.systems.create", "entities.systems.edit", "entities.alerts.rule.view",
                "entities.alerts.rule.create.edit.delete"));

        try {
            UserRights rights = client.getUserRights();
            requiredRights.removeAll(rights);
        } catch (Exception e) {
            Crashlytics.logException(e);
            Log.e(LOGTAG, "Could not get user rights", e);
        }

        return requiredRights;

    }

    @Override
    public User getUser() {
        try {
            return this.client.getCurrentUser();
        } catch (final Exception e) {
            Crashlytics.logException(e);
            Log.e(LOGTAG, "Could not get user name", e);
            return null;
        }
    }
}
