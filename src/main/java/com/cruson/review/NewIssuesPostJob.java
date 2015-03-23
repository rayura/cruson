package com.cruson.review;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.issue.IssueCache;
import org.sonar.core.DryRunIncompatible;

import com.google.common.collect.Ordering;

@DryRunIncompatible
public class NewIssuesPostJob implements PostJob {
	private final static String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

	private final Settings settings;
	private final IssueCache issueCache;
	private final NotificationManager notificationsManager;

	public NewIssuesPostJob(Settings settings, IssueCache issueCache,
			NotificationManager notificationsManager) {
		this.settings = settings;
		this.issueCache = issueCache;
		this.notificationsManager = notificationsManager;
	}

	@Override
	public void executeOn(Project project, SensorContext context) {
		for (Issue issue : issueCache.all()) {
			if (supports(issue)) {
				Notification notification = createNotification(project,
						context, issue);
				notificationsManager.scheduleForSending(notification);
			}
		}
	}

	protected boolean supports(Issue issue) {
		return supportsSeverity(issue.severity()) && issue.isNew()
				&& issue.resolution() == null;
	}

	protected boolean supportsSeverity(String severity) {
		String minSeverity = settings.getString(CruSonPlugin.CRUSON_SEVERITY)
				.toUpperCase();
		int index = Severity.ALL.indexOf(minSeverity);
		return Severity.ALL.indexOf(severity) >= index;
	}

	public Notification createNotification(Project project,
			SensorContext context, Issue issue) {

		Notification notification = new Notification(
				NotificationFields.NOTIFICATION_TYPE);
		notification.setFieldValue(NotificationFields.PROJECT_KEY,
				project.key());
		notification.setFieldValue(NotificationFields.COMPONENT_KEY,
				issue.componentKey());
		notification.setFieldValue(NotificationFields.SEVERITY,
				issue.severity());
		notification.setFieldValue(NotificationFields.MESSAGE, issue.message());
		notification.setFieldValue(NotificationFields.LINE,
				ObjectUtils.toString(issue.line()));
		notification.setFieldValue(NotificationFields.RULE_KEY, issue.ruleKey()
				.toString());

		String path = StringUtils.substringAfter(issue.componentKey(),
				issue.projectKey() + ":");
		Resource resource = context.getResource(File.create(path));

		Integer line = issue.line();
		if (line == null) {
			line = getLastRevisionLine(context, resource);
		}

		notification.setFieldValue(
				NotificationFields.SCM_AUTHOR,
				getResourceData(context, CoreMetrics.SCM_AUTHORS_BY_LINE,
						resource, line));

		notification.setFieldValue(
				NotificationFields.SCM_DATE,
				getResourceData(context,
						CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE,
						resource, line));
		notification.setFieldValue(
				NotificationFields.SCM_REVISION,
				getResourceData(context, CoreMetrics.SCM_REVISIONS_BY_LINE,
						resource, line));

		return notification;
	}

	protected String getResourceData(SensorContext context,
			Metric<String> metric, Resource resource, Integer line) {
		Measure<String> measure = (Measure<String>) context.getMeasure(
				resource, metric);

		if (measure == null) {
			return null;
		}

		Map<Integer, String> map = KeyValueFormat.parseIntString(measure
				.getData());
		return map.get(line);
	}

	protected Integer getLastRevisionLine(SensorContext context,
			Resource resource) {
		Measure<String> measureDates = (Measure<String>) context.getMeasure(
				resource, CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE);
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

}
