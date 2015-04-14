package com.cruson.review;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class NotificationFields {
    public static final String NOTIFICATION_TYPE = "new-issue-review";
    public static final String PROJECT_KEY = "projectKey";
    public static final String COMPONENT_PATH = "componentPath";
    public static final String MESSAGE = "message";
    public static final String SEVERITY = "severity";
    public static final String LINE = "line";
    public static final String SCM_REVISION = "scmRevision";
    public static final String SCM_REVISION_LAST = "scmRevisionLast";
    public static final String SCM_DATE = "scmDate";
    public static final String SCM_AUTHOR = "scmAuthor";
    public static final String RULE_KEY = "ruleKey";
    private static final Pattern EMAIL_PREFIX = Pattern
            .compile("([_\\w-\\+\\.]*)@");

    private NotificationFields() {
    }

    public static String getAuthorName(String scmAuthor) {
        Matcher matcher = EMAIL_PREFIX.matcher(scmAuthor);
        return matcher.find() ? matcher.group(1) : null;
    }
}
