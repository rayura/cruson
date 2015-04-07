package com.cruson.review;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerExtension;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CrucibleApiImpl implements CrucibleApi, ServerExtension {
    private static final Logger LOG = LoggerFactory
            .getLogger(CrucibleApiImpl.class);

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
    private String url;
    private String login;
    private String password;

    public CrucibleApiImpl(HttpDownload httpDownload) {
        this.httpDownload = httpDownload;
    }

    @Override
    public boolean isUserExist(String user) throws IOException {
        try {
            String content = httpDownload.doGet(
                    url + String.format(LINK_USER_INFO, user), login, password);
            return convertResponse(content).get(USER_INFO_DATA) != null;
        } catch (IOException e) {
            LOG.warn(e.getMessage());
            return false;
        }
    }

    @Override
    public void addReviewer(String reviewId, String reviewer)
            throws IOException {
        httpDownload.doPost(
                url + String.format(LINK_REVIEW_REVIEWER, reviewId), login,
                password, reviewer);
    }

    @Override
    public String createReview(String project, String message,
            String description, String author) throws IOException {
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

        String content = httpDownload.doPost(url + LINK_REVIEW_DATA, login,
                password, data.toString());

        data = convertResponse(content);
        return data.getAsJsonObject("permaId").get("id").getAsString();
    }

    @Override
    public void startReview(String reviewId) throws IOException {
        String content = httpDownload.doPost(
                url + String.format(LINK_REVIEW_START, reviewId), login,
                password, "");
        convertResponse(content);
    }

    @Override
    public String addReviewItem(String repository, String reviewId,
            String path, String revision) throws IOException {
        JsonObject data = new JsonObject();

        data.addProperty("repositoryName", repository);
        data.addProperty("fromPath", path);
        data.addProperty("fromRevision", revision);
        data.addProperty("toPath", path);
        data.addProperty("toRevision", revision);

        String content = httpDownload.doPost(
                url + String.format(LINK_REVIEW_ITEM, reviewId), login,
                password, data.toString());
        data = convertResponse(content);
        return data.getAsJsonObject("permId").get("id").getAsString();
    }

    @Override
    public void addReviewComment(String reviewId, String itemId,
            String message, String line) throws IOException {
        JsonObject data = new JsonObject();
        data.addProperty("message", message);
        if (StringUtils.isNotBlank(line)) {
            data.addProperty("toLineRange", line);
        }
        String content = httpDownload.doPost(
                url + String.format(LINK_REVIEW_COMMENT, reviewId, itemId),
                login, password, data.toString());
        convertResponse(content);
    }

    protected JsonObject convertResponse(String content) throws IOException {
        JsonObject jsonObject = new JsonParser().parse(content)
                .getAsJsonObject();
        checkError(jsonObject);
        return jsonObject;
    }

    protected void checkError(JsonObject jsonObject) throws IOException {
        if (jsonObject.get(ERROR_MESSAGE) != null) {
            throw new IOException(jsonObject.get(ERROR_MESSAGE).getAsString());
        }
    }

    public HttpDownload getHttpDownload() {
        return httpDownload;
    }

    public void setHttpDownload(HttpDownload httpDownload) {
        this.httpDownload = httpDownload;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
