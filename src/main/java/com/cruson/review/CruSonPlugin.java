package com.cruson.review;

import java.util.List;

import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import com.google.common.collect.ImmutableList;

public final class CruSonPlugin extends SonarPlugin {

	public static final String CRUSON_HOST_USER = "cruson.crucible.host.user";
	public static final String CRUSON_HOST_PASSWORD = "cruson.crucible.host.password";
	public static final String CRUSON_HOST_URL = "cruson.crucible.host.url";
	public static final String CRUSON_PROJECT = "cruson.crucible.project";
	public static final String CRUSON_REPOSITORY = "cruson.crucible.repository";
	public static final String CRUSON_SEVERITY = "cruson.severity";

	@Override
	public List getExtensions() {
		return ImmutableList.of(
			PropertyDefinition.builder(CRUSON_HOST_URL)
		        .name("Crucible host url")
		        .description("Crucible host url like http://crucible:8080")
		        .onQualifiers(Qualifiers.PROJECT)
		        .category(CoreProperties.CATEGORY_GENERAL)
		        .type(PropertyType.STRING)
		        .build(),
			PropertyDefinition.builder(CRUSON_HOST_USER)
		        .name("Crucible user name")
		        .onQualifiers(Qualifiers.PROJECT)
		        .category(CoreProperties.CATEGORY_GENERAL)
		        .type(PropertyType.STRING)
		        .build(),
			PropertyDefinition.builder(CRUSON_HOST_PASSWORD)
		        .name("Crucible user password")
		        .onQualifiers(Qualifiers.PROJECT)
		        .category(CoreProperties.CATEGORY_GENERAL)
		        .type(PropertyType.STRING)
		        .build(),
			PropertyDefinition.builder(CRUSON_PROJECT)
		        .name("Crucible project for review pushing")
		        .onQualifiers(Qualifiers.PROJECT)
		        .category(CoreProperties.CATEGORY_GENERAL)
		        .type(PropertyType.STRING)
		        .build(),
			PropertyDefinition.builder(CRUSON_REPOSITORY)
		        .name("Crucible repository name")
		        .onQualifiers(Qualifiers.PROJECT)
		        .category(CoreProperties.CATEGORY_GENERAL)
		        .type(PropertyType.STRING)
		        .build(),
			PropertyDefinition.builder(CRUSON_SEVERITY)
		        .name("Crucible minimum severity")
		        .description("Crucible minimum severity. May be one of INFO, MINOR, MAJOR, CRITICAL, BLOCKER")
		        .onQualifiers(Qualifiers.PROJECT)
		        .category(CoreProperties.CATEGORY_GENERAL)
		        .type(PropertyType.STRING)
		        .build(),
			NewIssuesPostJob.class,
			NewIssuesNotificationDispatcher.class,
			CrucibleNotificationChannel.class);
	}
}
