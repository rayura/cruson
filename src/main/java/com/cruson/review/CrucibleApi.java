package com.cruson.review;

public interface CrucibleApi {
	public abstract boolean isUserExist(String user) throws Exception;

	public abstract void addReviewer(String reviewId, String user)
			throws Exception;

	public abstract String createReview(String project, String message,
			String description, String author) throws Exception;

	public abstract void startReview(String reviewId) throws Exception;

	public abstract String addReviewItem(String repository, String reviewId,
			String path, String revision) throws Exception;

	public abstract void addReviewComment(String reviewId, String itemId,
			String message, String line) throws Exception;

}