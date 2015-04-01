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
import org.sonar.api.batch.DecoratorContext;
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
public class NewIssueResourceDecoratorTest {

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
	private DecoratorContext context;

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

	private NewIssueResourceDecorator decorator;

	@Before
	public void setUp() {
		Map<String, String> properties = new HashMap<>();
		properties.put(CruSonPlugin.CRUSON_SEVERITY, "MAJOR");
		Settings settings = new Settings();
		settings.setProperties(properties);

		decorator = new NewIssueResourceDecorator(settings, issueCache, manager);
		decorator = Mockito.spy(decorator);
		dateMeasure = new Measure<>(
				CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE, TEST_DATES);

		project.setPath("module");
	}

	@Test
	public void testExecuteOn() throws Exception {
		List<DefaultIssue> issues = Arrays.asList(new DefaultIssue[] { issue,
				issue1 });
		String key = "key";
		when(resource.getEffectiveKey()).thenReturn(key);
		when(issueCache.byComponent(key)).thenReturn(issues);
		when(decorator.supports(issue)).thenReturn(false);
		when(decorator.supports(issue1)).thenReturn(true);
		Notification notification = new Notification("");
		doReturn(notification).when(decorator).createNotification(context,
				issue1);
		doNothing().when(manager).scheduleForSending(eq(notification));

		decorator.decorate(resource, context);

		verify(manager).scheduleForSending(eq(notification));
	}

	@Test
	public void testCreateNotification() throws Exception {
		Integer line = new Integer(5);
		Integer lastLine = new Integer(2);
		String componentPath = "componentPath";

		when(context.getProject()).thenReturn(project);
		when(context.getResource()).thenReturn(resource);
		when(resource.getPath()).thenReturn(componentPath);
		when(issue.severity()).thenReturn(NotificationFields.SEVERITY);
		when(issue.message()).thenReturn(NotificationFields.MESSAGE);
		when(issue.ruleKey()).thenReturn(
				RuleKey.of(NotificationFields.PROJECT_KEY,
						NotificationFields.RULE_KEY));
		when(issue.line()).thenReturn(line);

		doReturn(lastLine).when(decorator).getLastRevisionLine(context);

		doReturn(NotificationFields.SCM_AUTHOR)
				.when(decorator)
				.getResourceData(context, CoreMetrics.SCM_AUTHORS_BY_LINE, line);
		doReturn(NotificationFields.SCM_DATE).when(decorator).getResourceData(
				context, CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE, line);
		doReturn(NotificationFields.SCM_REVISION).when(decorator)
				.getResourceData(context, CoreMetrics.SCM_REVISIONS_BY_LINE,
						line);
		doReturn(NotificationFields.SCM_REVISION_LAST).when(decorator)
				.getResourceData(context, CoreMetrics.SCM_REVISIONS_BY_LINE,
						lastLine);

		Notification notification = decorator
				.createNotification(context, issue);

		Assert.assertEquals(NotificationFields.NOTIFICATION_TYPE,
				notification.getType());
		Assert.assertEquals(NotificationFields.PROJECT_KEY,
				notification.getFieldValue(NotificationFields.PROJECT_KEY));
		Assert.assertEquals(project.getPath() + "/" + componentPath,
				notification.getFieldValue(NotificationFields.COMPONENT_PATH));
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
		Assert.assertEquals(NotificationFields.SCM_REVISION_LAST, notification
				.getFieldValue(NotificationFields.SCM_REVISION_LAST));
	}

	@Test
	public void testCreateNotificationNullLine() throws Exception {
		Integer line = new Integer(5);
		Integer lastLine = new Integer(2);
		String componentPath = "componentPath";

		when(context.getProject()).thenReturn(project);
		when(context.getResource()).thenReturn(resource);
		when(resource.getPath()).thenReturn(componentPath);
		when(issue.severity()).thenReturn(NotificationFields.SEVERITY);
		when(issue.message()).thenReturn(NotificationFields.MESSAGE);
		when(issue.ruleKey()).thenReturn(
				RuleKey.of(NotificationFields.PROJECT_KEY,
						NotificationFields.RULE_KEY));
		when(issue.line()).thenReturn(null);

		doReturn(lastLine).when(decorator).getLastRevisionLine(context);

		doReturn(NotificationFields.SCM_AUTHOR)
				.when(decorator)
				.getResourceData(context, CoreMetrics.SCM_AUTHORS_BY_LINE, lastLine);
		doReturn(NotificationFields.SCM_DATE).when(decorator).getResourceData(
				context, CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE, lastLine);
		doReturn(NotificationFields.SCM_REVISION).when(decorator)
				.getResourceData(context, CoreMetrics.SCM_REVISIONS_BY_LINE,
						lastLine);
		doReturn(NotificationFields.SCM_REVISION_LAST).when(decorator)
				.getResourceData(context, CoreMetrics.SCM_REVISIONS_BY_LINE,
						lastLine);

		Notification notification = decorator
				.createNotification(context, issue);

		Assert.assertEquals(NotificationFields.NOTIFICATION_TYPE,
				notification.getType());
		Assert.assertEquals(NotificationFields.PROJECT_KEY,
				notification.getFieldValue(NotificationFields.PROJECT_KEY));
		Assert.assertEquals(project.getPath() + "/" + componentPath,
				notification.getFieldValue(NotificationFields.COMPONENT_PATH));
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
		Assert.assertEquals(NotificationFields.SCM_REVISION_LAST,
				notification.getFieldValue(NotificationFields.SCM_REVISION));
		Assert.assertEquals(NotificationFields.SCM_REVISION_LAST, notification
				.getFieldValue(NotificationFields.SCM_REVISION_LAST));
	}

	@Test
	public void testSupports() throws Exception {
		when(issue.severity()).thenReturn("");
		when(decorator.supportsSeverity("")).thenReturn(false);
		when(decorator.isPluginEnabled()).thenReturn(false);
		Assert.assertFalse(decorator.supports(issue));

		when(decorator.supportsSeverity("")).thenReturn(true);
		Assert.assertFalse(decorator.supports(issue));

		when(issue.isNew()).thenReturn(true);
		when(issue.resolution()).thenReturn(" ");
		Assert.assertFalse(decorator.supports(issue));

		when(decorator.isPluginEnabled()).thenReturn(true);
		Assert.assertFalse(decorator.supports(issue));

		when(issue.resolution()).thenReturn(null);
		Assert.assertTrue(decorator.supports(issue));
	}

	@Test
	public void testSupportsSeverity() throws Exception {
		Assert.assertFalse(decorator.supportsSeverity("INFO"));
		Assert.assertFalse(decorator.supportsSeverity("MINOR"));
		Assert.assertTrue(decorator.supportsSeverity("MAJOR"));
		Assert.assertTrue(decorator.supportsSeverity("CRITICAL"));
		Assert.assertTrue(decorator.supportsSeverity("BLOCKER"));
		Assert.assertFalse(decorator.supportsSeverity("BAD"));
	}

	@Test
	public void testGetResourceData() throws Exception {
		when(context.getMeasure(CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE))
				.thenReturn(dateMeasure);
		Assert.assertEquals("2015-03-12T12:30:29+0200", decorator
				.getResourceData(context,
						CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE, 9));

	}

	@Test
	public void testGetResourceDataNull() throws Exception {
		when(context.getMeasure(CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE))
				.thenReturn(null);
		Assert.assertNull(decorator.getResourceData(context,
				CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE, 9));

	}

	@Test
	public void testGetLastRevision() throws Exception {
		when(context.getMeasure(CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE))
				.thenReturn(dateMeasure);
		Assert.assertEquals(new Integer(9),
				decorator.getLastRevisionLine(context));

	}
}
