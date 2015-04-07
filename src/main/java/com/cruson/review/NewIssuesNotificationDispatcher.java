package com.cruson.review;

import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationDispatcher;
import org.sonar.api.notifications.NotificationDispatcherMetadata;

public class NewIssuesNotificationDispatcher extends NotificationDispatcher {

	public static final String KEY = "NewIssuesReview";
	public static final String ANONYMOUS = "anonymous";
	private final CrucibleNotificationChannel channel;

	public NewIssuesNotificationDispatcher(CrucibleNotificationChannel channel) {
		super(NotificationFields.NOTIFICATION_TYPE);
		this.channel = channel;
	}

	@Override
	public String getKey() {
		return KEY;
	}

	public static NotificationDispatcherMetadata newMetadata() {
		return NotificationDispatcherMetadata
				.create(KEY)
				.setProperty(
						NotificationDispatcherMetadata.GLOBAL_NOTIFICATION,
						String.valueOf(false))
				.setProperty(
						NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION,
						String.valueOf(false));
	}

	@Override
	public void dispatch(Notification notification, Context context) {
		context.addUser(ANONYMOUS, channel);
	}
}
