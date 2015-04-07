package com.cruson.review;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;

public class CrucibleNotificationChannel extends NotificationChannel {
    private static final Logger LOG = LoggerFactory
            .getLogger(CrucibleNotificationChannel.class);

    private final Settings globalSettings;
    private final PropertiesDao propertiesDao;
    private final HttpDownload httpDownload;

    public CrucibleNotificationChannel(Settings globalSettings,
            PropertiesDao propertiesDao, HttpDownload httpDownload) {
        this.globalSettings = globalSettings;
        this.propertiesDao = propertiesDao;
        this.httpDownload = httpDownload;
    }

    @Override
    public void deliver(Notification notification, String userlogin) {
        try {
            ProjectSettings settings = buildProjectSettings(notification
                    .getFieldValue(NotificationFields.PROJECT_KEY));
            CrucibleApi api = buildCrucibleApi(settings);

            createReview(settings, api, notification);
        } catch (Exception e) {
            LOG.error(
                    "on notification: " + notification + " error \n"
                            + e.getMessage(), e);
        }
    }

    protected void createReview(Settings settings, CrucibleApi api,
            Notification notification) throws IOException {
        boolean authorExist = api.isUserExist(NotificationFields
                .getAuthorName(notification
                        .getFieldValue(NotificationFields.SCM_AUTHOR)));

        String reviewId = api.createReview(
                settings.getString(CruSonPlugin.CRUSON_PROJECT),
                notification.getFieldValue(NotificationFields.MESSAGE),
                makeDescription(notification),
                settings.getString(CruSonPlugin.CRUSON_HOST_LOGIN));

        if (authorExist) {
            api.addReviewer(reviewId, NotificationFields
                    .getAuthorName(notification
                            .getFieldValue(NotificationFields.SCM_AUTHOR)));
        }

        String itemId = api.addReviewItem(settings
                .getString(CruSonPlugin.CRUSON_REPOSITORY), reviewId,
                notification.getFieldValue(NotificationFields.COMPONENT_PATH),
                notification
                        .getFieldValue(NotificationFields.SCM_REVISION_LAST));

        api.addReviewComment(reviewId, itemId,
                notification.getFieldValue(NotificationFields.MESSAGE) + " "
                        + makeRuleLink(notification),
                notification.getFieldValue(NotificationFields.LINE));

        api.startReview(reviewId);
    }

    protected String makeDescription(Notification notification) {
        return String.format(
                "New %s issue created by %s in %s at line %s commit %s at %s",
                notification.getFieldValue(NotificationFields.SEVERITY),
                notification.getFieldValue(NotificationFields.SCM_AUTHOR),
                notification.getFieldValue(NotificationFields.COMPONENT_PATH),
                notification.getFieldValue(NotificationFields.LINE),
                notification.getFieldValue(NotificationFields.SCM_REVISION),
                notification.getFieldValue(NotificationFields.SCM_DATE));
    }

    protected String makeRuleLink(Notification notification) {
        return "[rule|http://nemo.sonarqube.org/coding_rules#rule_key%3D"
                + notification.getFieldValue(NotificationFields.RULE_KEY) + "]";
    }

    public ProjectSettings buildProjectSettings(String projectKey) {
        List<PropertyDto> properties = propertiesDao
                .selectProjectProperties(projectKey);
        Map<String, String> prop = new HashMap<>();
        for (PropertyDto dto : properties) {
            prop.put(dto.getKey(), dto.getValue());
        }
        return new ProjectSettings(globalSettings, prop);
    }

    public CrucibleApi buildCrucibleApi(Settings settings) {
        CrucibleApiImpl api = new CrucibleApiImpl(httpDownload);
        api.setUrl(settings.getString(CruSonPlugin.CRUSON_HOST_URL));
        api.setLogin(settings.getString(CruSonPlugin.CRUSON_HOST_LOGIN));
        api.setPassword(settings.getString(CruSonPlugin.CRUSON_HOST_PASSWORD));
        return api;
    }
}
