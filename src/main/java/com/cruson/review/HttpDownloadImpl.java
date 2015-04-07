package com.cruson.review;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.sonar.api.ServerExtension;

public class HttpDownloadImpl implements HttpDownload, ServerExtension {
	private static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";

	private HttpClient httpClient;

	public HttpDownloadImpl() {
		httpClient = new DefaultHttpClient();
	}

	@Override
	public String doGet(String url, String user, String password)
			throws Exception {
		return doRequest(url, user, password, null);
	}

	@Override
	public String doPost(String url, String user, String password,
			String content) throws Exception {
		return doRequest(url, user, password, content);
	}

	protected String doRequest(String url, String user, String password,
			String requestContent) throws Exception {
		String responseContent = "";
		HttpEntity entity = null;
		try {
			HttpRequestBase httpRequest;
			if (requestContent == null) {
				httpRequest = new HttpGet(url);
			} else {
				HttpPost httpPost = new HttpPost(url);
				httpPost.setEntity(new StringEntity(requestContent));
				httpRequest = httpPost;
			}

			httpRequest.addHeader(new BasicScheme().authenticate(
					new UsernamePasswordCredentials(user, password),
					httpRequest));
			httpRequest.setHeader("Accept", CONTENT_TYPE_APPLICATION_JSON);
			httpRequest
					.setHeader("Content-Type", CONTENT_TYPE_APPLICATION_JSON);

			HttpResponse response = httpClient.execute(httpRequest);
			entity = response.getEntity();

			if (entity != null) {
				responseContent = IOUtils.toString(entity.getContent());
			}
		} catch (Exception e) {
			throw e;
		} finally {
			EntityUtils.consume(entity);
		}
		return responseContent;
	}

	public HttpClient getHttpClient() {
		return httpClient;
	}

	public void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}
}
