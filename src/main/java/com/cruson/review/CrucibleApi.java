package com.cruson.review;

public interface CrucibleApi {
	public boolean isUserExist(String user) throws Exception;

	public void addReviewer(String reviewId, String user) throws Exception;

	public String createReview(String project, String message,
			String description, String author) throws Exception;

	public void startReview(String reviewId) throws Exception;

	public String addReviewItem(String repository, String reviewId,
			String path, String revision) throws Exception;

	public void addReviewComment(String reviewId, String itemId,
			String message, String line) throws Exception;

}