package com.cruson.review;

import java.io.IOException;

public interface HttpDownload {
	String doGet(String url, String user, String password) throws IOException;

	String doPost(String url, String user, String password, String content)
			throws IOException;
}
