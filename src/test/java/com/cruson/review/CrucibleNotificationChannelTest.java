package com.cruson.review;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.config.Settings;
import org.sonar.api.notifications.Notification;

@RunWith(MockitoJUnitRunner.class)
public class CrucibleNotificationChannelTest {
	@Mock
	private CrucibleApi api;

	private CrucibleNotificationChannel channel;

	private Notification notification;

	private Settings settings;

	@Before
	public void setUp() throws Exception {
		Map<String, String> properties = new HashMap<>();
		properties.put(CruSonPlugin.CRUSON_HOST_URL, "http://cnx:8060");
		properties.put(CruSonPlugin.CRUSON_HOST_USER, "test");
		properties.put(CruSonPlugin.CRUSON_HOST_PASSWORD, "1234");
		properties.put(CruSonPlugin.CRUSON_PROJECT, "test");
		properties.put(CruSonPlugin.CRUSON_REPOSITORY, "YuraTest");
		settings = new Settings();
		settings.setProperties(properties);

		channel = new CrucibleNotificationChannel(settings, api);
		channel = spy(channel);

		notification = new Notification(NotificationFields.NOTIFICATION_TYPE)
				.setFieldValue(NotificationFields.PROJECT_KEY, "PROJECT_KEY")
				.setFieldValue(NotificationFields.COMPONENT_PATH,
						"PROJECT_KEY:src/main/java/sonar/git/gitest/TestGit.java")
				.setFieldValue(NotificationFields.SEVERITY, "SEVERITY")
				.setFieldValue(NotificationFields.SCM_AUTHOR, "yura@")
				.setFieldValue(NotificationFields.LINE, "9")
				.setFieldValue(NotificationFields.SCM_DATE, "SCM_DATE")
				.setFieldValue(NotificationFields.SCM_REVISION,
						"6f67f322e7fb490cc8ee116b21fec64af97d792e")
				.setFieldValue(NotificationFields.SCM_REVISION_LAST,
						"6f67f322e7fb490cc8ee116b21fec64af97d7921")
				.setFieldValue(NotificationFields.RULE_KEY, "squid:S106")
				.setFieldValue(NotificationFields.MESSAGE, "MESSAGE");
	}

	@Test
	public void testDeliver() throws Exception {
		doThrow(new Exception()).when(channel).createReview(
				new Notification(""));
	}

	@Test
	public void testCreateReview() throws Exception {
		String reviewId = "reviewId";
		String itemId = "itemId";

		when(api.isUserExist("yura")).thenReturn(true);
		when(
				api.createReview(eq("test"), eq("MESSAGE"),
						Mockito.anyString(), eq("test"))).thenReturn(reviewId);
		doNothing().when(api).addReviewer(reviewId, "yura");

		when(
				api.addReviewItem(reviewId,
						"PROJECT_KEY:src/main/java/sonar/git/gitest/TestGit.java",
						"6f67f322e7fb490cc8ee116b21fec64af97d7921"))
				.thenReturn(itemId);

		doNothing().when(api).addReviewComment(eq(reviewId), eq(itemId),
				anyString(), eq("9"));

		doNothing().when(api).startReview(reviewId);

		channel.createReview(notification);

		verify(api).isUserExist("yura");
		verify(api).createReview(eq("test"), eq("MESSAGE"),
				Mockito.anyString(), eq("test"));
		verify(api).addReviewer(reviewId, "yura");
		verify(api).addReviewItem(reviewId,
				"PROJECT_KEY:src/main/java/sonar/git/gitest/TestGit.java",
				"6f67f322e7fb490cc8ee116b21fec64af97d7921");
		verify(api).addReviewComment(eq(reviewId), eq(itemId), anyString(),
				eq("9"));
		verify(api).startReview(reviewId);
		verifyNoMoreInteractions(api);
	}

	@Test
	@Ignore
	public void testMakeReviewData() {
		Notification notification = new Notification(
				NotificationFields.NOTIFICATION_TYPE)
				.setFieldValue(NotificationFields.PROJECT_KEY, "PROJECT_KEY")
				.setFieldValue(NotificationFields.COMPONENT_PATH,
						"PROJECT_KEY:module2:src/main/java/sonar/git/gitest/module2/TestGit2.java")
				.setFieldValue(NotificationFields.SEVERITY, "SEVERITY")
				.setFieldValue(NotificationFields.SCM_AUTHOR, "yura@")
				.setFieldValue(NotificationFields.LINE, "9")
				.setFieldValue(NotificationFields.SCM_DATE, "SCM_DATE")
				.setFieldValue(NotificationFields.SCM_REVISION,
						"6f67f322e7fb490cc8ee116b21fec64af97d792e")
				.setFieldValue(NotificationFields.RULE_KEY, "squid:S106")
				.setFieldValue(NotificationFields.MESSAGE, "MESSAGE");
		channel = new CrucibleNotificationChannel(settings,
				new CrucibleApiImpl(settings, new HttpDownloadImpl()));
		channel.deliver(notification, null);
	}

	@Test
	@Ignore
	public void testCloseAllReview() throws Exception {
		// channel.closeAllRereview();
	}
}
