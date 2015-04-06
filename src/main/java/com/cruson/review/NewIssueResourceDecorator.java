package com.cruson.review;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.issue.IssueCache;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

public class NewIssueResourceDecorator implements Decorator {
	private final static String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

	private final Settings settings;
	private final IssueCache issueCache;
	private final NotificationManager notificationsManager;

	@DependsUpon
	public Collection<Metric> usedMetrics() {
		return ImmutableList.<Metric> of(CoreMetrics.SCM_AUTHORS_BY_LINE,
				CoreMetrics.SCM_REVISIONS_BY_LINE,
				CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE,
				CoreMetrics.NEW_VIOLATIONS);
	}

	public NewIssueResourceDecorator(Settings settings, IssueCache issueCache,
			NotificationManager notificationsManager) {
		this.settings = settings;
		this.issueCache = issueCache;
		this.notificationsManager = notificationsManager;
	}

	public boolean shouldExecuteOnProject(Project project) {
		return true;
	}

	public void decorate(Resource resource, DecoratorContext context) {
		for (Issue issue : issueCache.byComponent(resource.getEffectiveKey())) {
			if (supports(issue)) {
				Notification notification = createNotification(context, issue);
				notificationsManager.scheduleForSending(notification);
			}
		}
	}

	protected boolean supports(Issue issue) {
		return isPluginEnabled() && supportsSeverity(issue.severity())
				&& issue.isNew() && issue.resolution() == null;
	}

	protected boolean isPluginEnabled() {
		return settings.getBoolean(CruSonPlugin.CRUSON_ENABLED);
	}

	protected boolean supportsSeverity(String severity) {
		String minSeverity = settings.getString(CruSonPlugin.CRUSON_SEVERITY)
				.toUpperCase();
		int index = Severity.ALL.indexOf(minSeverity);
		return Severity.ALL.indexOf(severity) >= index;
	}

	@SuppressWarnings("deprecation")
	public Notification createNotification(DecoratorContext context, Issue issue) {
		Notification notification = new Notification(
				NotificationFields.NOTIFICATION_TYPE);
		notification.setFieldValue(NotificationFields.PROJECT_ID,
				getRootProgect(context).getId().toString());
		notification
				.setFieldValue(NotificationFields.COMPONENT_PATH, context
						.getProject().getPath()
						+ "/"
						+ context.getResource().getPath());
		notification.setFieldValue(NotificationFields.SEVERITY,
				issue.severity());
		notification.setFieldValue(NotificationFields.MESSAGE, issue.message());
		notification.setFieldValue(NotificationFields.LINE,
				ObjectUtils.toString(issue.line()));
		notification.setFieldValue(NotificationFields.RULE_KEY, issue.ruleKey()
				.toString());

		Integer line = issue.line();
		Integer lineLastRevition = getLastRevisionLine(context);
		if (line == null) {
			line = lineLastRevition;
		}

		notification
				.setFieldValue(
						NotificationFields.SCM_AUTHOR,
						getResourceData(context,
								CoreMetrics.SCM_AUTHORS_BY_LINE, line));
		notification.setFieldValue(
				NotificationFields.SCM_DATE,
				getResourceData(context,
						CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE, line));
		notification.setFieldValue(
				NotificationFields.SCM_REVISION,
				getResourceData(context, CoreMetrics.SCM_REVISIONS_BY_LINE,
						line));
		notification.setFieldValue(
				NotificationFields.SCM_REVISION_LAST,
				getResourceData(context, CoreMetrics.SCM_REVISIONS_BY_LINE,
						lineLastRevition));

		return notification;
	}

	@SuppressWarnings("rawtypes")
	protected String getResourceData(DecoratorContext context,
			Metric<String> metric, Integer line) {
		Measure measure = context.getMeasure(metric);

		if (measure == null) {
			return null;
		}

		Map<Integer, String> map = KeyValueFormat.parseIntString(measure
				.getData());
		return map.get(line);
	}

	@SuppressWarnings({ "rawtypes", "deprecation" })
	protected Integer getLastRevisionLine(DecoratorContext context) {
		Measure measureDates = context
				.getMeasure(CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE);
		if (measureDates == null) {
			return null;
		}

		Map<Integer, Date> map = KeyValueFormat.parse(measureDates.getData(),
				KeyValueFormat.newIntegerConverter(),
				KeyValueFormat.newDateConverter(DATE_FORMAT));
		Ordering<Entry<Integer, Date>> ordering = new Ordering<Map.Entry<Integer, Date>>() {
			public int compare(Entry<Integer, Date> left,
					Entry<Integer, Date> right) {
				return left.getValue().compareTo(right.getValue());
			}
		};
		return ordering.max(map.entrySet()).getKey();
	}

	protected Project getRootProgect(DecoratorContext context) {
		Project project = context.getProject();
		while (project.isModule()) {
			project = project.getRoot();
		}
		return project;
	}
}
