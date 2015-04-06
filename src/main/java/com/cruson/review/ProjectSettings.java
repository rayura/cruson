package com.cruson.review;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Settings;

public class ProjectSettings extends Settings {
	private final Settings settings;
	private final Map<String, String> properties;

	public ProjectSettings(Settings settings, Map<String, String> properties) {
		this.settings = settings;
		this.properties = properties;
	}

	@Override
	public String getString(String key) {
		String value = get(key);
		if (value == null) {
			return settings.getString(key);
		}

		return value;
	}

	@Override
	public boolean getBoolean(String key) {
		String value = get(key);
		if (value == null) {
			return settings.getBoolean(key);
		}

		return StringUtils.isNotEmpty(value) && Boolean.parseBoolean(value);
	}

	@Override
	public int getInt(String key) {
		String value = get(key);
		if (value == null) {
			return settings.getInt(key);
		} else if (StringUtils.isNotEmpty(value)) {
			return Integer.parseInt(value);
		} else {
			return 0;
		}
	}

	private String get(String key) {
		return properties.get(key);
	}
}
