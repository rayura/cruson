package com.cruson.review;

import org.apache.commons.lang.StringUtils;

public class NotificationFields {
	public static final String NOTIFICATION_TYPE = "new-issue-review";
	public static final String PROJECT_KEY = "projectKey";
	public static final String COMPONENT_KEY = "componentKey";
	public static final String MESSAGE = "message";
	public static final String SEVERITY = "severity";
	public static final String LINE = "line";
	public static final String SCM_REVISION = "scmRevisions";
	public static final String SCM_DATE = "scmDate";
	public static final String SCM_AUTHOR = "scmAuthor";
	public static final String RULE_KEY = "ruleKey";

	public static String getAuthorName(String scmAuthor) {
		return StringUtils.substringBefore(scmAuthor, "@");
	}

	public static String getFile(String componentKey, String projectKey) {
		return StringUtils.substringAfter(componentKey, projectKey + ":");
	}
}
