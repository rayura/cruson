package com.cruson.review;


public interface HttpDownload {
	String doGet(String url, String user, String password) throws Exception;

	String doPost(String url, String user, String password, String content)
			throws Exception;
}
