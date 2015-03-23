package com.cruson.review;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.config.Settings;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@RunWith(MockitoJUnitRunner.class)
public class CrucibleApiImplTest {
	@Mock
	private HttpDownload httpDownload;

	private CrucibleApiImpl api;

	private String url = "CRUSON_HOST_URL";
	private String repository = "CRUSON_REPOSITORY";
	private String response = "response";
	private String user = "CRUSON_HOST_USER";
	private String password = "CRUSON_HOST_PASSWORD";

	@Before
	public void setUp() {
		Map<String, String> properties = new HashMap<>();
		properties.put(CruSonPlugin.CRUSON_HOST_URL, url);
		properties.put(CruSonPlugin.CRUSON_HOST_USER, user);
		properties.put(CruSonPlugin.CRUSON_HOST_PASSWORD, password);
		properties.put(CruSonPlugin.CRUSON_REPOSITORY, repository);

		Settings settings = new Settings();
		settings.setProperties(properties);

		api = new CrucibleApiImpl(settings, httpDownload);
		api = Mockito.spy(api);
	}

	@Test
	public void testIsUserExist() throws Exception {
		String userName = "userName";
		url = url + String.format(api.LINK_USER_INFO, userName);

		when(httpDownload.doGet(eq(url), eq(user), eq(password))).thenReturn(
				response);
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(CrucibleApiImpl.USER_INFO_DATA, userName);
		doReturn(jsonObject).when(api).convertResponse(response);

		assertTrue(api.isUserExist(userName));

		verify(api).convertResponse(response);
	}

	@Test
	public void testIsUserExistNotFound() throws Exception {
		String userName = "userName";
		url = url + String.format(api.LINK_USER_INFO, userName);

		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(CrucibleApiImpl.ERROR_MESSAGE, "not found");

		when(httpDownload.doGet(eq(url), eq(user), eq(password))).thenReturn(
				jsonObject.toString());

		assertFalse(api.isUserExist(userName));
	}

	@Test
	public void testAddReviewer() throws Exception {
		String reviewId = "reviewId";
		String reviewer = "reviewer";
		url = url + String.format(api.LINK_REVIEW_REVIEWER, reviewId);

		when(httpDownload.doPost(eq(url), eq(user), eq(password), eq(reviewer)))
				.thenReturn(response);
//		doReturn(null).when(api).convertResponse(response);

		api.addReviewer(reviewId, reviewer);

		verify(httpDownload).doPost(eq(url), eq(user), eq(password),
				eq(reviewer));
//		verify(api).convertResponse(response);
	}

	@Test
	public void testCreateReview() throws Exception {
		String project = "project";
		String message = "message";
		String description = "description";
		String author = "author";
		String reviewId = "reviewId";
		url = url + api.LINK_REVIEW_DATA;

		when(httpDownload.doPost(eq(url), eq(user), eq(password), anyString()))
				.thenReturn(response);

		JsonObject data = new JsonObject();
		JsonObject permaId = new JsonObject();
		data.add("permaId", permaId);
		permaId.addProperty("id", reviewId);
		doReturn(data).when(api).convertResponse(response);

		assertEquals(reviewId,
				api.createReview(project, message, description, author));

		ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
		verify(httpDownload).doPost(eq(url), eq(user), eq(password),
				argument.capture());
		JsonObject jsonObject = new JsonParser().parse(argument.getValue())
				.getAsJsonObject().getAsJsonObject("reviewData");
		assertEquals(project, jsonObject.get("projectKey").getAsString());
		assertEquals(message, jsonObject.get("name").getAsString());
		assertEquals(description, jsonObject.get("description").getAsString());
		assertEquals(author,
				jsonObject.getAsJsonObject("author").get("userName")
						.getAsString());
		verify(api).convertResponse(response);
	}

	@Test
	public void testAddReviewItem() throws Exception {
		String path = "path";
		String revision = "revision";
		String reviewId = "reviewId";
		String itemId = "itemId";
		url = url + String.format(api.LINK_REVIEW_ITEM, reviewId);

		when(httpDownload.doPost(eq(url), eq(user), eq(password), anyString()))
				.thenReturn(response);

		JsonObject data = new JsonObject();
		JsonObject permaId = new JsonObject();
		data.add("permId", permaId);
		permaId.addProperty("id", reviewId);
		doReturn(data).when(api).convertResponse(response);

		assertEquals(reviewId, api.addReviewItem(reviewId, path, revision));

		ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
		verify(httpDownload).doPost(eq(url), eq(user), eq(password),
				argument.capture());
		JsonObject jsonObject = new JsonParser().parse(argument.getValue())
				.getAsJsonObject();
		assertEquals(path, jsonObject.get("fromPath").getAsString());
		assertEquals(path, jsonObject.get("toPath").getAsString());
		assertEquals(revision, jsonObject.get("fromRevision").getAsString());
		assertEquals(revision, jsonObject.get("toRevision").getAsString());
		assertEquals(repository, jsonObject.get("repositoryName").getAsString());

		verify(api).convertResponse(response);
	}

	@Test
	public void testStartReview() throws Exception {
		String reviewId = "reviewId";
		url = url + String.format(api.LINK_REVIEW_START, reviewId);

		when(httpDownload.doPost(url, user, password, "")).thenReturn(response);

		doReturn(null).when(api).convertResponse(response);

		api.startReview(reviewId);

		verify(httpDownload).doPost(eq(url), eq(user), eq(password), eq(""));
		verify(api).convertResponse(response);
	}

	@Test
	public void testAddReviewComment() throws Exception {
		String reviewId = "reviewId";
		String itemId = "itemId";
		String message = "message";
		String line = "line";
		url = url + String.format(api.LINK_REVIEW_COMMENT, reviewId, itemId);

		when(httpDownload.doPost(eq(url), eq(user), eq(password), anyString()))
				.thenReturn(response);
		doReturn(null).when(api).convertResponse(response);

		api.addReviewComment(reviewId, itemId, message, line);

		ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
		verify(httpDownload).doPost(eq(url), eq(user), eq(password),
				argument.capture());
		JsonObject jsonObject = new JsonParser().parse(argument.getValue())
				.getAsJsonObject();
		assertEquals(message, jsonObject.get("message").getAsString());
		assertEquals(line, jsonObject.get("toLineRange").getAsString());
		verify(api).convertResponse(response);
	}

	@Test
	public void testAddReviewCommentToFile() throws Exception {
		String reviewId = "reviewId";
		String itemId = "itemId";
		String message = "message";
		String line = null;
		url = url + String.format(api.LINK_REVIEW_COMMENT, reviewId, itemId);

		when(httpDownload.doPost(eq(url), eq(user), eq(password), anyString()))
				.thenReturn(response);
		doReturn(null).when(api).convertResponse(response);

		api.addReviewComment(reviewId, itemId, message, line);

		ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
		verify(httpDownload).doPost(eq(url), eq(user), eq(password),
				argument.capture());
		JsonObject jsonObject = new JsonParser().parse(argument.getValue())
				.getAsJsonObject();
		assertEquals(message, jsonObject.get("message").getAsString());
		assertNull(jsonObject.get("toLineRange"));
		verify(api).convertResponse(response);
	}

	@Test(expected = IOException.class)
	public void testCheckError() throws Exception {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(CrucibleApiImpl.ERROR_MESSAGE, "ERROR_MESSAGE");

		api.checkError(jsonObject);
	}

	@Test
	public void testCheckNoError() throws Exception {
		JsonObject jsonObject = new JsonObject();

		api.checkError(jsonObject);
	}
}
