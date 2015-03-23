package com.cruson.review;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerExtension;
import org.sonar.api.config.Settings;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CrucibleApiImpl implements CrucibleApi, ServerExtension {

	protected static final String LINK_USER_INFO = "/rest-service/users-v1/%s";
	protected static final String LINK_REVIEW_DATA = "/rest-service/reviews-v1";
	protected static final String LINK_REVIEW_REVIEWER = "/rest-service/reviews-v1/%s/reviewers";
	protected static final String LINK_REVIEW_ITEM = "/rest-service/reviews-v1/%s/reviewitems";
	protected static final String LINK_REVIEW_COMMENT = "/rest-service/reviews-v1/%s/reviewitems/%s/comments";
	protected static final String LINK_REVIEW_STATE = "/rest-service/reviews-v1/%s/transition?action=";
	protected static final String LINK_REVIEW_START = LINK_REVIEW_STATE
			+ "action:approveReview";

	protected static final String USER_INFO_DATA = "userData";
	protected static final String ERROR_MESSAGE = "stacktrace";

	private HttpDownload httpDownload;
	private Settings settings;

	public CrucibleApiImpl(Settings settings, HttpDownload httpDownload) {
		this.settings = settings;
		this.httpDownload = httpDownload;
	}

	public boolean isUserExist(String user) throws Exception {
		try {
			String content = httpDownload.doGet(
					getUrl() + String.format(LINK_USER_INFO, user), getUser(),
					getPassword());
			return convertResponse(content).get(USER_INFO_DATA) != null;
		} catch (IOException e) {
			return false;
		}
	}

	public void addReviewer(String reviewId, String user) throws Exception {
		httpDownload.doPost(
				getUrl() + String.format(LINK_REVIEW_REVIEWER, reviewId),
				getUser(), getPassword(), user);
		// convertResponse(content);
	}

	public String createReview(String project, String message,
			String description, String author) throws Exception {
		JsonObject data = new JsonObject();
		JsonObject reviewData = new JsonObject();
		data.add("reviewData", reviewData);
		reviewData.addProperty("projectKey", project);
		reviewData.addProperty("name", message);
		reviewData.addProperty("description", description);
		reviewData.addProperty("allowReviewersToJoin", "true");

		JsonObject authorObj = new JsonObject();
		reviewData.add("author", authorObj);
		authorObj.addProperty("userName", author);

		String content = httpDownload.doPost(getUrl() + LINK_REVIEW_DATA,
				getUser(), getPassword(), data.toString());

		data = convertResponse(content);
		return data.getAsJsonObject("permaId").get("id").getAsString();
	}

	public void startReview(String reviewId) throws Exception {
		String content = httpDownload.doPost(
				getUrl() + String.format(LINK_REVIEW_START, reviewId),
				getUser(), getPassword(), "");
		convertResponse(content);
	}

	public String addReviewItem(String reviewId, String path, String revision)
			throws Exception {
		JsonObject data = new JsonObject();
		data.addProperty("repositoryName",
				settings.getString(CruSonPlugin.CRUSON_REPOSITORY));

		data.addProperty("fromPath", path);
		data.addProperty("fromRevision", revision);
		data.addProperty("toPath", path);
		data.addProperty("toRevision", revision);

		String content = httpDownload.doPost(
				getUrl() + String.format(LINK_REVIEW_ITEM, reviewId),
				getUser(), getPassword(), data.toString());
		data = convertResponse(content);
		return data.getAsJsonObject("permId").get("id").getAsString();
	}

	public void addReviewComment(String reviewId, String itemId,
			String message, String line) throws Exception {
		JsonObject data = new JsonObject();
		data.addProperty("message", message);
		if (StringUtils.isNotBlank(line)) {
			data.addProperty("toLineRange", line);
		}
		String content = httpDownload
				.doPost(getUrl()
						+ String.format(LINK_REVIEW_COMMENT, reviewId, itemId),
						getUser(), getPassword(), data.toString());
		convertResponse(content);
	}

	protected JsonObject convertResponse(String content) throws Exception {
		JsonObject jsonObject = new JsonParser().parse(content)
				.getAsJsonObject();
		checkError(jsonObject);
		return jsonObject;
	}

	protected void checkError(JsonObject jsonObject) throws Exception {
		if (jsonObject.get(ERROR_MESSAGE) != null) {
			throw new IOException(jsonObject.get(ERROR_MESSAGE).getAsString());
		}
	}

	protected String getUser() {
		return settings.getString(CruSonPlugin.CRUSON_HOST_USER);
	}

	protected String getPassword() {
		return settings.getString(CruSonPlugin.CRUSON_HOST_PASSWORD);
	}

	protected String getUrl() {
		return settings.getString(CruSonPlugin.CRUSON_HOST_URL);
	}

	public HttpDownload getHttpDownload() {
		return httpDownload;
	}

	public void setHttpDownload(HttpDownload httpDownload) {
		this.httpDownload = httpDownload;
	}

	public Settings getSettings() {
		return settings;
	}

	public void setSettings(Settings settings) {
		this.settings = settings;
	}
}
