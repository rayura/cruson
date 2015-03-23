package com.cruson.review;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;

public class CrucibleNotificationChannel extends NotificationChannel {

	private static final Logger LOG = LoggerFactory
			.getLogger(CrucibleNotificationChannel.class);

	private Settings settings;
	private CrucibleApi api;

	public CrucibleNotificationChannel(Settings settings, CrucibleApi api) {
		this.settings = settings;
		this.api = api;
	}

	@Override
	public void deliver(Notification notification, String userlogin) {
		try {
			createReview(notification);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	protected void createReview(Notification notification) throws Exception {
		boolean authorExist = api.isUserExist(NotificationFields
				.getAuthorName(notification
						.getFieldValue(NotificationFields.SCM_AUTHOR)));

		String reviewId = api.createReview(
				settings.getString(CruSonPlugin.CRUSON_PROJECT),
				notification.getFieldValue(NotificationFields.MESSAGE),
				makeDescription(notification),
				settings.getString(CruSonPlugin.CRUSON_HOST_USER));

		if (authorExist) {
			api.addReviewer(reviewId, NotificationFields
					.getAuthorName(notification
							.getFieldValue(NotificationFields.SCM_AUTHOR)));
		}

		String file = NotificationFields.getFile(
				notification.getFieldValue(NotificationFields.COMPONENT_KEY),
				notification.getFieldValue(NotificationFields.PROJECT_KEY));
		String itemId = api.addReviewItem(reviewId, file,
				notification.getFieldValue(NotificationFields.SCM_REVISION));

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
				notification.getFieldValue(NotificationFields.COMPONENT_KEY),
				notification.getFieldValue(NotificationFields.LINE),
				notification.getFieldValue(NotificationFields.SCM_REVISION),
				notification.getFieldValue(NotificationFields.SCM_DATE));
	}

	protected String makeRuleLink(Notification notification) throws Exception {
		return "[rule|http://nemo.sonarqube.org/coding_rules#rule_key%3D"
				+ notification.getFieldValue(NotificationFields.RULE_KEY) + "]";
	}
}
