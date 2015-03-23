package com.cruson.review;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.issue.IssueCache;

@RunWith(MockitoJUnitRunner.class)
public class NewIssuesPostJobTest {

	private static final String TEST_DATES = "1=2015-03-10T15:55:46+0200;"
			+ "2=2015-03-10T15:55:46+0200;" + "3=2015-03-10T15:55:46+0200;"
			+ "4=2015-03-10T15:55:46+0200;" + "5=2015-03-10T16:26:55+0200;"
			+ "6=2015-03-10T16:26:55+0200;" + "7=2015-03-11T13:37:52+0200;"
			+ "8=2015-03-10T16:26:55+0200;" + "9=2015-03-12T12:30:29+0200;"
			+ "10=2015-03-12T12:30:29+0200;" + "11=2015-03-12T12:30:29+0200;"
			+ "12=2015-03-12T12:30:29+0200;" + "13=2015-03-10T16:26:55+0200;"
			+ "14=2015-03-10T15:55:46+0200;" + "15=2015-03-10T15:55:46+0200;"
			+ "16=2015-03-10T15:55:46+0200";

	@Mock
	private SensorContext context;

	private Project project = new Project(NotificationFields.PROJECT_KEY);

	@Mock
	private Resource resource;

	@Mock
	private DefaultIssue issue;

	@Mock
	private DefaultIssue issue1;

	@Mock
	private IssueCache issueCache;

	@Mock
	private NotificationManager manager;

	private Measure<String> dateMeasure;

	private NewIssuesPostJob job;

	@Before
	public void setUp() {
		Map<String, String> properties = new HashMap<>();
		properties.put(CruSonPlugin.CRUSON_SEVERITY, "MAJOR");
		Settings settings = new Settings();
		settings.setProperties(properties);

		job = new NewIssuesPostJob(settings, issueCache, manager);
		job = Mockito.spy(job);
		dateMeasure = new Measure<>(
				CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE, TEST_DATES);

	}

	@Test
	public void testExecuteOn() throws Exception {
		List<DefaultIssue> issues = Arrays.asList(new DefaultIssue[] { issue,
				issue1 });
		when(issueCache.all()).thenReturn(issues);
		when(job.supports(issue)).thenReturn(false);
		when(job.supports(issue1)).thenReturn(true);
		Notification notification = new Notification("");
		doReturn(notification).when(job).createNotification(project, context,
				issue1);
		doNothing().when(manager).scheduleForSending(eq(notification));

		job.executeOn(project, context);

		verify(manager).scheduleForSending(eq(notification));
	}

	@Test
	public void testCreateNotification() throws Exception {
		Integer line = new Integer(5);
		when(issue.componentKey()).thenReturn(
				NotificationFields.PROJECT_KEY + ":"
						+ NotificationFields.COMPONENT_KEY);
		when(issue.severity()).thenReturn(NotificationFields.SEVERITY);
		when(issue.message()).thenReturn(NotificationFields.MESSAGE);
		when(issue.line()).thenReturn(line);
		when(issue.ruleKey()).thenReturn(
				RuleKey.of(NotificationFields.PROJECT_KEY,
						NotificationFields.RULE_KEY));
		when(context.getResource((Resource) notNull())).thenReturn(resource);
		doReturn(NotificationFields.SCM_AUTHOR).when(job).getResourceData(
				context, CoreMetrics.SCM_AUTHORS_BY_LINE, resource, line);
		doReturn(NotificationFields.SCM_DATE).when(job).getResourceData(
				context, CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE,
				resource, line);
		doReturn(NotificationFields.SCM_REVISION).when(job).getResourceData(
				context, CoreMetrics.SCM_REVISIONS_BY_LINE, resource, line);

		Notification notification = job.createNotification(project, context,
				issue);

		Assert.assertEquals(NotificationFields.NOTIFICATION_TYPE,
				notification.getType());
		Assert.assertEquals(NotificationFields.PROJECT_KEY,
				notification.getFieldValue(NotificationFields.PROJECT_KEY));
		Assert.assertEquals(NotificationFields.PROJECT_KEY + ":"
				+ NotificationFields.COMPONENT_KEY,
				notification.getFieldValue(NotificationFields.COMPONENT_KEY));
		Assert.assertEquals(NotificationFields.SEVERITY,
				notification.getFieldValue(NotificationFields.SEVERITY));
		Assert.assertEquals(NotificationFields.MESSAGE,
				notification.getFieldValue(NotificationFields.MESSAGE));
		Assert.assertEquals(line.toString(),
				notification.getFieldValue(NotificationFields.LINE));
		Assert.assertEquals(NotificationFields.PROJECT_KEY + ":"
				+ NotificationFields.RULE_KEY,
				notification.getFieldValue(NotificationFields.RULE_KEY));
		Assert.assertEquals(NotificationFields.SCM_AUTHOR,
				notification.getFieldValue(NotificationFields.SCM_AUTHOR));
		Assert.assertEquals(NotificationFields.SCM_DATE,
				notification.getFieldValue(NotificationFields.SCM_DATE));
		Assert.assertEquals(NotificationFields.SCM_REVISION,
				notification.getFieldValue(NotificationFields.SCM_REVISION));
	}

	@Test
	public void testCreateNotificationNullLine() throws Exception {
		Integer line = new Integer(6);
		when(issue.componentKey()).thenReturn(
				NotificationFields.PROJECT_KEY + ":"
						+ NotificationFields.COMPONENT_KEY);
		when(issue.severity()).thenReturn(NotificationFields.SEVERITY);
		when(issue.message()).thenReturn(NotificationFields.MESSAGE);
		when(issue.line()).thenReturn(null);
		when(issue.ruleKey()).thenReturn(
				RuleKey.of(NotificationFields.PROJECT_KEY,
						NotificationFields.RULE_KEY));
		when(context.getResource((Resource) notNull())).thenReturn(resource);
		doReturn(NotificationFields.SCM_AUTHOR).when(job).getResourceData(
				context, CoreMetrics.SCM_AUTHORS_BY_LINE, resource, line);
		doReturn(NotificationFields.SCM_DATE).when(job).getResourceData(
				context, CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE,
				resource, line);
		doReturn(NotificationFields.SCM_REVISION).when(job).getResourceData(
				context, CoreMetrics.SCM_REVISIONS_BY_LINE, resource, line);
		doReturn(line).when(job).getLastRevisionLine(context, resource);

		Notification notification = job.createNotification(project, context,
				issue);

		Assert.assertEquals(NotificationFields.NOTIFICATION_TYPE,
				notification.getType());
		Assert.assertEquals(NotificationFields.PROJECT_KEY,
				notification.getFieldValue(NotificationFields.PROJECT_KEY));
		Assert.assertEquals(NotificationFields.PROJECT_KEY + ":"
				+ NotificationFields.COMPONENT_KEY,
				notification.getFieldValue(NotificationFields.COMPONENT_KEY));
		Assert.assertEquals(NotificationFields.SEVERITY,
				notification.getFieldValue(NotificationFields.SEVERITY));
		Assert.assertEquals(NotificationFields.MESSAGE,
				notification.getFieldValue(NotificationFields.MESSAGE));
		Assert.assertEquals("",
				notification.getFieldValue(NotificationFields.LINE));
		Assert.assertEquals(NotificationFields.PROJECT_KEY + ":"
				+ NotificationFields.RULE_KEY,
				notification.getFieldValue(NotificationFields.RULE_KEY));
		Assert.assertEquals(NotificationFields.SCM_AUTHOR,
				notification.getFieldValue(NotificationFields.SCM_AUTHOR));
		Assert.assertEquals(NotificationFields.SCM_DATE,
				notification.getFieldValue(NotificationFields.SCM_DATE));
		Assert.assertEquals(NotificationFields.SCM_REVISION,
				notification.getFieldValue(NotificationFields.SCM_REVISION));
	}

	@Test
	public void testSupports() throws Exception {
		when(issue.severity()).thenReturn("");
		when(job.supportsSeverity("")).thenReturn(false);
		Assert.assertFalse(job.supports(issue));

		when(job.supportsSeverity("")).thenReturn(true);
		when(issue.isNew()).thenReturn(false);
		Assert.assertFalse(job.supports(issue));

		when(issue.isNew()).thenReturn(true);
		when(issue.resolution()).thenReturn(" ");
		Assert.assertFalse(job.supports(issue));

		when(issue.resolution()).thenReturn(null);
		Assert.assertTrue(job.supports(issue));
	}

	@Test
	public void testSupportsSeverity() throws Exception {
		Assert.assertFalse(job.supportsSeverity("INFO"));
		Assert.assertFalse(job.supportsSeverity("MINOR"));
		Assert.assertTrue(job.supportsSeverity("MAJOR"));
		Assert.assertTrue(job.supportsSeverity("CRITICAL"));
		Assert.assertTrue(job.supportsSeverity("BLOCKER"));
		Assert.assertFalse(job.supportsSeverity("BAD"));
	}

	@Test
	public void testGetResourceData() throws Exception {
		when(
				context.getMeasure(resource,
						CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE))
				.thenReturn(dateMeasure);
		Assert.assertEquals("2015-03-12T12:30:29+0200", job.getResourceData(
				context, CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE,
				resource, 9));

	}

	@Test
	public void testGetResourceDataNull() throws Exception {
		when(
				context.getMeasure(resource,
						CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE))
				.thenReturn(null);
		Assert.assertNull(job.getResourceData(context,
				CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE, resource, 9));

	}

	@Test
	public void testGetLastRevision() throws Exception {
		when(
				context.getMeasure(resource,
						CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE))
				.thenReturn(dateMeasure);
		Assert.assertEquals(new Integer(9),
				job.getLastRevisionLine(context, resource));

	}
}
